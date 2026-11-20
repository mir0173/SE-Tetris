package tetris.network;

import java.util.Objects;
import java.util.function.Consumer;

import tetris.battle.LocalPlayer;
import tetris.battle.Player;
import tetris.controller.DelayScheduler;
import tetris.controller.GameController;
import tetris.controller.GameTimer;
import tetris.model.Game;
import tetris.model.GameCommand;
import tetris.model.GameSnapshot;
import tetris.view.GameView;

/** T20 통신 기반 */
public final class P2pBattleController implements AutoCloseable {
    public static final long STATE_INTERVAL_MILLIS = 50;

    private final BattleLink link;
    private final GameController controller;
    private final GameTimer stateTimer;
    private final Player localPlayer;
    private final RemotePlayer remotePlayer;
    private final Consumer<String> onStatus;
    private Runnable matchObserver = () -> { };
    private GameSnapshot latestState;
    private GameSnapshot publishedState;
    private boolean started;
    private boolean closed;

    public P2pBattleController(Game game, GameView view, GameTimer dropTimer, DelayScheduler delay,
            GameTimer stateTimer, BattleLink link, Consumer<GameSnapshot> onRemoteSnapshot,
            Consumer<String> onStatus, Runnable onReturnToLobby) {
        this.link = Objects.requireNonNull(link, "link");
        this.stateTimer = stateTimer;
        this.onStatus = onStatus;
        remotePlayer = new RemotePlayer(link, game.createSnapshot().getCells()[0].length,
                game.createSnapshot().getCells().length, onRemoteSnapshot);

        // 기존 컨트롤러와 화면 사이에서 상태를 받아 별도의 모델 복사를 줄인다.
        GameView bridge = new GameView() {
            @Override
            public void setCommandHandler(Consumer<GameCommand> handler) {
                view.setCommandHandler(command -> {
                    if (closed || command == GameCommand.PAUSE) {
                        return;
                    }
                    if (command == GameCommand.QUIT_GAME) {
                        onReturnToLobby.run();
                    } else {
                        handler.accept(command);
                    }
                });
            }
            @Override public void render(GameSnapshot snapshot) { latestState = snapshot; view.render(snapshot); }
            @Override public void show() { view.show(); }
            @Override public void close() { view.close(); }
        };
        controller = new GameController(game, bridge, dropTimer, delay, () -> { }, score -> { });
        localPlayer = new LocalPlayer(game, controller) {
            @Override
            public void setOnUpdate(Runnable onUpdate) {
                // 심판의 관찰자 등록이 상태 송신 연결을 덮어쓰지 않게 분리한다.
                matchObserver = Objects.requireNonNull(onUpdate, "onUpdate");
            }
        };
        controller.setOnUpdate(() -> {
            if (!closed) {
                matchObserver.run();
            }
        });
    }

    public void start() {
        if (started || closed) {
            return;
        }
        started = true;
        link.setReceiver(this::receive);
        controller.start();
        publishPendingState();
        stateTimer.start(this::publishPendingState);
    }

    /**현재 미완성 부분은 자동 연결하지 않는다. */
    public Player getLocalPlayer() { return localPlayer; }
    public RemotePlayer getRemotePlayer() { return remotePlayer; }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        stateTimer.stop();
        matchObserver = () -> { };
        localPlayer.stop();
    }

    private void publishPendingState() {
        if (!closed && latestState != null && latestState != publishedState) {
            link.sendStateIfOpen(latestState);
            publishedState = latestState;
        }
    }

    private void receive(BattleMessage message) {
        if (closed) {
            return;
        }
        remotePlayer.accept(message);
        if (message.type() == BattleMessage.Type.ATTACK) {
            onStatus.accept("상대 공격 데이터 " + message.rowMasks().size() + "줄 수신");
        } else if (message.type() == BattleMessage.Type.STOP) {
            localPlayer.stop();
            publishPendingState();
            stateTimer.stop();
            onStatus.accept("상대의 경기 중지 요청을 받았습니다.");
        }
    }
}
