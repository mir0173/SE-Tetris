package tetris.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import tetris.controller.DelayScheduler;
import tetris.controller.GameController;
import tetris.controller.GameTimer;
import tetris.model.Board;
import tetris.model.Difficulty;
import tetris.model.Game;
import tetris.model.GameCommand;
import tetris.model.GameMode;
import tetris.model.GameSnapshot;
import tetris.model.GameStatus;
import tetris.model.PieceType;
import tetris.model.TetrominoGenerator;
import tetris.view.GameView;

class LocalPlayerTest {

    // AI-assisted code start
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final int GARBAGE_ROW_VALUE = 1;
    private static final int EXPECTED_ONE_PENDING_ROW = 1;

    @Test // LocalPlayer의 상태, 점수, 스냅샷, 공격 행 메서드가 실제 게임에 연결되는지 확인하는 테스트
    void methodsReachGame() {
        // given
        Fixture fixture = new Fixture();
        LocalPlayer player = fixture.player();

        // when
        GameSnapshot snapshot = player.createSnapshot();
        int[] garbageRow = garbageRow();
        player.receiveGarbageRows(List.of(garbageRow));

        // then
        assertEquals(GameStatus.PLAYING, player.getStatus());
        assertEquals(0L, player.getScore());
        assertFalse(player.isGameOver());
        assertEquals(snapshot.getStatus(), player.createSnapshot().getStatus());
        assertEquals(EXPECTED_ONE_PENDING_ROW, fixture.game().getPendingGarbageRowCount());
        assertTrue(player.takeAttackRows().isEmpty());
        assertEquals(fixture.game().createSnapshot().getScore(), player.getScore());
    }

    @Test // LocalPlayer의 일시정지와 재개 및 종료가 게임과 타이머에 반영되는지 확인하는 테스트
    void pauseResumeAndStopControlGameAndTimer() {
        // given
        Fixture fixture = new Fixture();
        LocalPlayer player = fixture.player();

        // when
        player.pause();

        // then
        assertEquals(GameStatus.PAUSED, player.getStatus());
        assertFalse(fixture.timer().running);

        // when
        player.resume();

        // then
        assertEquals(GameStatus.PLAYING, player.getStatus());
        assertTrue(fixture.timer().running);

        // when
        player.stop();

        // then
        assertEquals(GameStatus.QUIT, player.getStatus());
        assertFalse(fixture.timer().running);
    }

    @Test // LocalPlayer에 등록한 갱신 핸들러가 컨트롤러 렌더링마다 실행되는지 확인하는 테스트
    void setOnUpdateRunsWhenControllerRenders() {
        // given
        Fixture fixture = new Fixture();
        LocalPlayer player = fixture.player();
        int[] updateCount = { 0 };
        player.setOnUpdate(() -> updateCount[0]++);

        // when
        fixture.controller().refresh();

        // then
        assertTrue(updateCount[0] > 0);
    }

    private static int[] garbageRow() {
        int[] row = new int[BOARD_WIDTH];
        row[0] = GARBAGE_ROW_VALUE;
        return row;
    }

    private static class Fixture {
        private final Game game;
        private final GameController controller;
        private final FakeGameView view;
        private final FakeGameTimer timer;
        private final LocalPlayer player;

        private Fixture() {
            this.game = new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT),
                    constantGenerator(), Difficulty.NORMAL, GameMode.NORMAL);
            this.view = new FakeGameView();
            this.timer = new FakeGameTimer();
            DelayScheduler immediateScheduler = (delayMillis, task) -> task.run();
            this.controller = new GameController(game, view, timer, immediateScheduler, () -> {
            }, score -> {
            });
            this.player = new LocalPlayer(game, controller);
            controller.start();
        }

        private Game game() {
            return game;
        }

        private GameController controller() {
            return controller;
        }

        private FakeGameTimer timer() {
            return timer;
        }

        private LocalPlayer player() {
            return player;
        }
    }

    private static TetrominoGenerator constantGenerator() {
        return new TetrominoGenerator(new Random(0L)) {
            @Override
            public PieceType generateNext() {
                return PieceType.O;
            }
        };
    }

    private static class FakeGameView implements GameView {

        @Override
        public void setCommandHandler(Consumer<GameCommand> handler) {
        }

        @Override
        public void render(GameSnapshot snapshot) {
        }

        @Override
        public void show() {
        }

        @Override
        public void close() {
        }
    }

    private static class FakeGameTimer implements GameTimer {
        private boolean running;

        @Override
        public void start(Runnable task) {
            this.running = true;
        }

        @Override
        public void stop() {
            this.running = false;
        }

        @Override
        public void setInterval(long intervalMillis) {
        }
    }
    // AI-assisted code end
}
