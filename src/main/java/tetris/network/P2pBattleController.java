package tetris.network;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import tetris.battle.LocalPlayer;
import tetris.battle.MatchManager;
import tetris.battle.MatchResult;
import tetris.battle.Player;
import tetris.controller.DelayScheduler;
import tetris.controller.GameController;
import tetris.controller.GameTimer;
import tetris.model.ControlledGame;
import tetris.model.GameCommand;
import tetris.model.GameSnapshot;
import tetris.view.GameView;

/** P2P 경기의 입력, 상태 교환 및 결과 처리를 연결한다. */
public final class P2pBattleController implements AutoCloseable {
    public static final long STATE_INTERVAL_MILLIS = 50;

    private final BattleLink link;
    private final GameController controller;
    private final GameTimer stateTimer;
    private final Player localPlayer;
    private final RemotePlayer remotePlayer;
    private final MatchManager matchManager;
    private final MatchStart matchStart;
    private final GameTimer timeLimitTimer;
    private final Consumer<String> onStatus;
    private final Consumer<MatchResult> onResult;
    private final IntConsumer onTimeLeft;
    private final Runnable onReturnToLobby;
    private GameSnapshot latestState;
    private GameSnapshot publishedState;
    private MatchResult result;
    private boolean started;
    private boolean closed;
    private boolean forfeitRequested;
    private boolean peerReceivedResult;
    private boolean returnRequested;
    private boolean disconnected;
    private Runnable closeWindowAfterResult;

    public P2pBattleController(ControlledGame game, GameView view, GameTimer dropTimer, DelayScheduler delay,
            GameTimer stateTimer, GameTimer timeLimitTimer, BattleLink link, MatchStart matchStart,
            Consumer<GameSnapshot> onRemoteSnapshot, Consumer<String> onStatus,
            Consumer<MatchResult> onResult, IntConsumer onTimeLeft, Runnable onReturnToLobby) {
        this.link = Objects.requireNonNull(link, "link");
        this.stateTimer = stateTimer;
        this.timeLimitTimer = timeLimitTimer;
        this.matchStart = matchStart;
        this.onStatus = onStatus;
        this.onResult = onResult;
        this.onTimeLeft = onTimeLeft;
        this.onReturnToLobby = onReturnToLobby;
        int[][] cells = game.createSnapshot().getCells();
        if (game.kind() != matchStart.settings().gameKind()) {
            throw new IllegalArgumentException("게임 종류가 대기실 설정과 다릅니다.");
        }
        remotePlayer = new RemotePlayer(link, game.kind(), cells[0].length, cells.length, onRemoteSnapshot);

        // 렌더링에 쓰는 스냅샷을 경기 상태 전송에도 사용한다.
        GameView bridge = new GameView() {
            @Override
            public void setCommandHandler(Consumer<GameCommand> handler) {
                view.setCommandHandler(command -> {
                    if (command == GameCommand.QUIT_GAME) {
                        requestReturnToLobby();
                    } else if (!closed && result == null && !forfeitRequested && command != GameCommand.PAUSE) {
                        handler.accept(command);
                    }
                });
            }
            @Override public void render(GameSnapshot snapshot) { latestState = snapshot; view.render(snapshot); }
            @Override public void show() { view.show(); }
            @Override public void close() { view.close(); }
        };
        controller = new GameController(game, bridge, dropTimer, delay, () -> { },
                score -> publishPendingState());
        localPlayer = new LocalPlayer(game, controller);
        matchManager = new MatchManager(localPlayer, remotePlayer, this::finishAsHost, isHost());
    }

    public void start() {
        if (started || closed) {
            return;
        }
        started = true;
        matchManager.start();
        controller.start();
        link.setReceiver(this::receive);
        publishPendingState();
        if (result == null) {
            stateTimer.start(this::publishPendingState);
        }
        if (isHost() && matchStart.settings().timeLimitSeconds() > 0) {
            matchManager.startTimeLimit(timeLimitTimer, matchStart.settings().timeLimitSeconds(), seconds -> {
                onTimeLeft.accept(seconds);
                link.sendTimeLeft(seconds);
            });
        }
    }

    public boolean hasResult() {
        return result != null;
    }

    /** 상대 연결이 끝났음을 반영하고 대기 중인 창 닫기 요청이 있으면 처리한다. */
    public boolean peerDisconnected() {
        disconnected = true;
        return completeWindowClose();
    }

    /** 경기 중 창 닫기는 기권으로 처리하고 결과가 전달될 때까지 연결을 유지한다. */
    public void requestWindowClose(Runnable closeWindow) {
        closeWindowAfterResult = Objects.requireNonNull(closeWindow, "closeWindow");
        if (closed || disconnected || (hasResult() && (!isHost() || peerReceivedResult))) {
            completeWindowClose();
        } else if (!hasResult()) {
            requestReturnToLobby();
        }
    }

    /** 경기 중에는 기권을 요청하고, 결과 화면에서는 대기실 복귀를 요청한다. */
    public void requestReturnToLobby() {
        if (closed || !started) {
            return;
        }
        if (disconnected) {
            onReturnToLobby.run();
            return;
        }
        if (returnRequested) {
            return;
        }
        if (result != null) {
            returnRequested = true;
            if (!isHost() || peerReceivedResult) {
                onReturnToLobby.run();
            }
        } else if (isHost()) {
            matchManager.forfeit(localPlayer);
        } else if (!forfeitRequested) {
            forfeitRequested = true;
            publishPendingState();
            stateTimer.stop();
            localPlayer.stop();
            onStatus.accept("기권을 요청했습니다. 경기 결과를 기다립니다.");
            link.sendForfeit();
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        stateTimer.stop();
        matchManager.cancel();
    }

    private void publishPendingState() {
        if (!closed && result == null && latestState != null && latestState != publishedState) {
            link.sendStateIfOpen(latestState);
            publishedState = latestState;
        }
    }

    private void receive(BattleMessage message) {
        if (closed) {
            return;
        }
        if (result != null) {
            if (isHost() && message.type() == BattleMessage.Type.RESULT_ACK && !peerReceivedResult) {
                peerReceivedResult = true;
                if (!completeWindowClose() && returnRequested) {
                    onReturnToLobby.run();
                }
            }
            return;
        }
        switch (message.type()) {
            case STATE, ATTACK -> remotePlayer.accept(message);
            case FORFEIT -> {
                requireRole(PeerRole.HOST);
                matchManager.forfeit(remotePlayer);
            }
            case RESULT -> {
                requireRole(PeerRole.CLIENT);
                showResult(message.result().swapSides());
                link.acknowledgeResult();
                completeWindowClose();
            }
            case TIME_LEFT -> {
                requireRole(PeerRole.CLIENT);
                if (matchStart.settings().timeLimitSeconds() == 0
                        || message.remainingSeconds() > matchStart.settings().timeLimitSeconds()) {
                    throw new IllegalArgumentException("경기 설정과 남은 시간이 일치하지 않습니다.");
                }
                onTimeLeft.accept(message.remainingSeconds());
            }
            case RESULT_ACK -> { } // 경기 결과를 보낸 뒤에만 확인을 받는다.
        }
    }

    private void finishAsHost(MatchResult outcome) {
        publishPendingState();
        if (link.sendResult(outcome)) {
            showResult(outcome);
        }
    }

    private void showResult(MatchResult outcome) {
        result = outcome;
        stateTimer.stop();
        matchManager.cancel();
        onResult.accept(outcome);
    }

    private boolean completeWindowClose() {
        Runnable closeWindow = closeWindowAfterResult;
        if (closeWindow == null) {
            return false;
        }
        closeWindowAfterResult = null;
        closeWindow.run();
        return true;
    }

    private boolean isHost() {
        return matchStart.role() == PeerRole.HOST;
    }

    private void requireRole(PeerRole role) {
        if (matchStart.role() != role) {
            throw new IllegalArgumentException("상대 역할에서 보낼 수 없는 경기 메시지입니다.");
        }
    }
}
