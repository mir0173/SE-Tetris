package tetris.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** 상대와 순서대로 송수신 */
final class PeerConnection implements AutoCloseable {
    private static final int SEND_QUEUE_CAPACITY = 128;

    private final Socket socket;
    private final Consumer<LobbyMessage> onMessage;
    private final Consumer<IOException> onFailure;
    private final BlockingQueue<LobbyMessage> outgoing = new ArrayBlockingQueue<>(SEND_QUEUE_CAPACITY);
    private final AtomicBoolean closed = new AtomicBoolean();
    private Thread reader;
    private Thread writer;

    PeerConnection(Socket socket, Consumer<LobbyMessage> onMessage, Consumer<IOException> onFailure) {
        this.socket = socket;
        this.onMessage = onMessage;
        this.onFailure = onFailure;
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

    void finishHandshake() throws IOException {
        socket.setSoTimeout(0);
    }

    @Override
    public void close() {
        closed.set(true);
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
                onMessage.accept(MessageCodec.read(input));
            }
        } catch (IOException exception) {
            fail(exception);
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
