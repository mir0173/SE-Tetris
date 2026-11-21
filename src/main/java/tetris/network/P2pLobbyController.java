package tetris.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import tetris.battle.BattleSettings;
import tetris.network.LobbyState.Phase;

// AI-assisted code start
/** 연결, 설정, 준비 관리 */
public final class P2pLobbyController implements AutoCloseable {
    public static final int DEFAULT_PORT = 50_000;
    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final int HANDSHAKE_TIMEOUT_MILLIS = 5_000;

    private final Executor uiExecutor;
    private final Consumer<LobbyState> onState;
    private final Consumer<MatchStart> onMatchStart;
    private final Consumer<String> onConnectedAddress;
    private final ExecutorService workers = Executors.newCachedThreadPool(
            Thread.ofPlatform().daemon(true).name("p2p-connect-", 0).factory());

    private Phase phase = Phase.IDLE;
    private PeerRole role;
    private BattleSettings settings;
    private String message = "서버를 열거나 상대 서버에 접속하세요.";
    private int port;
    private boolean localReady;
    private boolean remoteReady;
    private boolean helloReceived;
    private long generation;
    private long revision;
    private long matchId;
    private ServerSocket serverSocket;
    private Socket pendingSocket;
    private PeerConnection connection;
    private BattleLink battleLink;
    private PeerHealth health;

    public P2pLobbyController(Executor uiExecutor, Consumer<LobbyState> onState,
            Consumer<MatchStart> onMatchStart, BattleSettings initialSettings) {
        this(uiExecutor, onState, onMatchStart, initialSettings, address -> { });
    }

    /** 최초 설정 수신이 끝나면 실제로 연결된 IP를 전달한다. */
    public P2pLobbyController(Executor uiExecutor, Consumer<LobbyState> onState,
            Consumer<MatchStart> onMatchStart, BattleSettings initialSettings, Consumer<String> onConnectedAddress) {
        this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor");
        this.onState = Objects.requireNonNull(onState, "onState");
        this.onMatchStart = Objects.requireNonNull(onMatchStart, "onMatchStart");
        this.onConnectedAddress = Objects.requireNonNull(onConnectedAddress, "onConnectedAddress");
        settings = Objects.requireNonNull(initialSettings, "initialSettings");
    }

    public synchronized void host(int requestedPort) {
        requireIdle();
        validatePort(requestedPort, true);
        long attempt = begin(PeerRole.HOST, Phase.HOSTING);
        revision = 1;
        message = "서버를 여는 중입니다.";
        publish();
        workers.execute(() -> acceptPeer(attempt, requestedPort));
    }

    /** 지정한 IPv4 주소에 비동기로 접속 */
    public synchronized void connect(String address, int requestedPort) {
        requireIdle();
        validatePort(requestedPort, false);
        byte[] ipv4 = parseIpv4(address);
        long attempt = begin(PeerRole.CLIENT, Phase.CONNECTING);
        port = requestedPort;
        message = address.strip() + " 서버에 접속 중입니다.";
        publish();
        workers.execute(() -> connectPeer(attempt, ipv4, requestedPort));
    }

    /** 서버가 설정을 바꾸면 준비를 해제함 */
    public synchronized void updateSettings(BattleSettings newSettings) {
        Objects.requireNonNull(newSettings, "newSettings");
        if (role != PeerRole.HOST || (phase != Phase.HOSTING && phase != Phase.CONNECTED)) {
            throw new IllegalStateException("호스트만 설정을 변경할 수 있습니다.");
        }
        if (settings.equals(newSettings)) {
            return;
        }
        settings = newSettings;
        revision++;
        localReady = false;
        remoteReady = false;
        message = "설정이 변경되었습니다. 다시 게임 시작을 눌러주세요.";
        if (phase == Phase.CONNECTED) {
            send(LobbyMessage.config(revision, settings));
        }
        publish();
    }

    public synchronized void toggleReady() {
        if (phase != Phase.CONNECTED) {
            return;
        }
        localReady = !localReady;
        if (!send(LobbyMessage.ready(revision, localReady))) {
            return;
        }
        message = localReady ? "상대가 준비되길 기다립니다." : "준비를 취소했습니다.";
        publish();
        tryStart();
    }

    /** 연결을 유지한 채 대기실을 초기화 */
    public synchronized void returnToLobby() {
        if (role != PeerRole.HOST || phase != Phase.STARTED) {
            return;
        }
        revision++;
        closeBattleLink();
        localReady = false;
        remoteReady = false;
        phase = Phase.CONNECTED;
        message = "대기실로 돌아왔습니다. 다시 게임 시작을 누를 수 있습니다.";
        send(LobbyMessage.config(revision, settings));
        publish();
    }

    /** 역할 선택 상태로 돌아간다 */
    public synchronized void cancel() {
        if (phase == Phase.CLOSED) {
            return;
        }
        generation++;
        closeResources();
        role = null;
        localReady = false;
        remoteReady = false;
        phase = Phase.IDLE;
        message = "연결을 종료했습니다.";
        publish();
    }

    public synchronized LobbyState getState() {
        return new LobbyState(phase, role, port, settings, localReady, remoteReady, message, health);
    }

    /** 준비가 완료된 현재 경기에서만 송수신 통로를 제공한다. 소켓의 소유권은 대기실에 유지한다. */
    public synchronized BattleLink getBattleLink(MatchStart start) {
        if (phase != Phase.STARTED || battleLink == null || start.matchId() != matchId || start.role() != role) {
            throw new IllegalStateException("현재 경기의 연결을 사용할 수 없습니다.");
        }
        return battleLink;
    }

    /** 어느 쪽에서 요청해도 서버가 연결을 유지한 대기실 복귀를 확정한다. 승패 판정은 하지 않는다. */
    public synchronized void requestReturnToLobby() {
        if (phase != Phase.STARTED) {
            return;
        }
        if (role == PeerRole.HOST) {
            returnToLobby();
        } else {
            send(LobbyMessage.start(LobbyMessage.Type.RETURN_LOBBY, revision, matchId));
        }
    }

    public synchronized void refresh() {
        publish();
    }

    @Override
    public synchronized void close() {
        if (phase == Phase.CLOSED) {
            return;
        }
        generation++;
        closeResources();
        phase = Phase.CLOSED;
        workers.shutdownNow();
    }

    private long begin(PeerRole newRole, Phase newPhase) {
        generation++;
        closeResources();
        role = newRole;
        phase = newPhase;
        port = 0;
        localReady = false;
        remoteReady = false;
        helloReceived = false;
        revision = 0;
        matchId = 0;
        return generation;
    }

    private void acceptPeer(long attempt, int requestedPort) {
        try (ServerSocket listening = new ServerSocket()) {
            synchronized (this) {
                if (attempt != generation) {
                    return;
                }
                serverSocket = listening;
            }
            listening.bind(new InetSocketAddress(requestedPort));
            synchronized (this) {
                if (attempt != generation) {
                    return;
                }
                port = listening.getLocalPort();
                message = "서버 대기 중입니다 ";
                publish();
            }
            attach(attempt, listening.accept());
        } catch (IOException exception) {
            fail(attempt, "서버 연결을 진행할 수 없습니다: " + exception.getMessage());
        }
    }

    private void connectPeer(long attempt, byte[] ipv4, int requestedPort) {
        try {
            Socket socket = new Socket();
            synchronized (this) {
                if (attempt != generation) {
                    socket.close();
                    return;
                }
                pendingSocket = socket;
            }
            socket.connect(new InetSocketAddress(InetAddress.getByAddress(ipv4), requestedPort),
                    CONNECT_TIMEOUT_MILLIS);
            attach(attempt, socket);
        } catch (IOException exception) {
            fail(attempt, "서버에 접속하지 못했습니다: " + exception.getMessage());
        }
    }

    private synchronized void attach(long attempt, Socket socket) throws IOException {
        if (attempt != generation) {
            socket.close();
            return;
        }
        pendingSocket = socket;
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(HANDSHAKE_TIMEOUT_MILLIS);
        phase = Phase.HANDSHAKING;
        message = "연결 정보를 확인 중입니다.";
        connection = new PeerConnection(socket, received -> receive(attempt, received),
                error -> {
                    String detail = error.getMessage();
                    fail(attempt, detail == null ? "상대와의 연결이 끊겼습니다."
                            : "상대와의 연결이 종료되었습니다: " + detail);
                },
                result -> updateHealth(attempt, result));
        connection.start();
        send(LobbyMessage.hello(role));
        publish();
    }

    private synchronized void receive(long attempt, LobbyMessage received) {
        if (attempt != generation) {
            return;
        }
        try {
            switch (received.type()) {
                case HELLO -> receiveHello(received);
                case CONFIG -> receiveConfig(received);
                case READY -> receiveReady(received);
                case START -> receiveStart(received);
                case START_ACK -> receiveStartAcknowledgement(received);
                case START_COMMIT -> receiveStartCommit(received);
                case START_REJECT -> receiveStartRejection(received);
                case BATTLE -> receiveBattle(received);
                case RETURN_LOBBY -> {
                    requireProtocol(role == PeerRole.HOST);
                    if (phase == Phase.STARTED && received.matchId() == matchId && received.revision() == revision) {
                        returnToLobby();
                    }
                }
                case PING, PONG -> throw new IOException("연결 확인 메시지를 처리하지 못했습니다.");
            }
        } catch (IOException exception) {
            fail(attempt, "연결 정보를 처리할 수 없습니다: " + exception.getMessage());
        }
    }

    private void receiveHello(LobbyMessage received) throws IOException {
        requireProtocol(phase == Phase.HANDSHAKING && !helloReceived && received.role() != role);
        helloReceived = true;
        if (role == PeerRole.HOST) {
            finishHandshake();
            phase = Phase.CONNECTED;
            message = "모드를 선택하고 게임 시작을 눌러주세요.";
            send(LobbyMessage.config(revision, settings));
            publish();
        }
    }

    private void receiveConfig(LobbyMessage received) throws IOException {
        requireProtocol(role == PeerRole.CLIENT && helloReceived
                && (phase == Phase.HANDSHAKING || phase == Phase.CONNECTED || phase == Phase.STARTED));
        if (received.revision() <= revision) {
            return;
        }
        if (phase == Phase.HANDSHAKING) {
            finishHandshake();
            String address = pendingSocket.getInetAddress().getHostAddress();
            uiExecutor.execute(() -> onConnectedAddress.accept(address));
        }
        closeBattleLink();
        settings = received.settings();
        revision = received.revision();
        localReady = false;
        remoteReady = false;
        phase = Phase.CONNECTED;
        message = "서버 설정을 확인 후 게임 시작을 눌러주세요.";
        publish();
    }

    private void receiveReady(LobbyMessage received) throws IOException {
        if (received.revision() < revision) {
            return;
        }
        if (role == PeerRole.HOST && phase == Phase.STARTING && received.revision() == revision) {
            // 시작 요청 직전에 보낸 준비 취소는 이후 START_REJECT로 함께 처리한다.
            remoteReady = received.ready();
            return;
        }
        requireProtocol(phase == Phase.CONNECTED && helloReceived && received.revision() == revision);
        remoteReady = received.ready();
        message = remoteReady ? "상대가 준비를 마쳤습니다." : "상대가 준비 중입니다.";
        publish();
        tryStart();
    }

    private void tryStart() {
        if (role == PeerRole.HOST && phase == Phase.CONNECTED && localReady && remoteReady) {
            matchId++;
            phase = Phase.STARTING;
            message = "시작을 확인하고 있습니다.";
            send(LobbyMessage.start(LobbyMessage.Type.START, revision, matchId));
            publish();
        }
    }

    private void receiveStart(LobbyMessage received) throws IOException {
        requireProtocol(role == PeerRole.CLIENT && phase == Phase.CONNECTED && remoteReady
                && received.revision() == revision && received.matchId() > matchId);
        matchId = received.matchId();
        if (!localReady) {
            send(LobbyMessage.start(LobbyMessage.Type.START_REJECT, revision, matchId));
            return;
        }
        phase = Phase.STARTING;
        message = "서버의 시작 확정을 기다립니다.";
        send(LobbyMessage.start(LobbyMessage.Type.START_ACK, revision, matchId));
        publish();
    }

    private void receiveStartAcknowledgement(LobbyMessage received) throws IOException {
        requireProtocol(role == PeerRole.HOST && matchesStartingGame(received));
        if (send(LobbyMessage.start(LobbyMessage.Type.START_COMMIT, revision, matchId))) {
            finishStart();
        }
    }

    private void receiveStartRejection(LobbyMessage received) throws IOException {
        requireProtocol(role == PeerRole.HOST && matchesStartingGame(received));
        phase = Phase.STARTED;
        returnToLobby();
    }

    private void receiveStartCommit(LobbyMessage received) throws IOException {
        requireProtocol(role == PeerRole.CLIENT && matchesStartingGame(received));
        finishStart();
    }

    private boolean matchesStartingGame(LobbyMessage received) {
        return phase == Phase.STARTING && received.revision() == revision && received.matchId() == matchId;
    }

    private void finishStart() {
        phase = Phase.STARTED;
        message = "전원 준비가 완료되었습니다.";
        publish();
        long attempt = generation;
        long currentMatch = matchId;
        battleLink = new BattleLink(uiExecutor, outgoing -> sendBattle(attempt, currentMatch, outgoing),
                failure -> failBattle(attempt, currentMatch, failure));
        MatchStart start = new MatchStart(matchId, role, settings);
        uiExecutor.execute(() -> {
            synchronized (this) {
                if (attempt == generation && phase == Phase.STARTED && matchId == start.matchId()) {
                    onMatchStart.accept(start);
                }
            }
        });
    }

    private void receiveBattle(LobbyMessage received) throws IOException {
        // 전환 직전에 이미 전송된 이전 경기의 메시지는 새 대기실이나 경기에 반영하지 않는다.
        if (received.matchId() < matchId || phase != Phase.STARTED) {
            return;
        }
        requireProtocol(received.matchId() == matchId && received.revision() == revision);
        battleLink.receive(received.battle());
    }

    private synchronized void sendBattle(long attempt, long currentMatch, BattleMessage outgoing) {
        if (attempt == generation && phase == Phase.STARTED && currentMatch == matchId) {
            send(LobbyMessage.battle(revision, matchId, outgoing));
        }
    }

    private synchronized void failBattle(long attempt, long currentMatch, RuntimeException failure) {
        if (attempt == generation && phase == Phase.STARTED && currentMatch == matchId) {
            fail(attempt, "경기 정보를 처리할 수 없습니다: " + failure.getMessage());
        }
    }

    private void closeBattleLink() {
        if (battleLink != null) {
            battleLink.close();
            battleLink = null;
        }
    }

    private boolean send(LobbyMessage outgoing) {
        try {
            connection.send(outgoing);
            return true;
        } catch (IOException exception) {
            fail(generation, "상대에게 정보를 보내지 못했습니다: " + exception.getMessage());
            return false;
        }
    }

    private synchronized void fail(long attempt, String reason) {
        if (attempt != generation) {
            return;
        }
        generation++;
        closeResources();
        phase = Phase.IDLE;
        role = null;
        localReady = false;
        remoteReady = false;
        message = reason;
        publish();
    }

    private void publish() {
        LobbyState state = getState();
        long attempt = generation;
        uiExecutor.execute(() -> {
            synchronized (this) {
                if (attempt == generation && phase != Phase.CLOSED) {
                    onState.accept(state);
                }
            }
        });
    }

    private void closeResources() {
        health = null;
        closeBattleLink();
        if (connection != null) {
            connection.close();
            connection = null;
        }
        closeQuietly(pendingSocket);
        closeQuietly(serverSocket);
        pendingSocket = null;
        serverSocket = null;
    }

    private void finishHandshake() throws IOException {
        connection.finishHandshake();
        health = PeerHealth.MEASURING;
    }

    private synchronized void updateHealth(long attempt, PeerHealth result) {
        if (attempt == generation && health != null) {
            health = result;
            publish();
        }
    }

    private void requireIdle() {
        if (phase != Phase.IDLE) {
            throw new IllegalStateException("먼저 기존 연결을 종료해주세요.");
        }
    }

    private static void requireProtocol(boolean valid) throws IOException {
        if (!valid) {
            throw new IOException("대기실 메시지 순서 또는 권한이 올바르지 않습니다.");
        }
    }

    private static void validatePort(int requestedPort, boolean allowAutomatic) {
        if (requestedPort < (allowAutomatic ? 0 : 1) || requestedPort > 65_535) {
            throw new IllegalArgumentException("포트는 1~65535 사이여야 합니다.");
        }
    }

    private static byte[] parseIpv4(String address) {
        String[] parts = address == null ? new String[0] : address.strip().split("\\.", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("IPv4 주소를 입력해주세요.");
        }
        byte[] bytes = new byte[4];
        for (int index = 0; index < parts.length; index++) {
            if (!parts[index].matches("[0-9]{1,3}")) {
                throw new IllegalArgumentException("IPv4 주소 형식이 올바르지 않습니다.");
            }
            int value = Integer.parseInt(parts[index]);
            if (value > 255) {
                throw new IllegalArgumentException("IPv4 주소의 각 값은 0~255여야 합니다.");
            }
            bytes[index] = (byte) value;
        }
        return bytes;
    }

    private static void closeQuietly(AutoCloseable resource) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (Exception ignored) {
            // 정리 실패가 나머지 자원 정리를 중단하지 않게 한다.
        }
    }
}
// AI-assisted code end
