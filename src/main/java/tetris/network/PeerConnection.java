package tetris.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** 상대와 순서대로 송수신 */
final class PeerConnection implements AutoCloseable {
    private static final int SEND_QUEUE_CAPACITY = 128;
    private static final long HEALTH_CHECK_MILLIS = 100;

    private final Socket socket;
    private final Consumer<LobbyMessage> onMessage;
    private final Consumer<IOException> onFailure;
    private final Consumer<PeerHealth> onHealth;
    private final BlockingQueue<LobbyMessage> outgoing = new ArrayBlockingQueue<>(SEND_QUEUE_CAPACITY);
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ScheduledExecutorService healthWorker = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon(true).name("p2p-health").factory());
    private volatile PeerHeartbeat heartbeat;
    private PeerHealth lastHealth = PeerHealth.MEASURING;
    private Thread reader;
    private Thread writer;

    PeerConnection(Socket socket, Consumer<LobbyMessage> onMessage, Consumer<IOException> onFailure,
            Consumer<PeerHealth> onHealth) {
        this.socket = socket;
        this.onMessage = onMessage;
        this.onFailure = onFailure;
        this.onHealth = onHealth;
    }

    void start() {
        reader = Thread.ofPlatform().daemon(true).name("p2p-reader").start(this::readMessages);
        writer = Thread.ofPlatform().daemon(true).name("p2p-writer").start(this::writeMessages);
    }

    void send(LobbyMessage message) throws IOException {
        if (closed.get() || !outgoing.offer(message)) {
            throw new IOException("연결이 닫혔거나 송신 대기열이 가득 찼습니다.");
        }
    }

    synchronized void finishHandshake() throws IOException {
        if (closed.get()) {
            throw new IOException("이미 연결이 닫혔습니다.");
        }
        socket.setSoTimeout(0);
        if (heartbeat == null) {
            heartbeat = new PeerHeartbeat(System.nanoTime());
            healthWorker.scheduleWithFixedDelay(this::checkHealth, HEALTH_CHECK_MILLIS,
                    HEALTH_CHECK_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public synchronized void close() {
        closed.set(true);
        healthWorker.shutdown();
        closeSocket();
        if (reader != null) {
            reader.interrupt();
        }
        if (writer != null) {
            writer.interrupt();
        }
    }

    private void readMessages() {
        try {
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            while (!closed.get()) {
                LobbyMessage message = MessageCodec.read(input);
                switch (message.type()) {
                    case PING -> send(LobbyMessage.probe(LobbyMessage.Type.PONG, message.probeId()));
                    case PONG -> {
                        PeerHeartbeat current = heartbeat;
                        if (current != null) {
                            current.receivePong(message.probeId(), System.nanoTime());
                        }
                    }
                    default -> onMessage.accept(message);
                }
            }
        } catch (IOException exception) {
            fail(exception);
        }
    }

    private void checkHealth() {
        if (closed.get()) {
            return;
        }
        PeerHeartbeat.Check check = heartbeat.check(System.nanoTime());
        if (check.timedOut()) {
            fail(new SocketTimeoutException("상대가 " + PeerHeartbeat.TIMEOUT_MILLIS / 1_000
                    + "초 동안 응답하지 않았습니다."));
            return;
        }
        try {
            if (check.probeId() != 0) {
                send(LobbyMessage.probe(LobbyMessage.Type.PING, check.probeId()));
            }
        } catch (IOException exception) {
            fail(exception);
            return;
        }
        if (!lastHealth.equals(check.health())) {
            lastHealth = check.health();
            onHealth.accept(lastHealth);
        }
    }

    private void writeMessages() {
        try {
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            while (!closed.get()) {
                MessageCodec.write(output, outgoing.take());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException exception) {
            fail(exception);
        }
    }

    private void fail(IOException exception) {
        if (closed.compareAndSet(false, true)) {
            close();
            onFailure.accept(exception);
        }
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException ignored) {

        }
    }
}
