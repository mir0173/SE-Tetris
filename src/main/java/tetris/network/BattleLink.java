package tetris.network;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.LongFunction;

import tetris.battle.MatchResult;
import tetris.model.GameSnapshot;

/** 대기실 소켓을 유지하며 경기별 송수신을 분리한다.*/
public final class BattleLink implements AutoCloseable {
    private static final int MAX_PENDING_MESSAGES = 128;

    private final Executor uiExecutor;
    private final Consumer<BattleMessage> sender;
    private final Consumer<RuntimeException> onReceiverFailure;
    private final Deque<BattleMessage> pending = new ArrayDeque<>();
    private Consumer<BattleMessage> receiver;
    private long sentSequence;
    private long receivedSequence;
    private boolean drainScheduled;
    private boolean closed;

    BattleLink(Executor uiExecutor, Consumer<BattleMessage> sender) {
        this(uiExecutor, sender, failure -> { });
    }

    BattleLink(Executor uiExecutor, Consumer<BattleMessage> sender, Consumer<RuntimeException> onReceiverFailure) {
        this.uiExecutor = uiExecutor;
        this.sender = sender;
        this.onReceiverFailure = onReceiverFailure;
    }

    public void setReceiver(Consumer<BattleMessage> receiver) {
        synchronized (this) {
            requireOpen();
            this.receiver = Objects.requireNonNull(receiver, "receiver");
        }
        scheduleDrain();
    }

    public void sendState(GameSnapshot state) {
        BattleStateData data = BattleStateData.fromSnapshot(state);
        sender.accept(BattleMessage.state(nextSequence(), data));
    }

    void sendStateIfOpen(GameSnapshot state) {
        sendIfOpen(sequence -> BattleMessage.state(sequence, BattleStateData.fromSnapshot(state)));
    }

    public void sendAttackRows(List<int[]> rows) {
        if (!rows.isEmpty()) {
            sendIfOpen(sequence -> BattleMessage.attack(sequence, rows));
        }
    }

    void sendForfeit() {
        sendIfOpen(sequence -> BattleMessage.control(BattleMessage.Type.FORFEIT, sequence));
    }

    boolean sendResult(MatchResult result) {
        return sendIfOpen(sequence -> BattleMessage.result(sequence, result));
    }

    void acknowledgeResult() {
        sendIfOpen(sequence -> BattleMessage.control(BattleMessage.Type.RESULT_ACK, sequence));
    }

    void sendTimeLeft(int seconds) {
        sendIfOpen(sequence -> BattleMessage.timeLeft(sequence, seconds));
    }

    void receive(BattleMessage message) throws IOException {
        synchronized (this) {
            if (closed || message.sequence() <= receivedSequence) {
                return;
            }
            receivedSequence = message.sequence();
            if (message.type() == BattleMessage.Type.STATE && !pending.isEmpty()
                    && pending.peekLast().type() == BattleMessage.Type.STATE) {
                pending.removeLast();
            }
            if (pending.size() >= MAX_PENDING_MESSAGES) {
                throw new IOException("경기 수신 대기열이 가득 찼습니다.");
            }
            pending.addLast(message);
        }
        scheduleDrain();
    }

    @Override
    public synchronized void close() {
        closed = true;
        pending.clear();
        receiver = null;
    }

    private synchronized long nextSequence() {
        requireOpen();
        return ++sentSequence;
    }

    private boolean sendIfOpen(LongFunction<BattleMessage> createMessage) {
        long sequence;
        synchronized (this) {
            if (closed) {
                return false;
            }
            sequence = ++sentSequence;
        }
        sender.accept(createMessage.apply(sequence));
        synchronized (this) {
            return !closed;
        }
    }

    private void scheduleDrain() {
        synchronized (this) {
            if (closed || receiver == null || pending.isEmpty() || drainScheduled) {
                return;
            }
            drainScheduled = true;
        }
        uiExecutor.execute(this::drain);
    }

    private void drain() {
        while (true) {
            Consumer<BattleMessage> action;
            BattleMessage message;
            synchronized (this) {
                if (closed || receiver == null || pending.isEmpty()) {
                    drainScheduled = false;
                    return;
                }
                action = receiver;
                message = pending.removeFirst();
            }
            try {
                action.accept(message);
            } catch (RuntimeException failure) {
                close();
                onReceiverFailure.accept(failure);
                return;
            }
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("이미 종료된 경기 연결입니다.");
        }
    }
}
