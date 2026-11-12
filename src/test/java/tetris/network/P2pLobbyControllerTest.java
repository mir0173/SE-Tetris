package tetris.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tetris.model.Difficulty;
import tetris.network.LobbyState.Phase;

// AI-assisted code start
@Timeout(15)
class P2pLobbyControllerTest {
    private static final LobbySettings NORMAL = new LobbySettings(LobbyMode.NORMAL, Difficulty.NORMAL, 0);
    private final List<P2pLobbyController> controllers = new ArrayList<>();

    @AfterEach
    void closeConnections() {
        controllers.forEach(P2pLobbyController::close);
    }

    @Test
    void connectAndSynchronizeHostSettings() throws Exception {
        Pair pair = connectPair();
        assertEquals(NORMAL, pair.client.controller.getState().settings());
        LobbySettings settings = new LobbySettings(LobbyMode.TIME_LIMIT, Difficulty.HARD, 180);
        pair.host.controller.updateSettings(settings);
        LobbyState state = pair.client.await(s -> settings.equals(s.settings()));
        assertEquals(PeerRole.CLIENT, state.role());
        assertThrows(IllegalStateException.class, () -> pair.client.controller.updateSettings(NORMAL));
        System.out.println("[P2P 접속] 실제 TCP 연결·서버 설정 공유·클라이언트 설정 변경 차단 PASS");
    }

    @Test
    void requireBothPlayersReady() throws Exception {
        Pair pair = connectPair();
        pair.host.controller.toggleReady();
        pair.client.await(LobbyState::remoteReady);
        assertEquals(Phase.CONNECTED, pair.host.controller.getState().phase());
        assertTrue(pair.host.starts.isEmpty());
        assertTrue(pair.client.starts.isEmpty());
        pair.client.controller.toggleReady();
        MatchStart hostStart = pair.host.awaitStart();
        MatchStart clientStart = pair.client.awaitStart();
        assertEquals(hostStart.matchId(), clientStart.matchId());
        assertEquals(hostStart.settings(), clientStart.settings());
        assertEquals(PeerRole.HOST, hostStart.role());
        assertEquals(PeerRole.CLIENT, clientStart.role());
        pair.host.controller.toggleReady();
        pair.client.controller.toggleReady();
        assertTrue(pair.host.starts.isEmpty());
        assertTrue(pair.client.starts.isEmpty());
        System.out.println("[P2P 시작] 한 명 준비 시 미시작, 양쪽 준비·시작 확인 후 각 1회 콜백 PASS");
    }

    @Test
    void resetReadinessWhenHostChangesSettings() throws Exception {
        Pair pair = connectPair();
        pair.host.controller.toggleReady();
        pair.client.await(LobbyState::remoteReady);
        LobbySettings item = new LobbySettings(LobbyMode.ITEM, Difficulty.EASY, 0);
        pair.host.controller.updateSettings(item);
        LobbyState clientState = pair.client.await(s -> item.equals(s.settings()));
        assertFalse(clientState.localReady());
        assertFalse(clientState.remoteReady());
        assertFalse(pair.host.controller.getState().localReady());
        assertTrue(pair.host.starts.isEmpty());
    }

    @Test
    void reuseConnectionForSecondMatch() throws Exception {
        Pair pair = connectPair();
        pair.host.controller.toggleReady();
        pair.client.controller.toggleReady();
        MatchStart first = pair.host.awaitStart();
        pair.client.awaitStart();
        pair.host.controller.returnToLobby();
        pair.client.await(s -> s.phase() == Phase.CONNECTED && !s.localReady() && !s.remoteReady());
        pair.host.controller.toggleReady();
        pair.client.controller.toggleReady();
        MatchStart second = pair.host.awaitStart();
        MatchStart secondClient = pair.client.awaitStart();
        assertTrue(second.matchId() > first.matchId());
        assertEquals(second.matchId(), secondClient.matchId());
        System.out.println("[P2P 재준비] 연결 유지·준비 초기화·두 번째 시작 번호 " + second.matchId() + " PASS");
    }

    @Test
    void releaseListeningPortOnCancel() throws Exception {
        Endpoint host = endpoint();
        int port = host.openHost();
        host.controller.cancel();
        try (ServerSocket probe = new ServerSocket(port)) {
            assertEquals(port, probe.getLocalPort());
        }
        host.controller.host(port);
        host.await(s -> s.phase() == Phase.HOSTING && s.port() == port);
    }

    @Test
    void reportOccupiedPort() throws Exception {
        try (ServerSocket occupied = new ServerSocket(0)) {
            Endpoint host = endpoint();
            host.controller.host(occupied.getLocalPort());
            LobbyState state = host.await(s -> s.phase() == Phase.IDLE);
            assertTrue(state.message().contains("서버 연결"));
        }
    }

    @Test
    void reportConnectionRefusedAndAllowRetry() throws Exception {
        int unusedPort;
        try (ServerSocket reservation = new ServerSocket(0)) {
            unusedPort = reservation.getLocalPort();
        }
        Endpoint client = endpoint();
        client.controller.connect("127.0.0.1", unusedPort);
        client.await(s -> s.phase() == Phase.IDLE && s.message().contains("접속하지 못했습니다"));
        Endpoint host = endpoint();
        client.controller.connect("127.0.0.1", host.openHost());
        client.await(s -> s.phase() == Phase.CONNECTED);
    }

    @Test
    void reportPeerDepartureAndAllowHostingAgain() throws Exception {
        Pair pair = connectPair();
        pair.client.controller.close();
        pair.host.await(s -> s.phase() == Phase.IDLE);
        pair.host.controller.host(0);
        pair.host.await(s -> s.phase() == Phase.HOSTING && s.port() > 0);
    }

    @Test
    void cancelIncompleteHandshake() throws Exception {
        Endpoint host = endpoint();
        try (Socket peer = new Socket("127.0.0.1", host.openHost())) {
            host.await(s -> s.phase() == Phase.HANDSHAKING);
            host.controller.cancel();
            assertEquals(Phase.IDLE, host.controller.getState().phase());
            peer.setSoTimeout(2_000);
            assertTrue(peer.getInputStream().readAllBytes().length <= MessageCodec.MAX_MESSAGE_BYTES + 4);
        }
    }

    @Test
    void requireStartAcknowledgementAndIgnoreOldReadyMessage() throws Exception {
        Endpoint host = endpoint();
        try (Socket peer = new Socket("127.0.0.1", host.openHost())) {
            peer.setSoTimeout(3_000);
            DataInputStream input = new DataInputStream(peer.getInputStream());
            DataOutputStream output = new DataOutputStream(peer.getOutputStream());
            MessageCodec.read(input);
            MessageCodec.write(output, LobbyMessage.hello(PeerRole.CLIENT));
            LobbyMessage original = MessageCodec.read(input);
            host.await(s -> s.phase() == Phase.CONNECTED);
            host.controller.updateSettings(new LobbySettings(LobbyMode.ITEM, Difficulty.NORMAL, 0));
            LobbyMessage current = MessageCodec.read(input);
            host.states.clear();
            MessageCodec.write(output, LobbyMessage.ready(original.revision(), true));
            MessageCodec.write(output, LobbyMessage.ready(current.revision(), false));

            LobbyState readyState = host.await(s -> s.phase() == Phase.CONNECTED);
            assertFalse(
                    readyState.remoteReady(),
                    "이전 설정의 준비 메시지가 반영되면 안 됩니다.");
            host.controller.toggleReady();
            assertEquals(LobbyMessage.Type.READY, MessageCodec.read(input).type());
            MessageCodec.write(output, LobbyMessage.ready(current.revision(), true));
            LobbyMessage start = MessageCodec.read(input);
            assertEquals(LobbyMessage.Type.START, start.type());
            assertTrue(host.starts.isEmpty());
            MessageCodec.write(output, LobbyMessage.start(LobbyMessage.Type.START_ACK,
                    start.revision(), start.matchId()));
            assertEquals(LobbyMessage.Type.START_COMMIT, MessageCodec.read(input).type());
            assertNotNull(host.awaitStart());
            System.out.println("[P2P 일관성] 이전 설정의 준비 무시·클라이언트 시작 응답 전 미시작 PASS");
        }
    }

    @Test
    void recoverWhenReadyCancellationCrossesStartRequest() throws Exception {
        Endpoint host = endpoint();
        try (Socket peer = new Socket("127.0.0.1", host.openHost())) {
            peer.setSoTimeout(3_000);
            DataInputStream input = new DataInputStream(peer.getInputStream());
            DataOutputStream output = new DataOutputStream(peer.getOutputStream());
            MessageCodec.read(input);
            MessageCodec.write(output, LobbyMessage.hello(PeerRole.CLIENT));
            LobbyMessage config = MessageCodec.read(input);
            host.await(s -> s.phase() == Phase.CONNECTED);
            host.controller.toggleReady();
            MessageCodec.read(input);
            MessageCodec.write(output, LobbyMessage.ready(config.revision(), true));
            MessageCodec.write(output, LobbyMessage.ready(config.revision(), false));
            LobbyMessage start = MessageCodec.read(input);
            MessageCodec.write(output, LobbyMessage.start(LobbyMessage.Type.START_REJECT,
                    start.revision(), start.matchId()));
            LobbyMessage reset = MessageCodec.read(input);
            assertEquals(LobbyMessage.Type.CONFIG, reset.type());
            assertTrue(reset.revision() > config.revision());
            host.await(s -> s.phase() == Phase.CONNECTED && !s.localReady() && !s.remoteReady());
            assertTrue(host.starts.isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "localhost", "192.168.0", "256.1.1.1", "1..1.1", "1.2.3.4.5"})
    void rejectInvalidIpv4BeforeConnecting(String address) {
        Endpoint client = endpoint();
        assertThrows(IllegalArgumentException.class, () -> client.controller.connect(address, 50_000));
        assertEquals(Phase.IDLE, client.controller.getState().phase());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 65_536})
    void rejectInvalidClientPort(int port) {
        Endpoint client = endpoint();
        assertThrows(IllegalArgumentException.class, () -> client.controller.connect("127.0.0.1", port));
    }

    private Pair connectPair() throws Exception {
        Endpoint host = endpoint();
        Endpoint client = endpoint();
        client.controller.connect("127.0.0.1", host.openHost());
        host.await(s -> s.phase() == Phase.CONNECTED);
        client.await(s -> s.phase() == Phase.CONNECTED);
        return new Pair(host, client);
    }

    private Endpoint endpoint() {
        Endpoint endpoint = new Endpoint();
        controllers.add(endpoint.controller);
        return endpoint;
    }

    private record Pair(Endpoint host, Endpoint client) {
    }

    private static final class Endpoint {
        private final BlockingQueue<LobbyState> states = new LinkedBlockingQueue<>();
        private final BlockingQueue<MatchStart> starts = new LinkedBlockingQueue<>();
        private final P2pLobbyController controller = new P2pLobbyController(
                Runnable::run, states::add, starts::add, NORMAL);

        private int openHost() throws Exception {
            controller.host(0);
            return await(s -> s.phase() == Phase.HOSTING && s.port() > 0).port();
        }

        private LobbyState await(Predicate<LobbyState> condition) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (true) {
                long remaining = deadline - System.nanoTime();
                LobbyState state = states.poll(Math.max(0, remaining), TimeUnit.NANOSECONDS);
                assertNotNull(state, "대기실 상태 전환 시간 초과. 현재 상태: " + controller.getState());
                if (condition.test(state)) {
                    return state;
                }
            }
        }

        private MatchStart awaitStart() throws Exception {
            MatchStart start = starts.poll(5, TimeUnit.SECONDS);
            assertNotNull(start, "시작 콜백이 전달되지 않았습니다. 현재 상태: " + controller.getState());
            return start;
        }
    }
}
// AI-assisted code end
