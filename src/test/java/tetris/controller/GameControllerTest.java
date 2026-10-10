package tetris.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;
import java.util.Random;

import org.junit.jupiter.api.Test;

import tetris.model.Board;
import tetris.model.Game;
import tetris.model.GameCommand;
import tetris.model.GameSnapshot;
import tetris.model.TetrominoGenerator;
import tetris.model.TetrominoType;
import tetris.view.GameView;

class GameControllerTest {

    // AI-assisted code start
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final int GAME_OVER_BOARD_WIDTH = 4;
    private static final int GAME_OVER_BOARD_HEIGHT = 2;
    private static final int EXPECTED_RENDER_COUNT_AFTER_START = 1;
    private static final int EXPECTED_ONE_CALLBACK = 1;
    private static final long EXPECTED_ONE_DROP = 1L;

    @Test
    void startConnectsViewAndTimer() {
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        GameController controller = newController(new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT),
                sequenceGenerator(TetrominoType.O)), view, timer, new Callback());

        controller.start();

        assertNotNull(view.commandHandler);
        assertTrue(view.showCalled);
        assertTrue(view.renderCount >= EXPECTED_RENDER_COUNT_AFTER_START);
        assertEquals(EXPECTED_ONE_CALLBACK, timer.startCount);
        assertTrue(timer.running);
    }

    @Test
    void runTimerTaskTicksAndRenders() {
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        Game game = new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT), sequenceGenerator(TetrominoType.O));
        GameController controller = newController(game, view, timer, new Callback());
        controller.start();
        int renderCountBeforeTick = view.renderCount;

        timer.task.run();

        assertEquals(EXPECTED_ONE_DROP, game.getGravityDropCells());
        assertTrue(view.renderCount > renderCountBeforeTick);
    }

    @Test
    void renderAfterCommand() {
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        GameController controller = newController(
                new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT), sequenceGenerator(TetrominoType.O)),
                view, timer, new Callback());
        controller.start();
        int renderCountBeforeCommand = view.renderCount;

        view.commandHandler.accept(GameCommand.MOVE_LEFT);

        assertTrue(view.renderCount > renderCountBeforeCommand);
    }

    @Test
    void stopTimerAndReturnToMenuOnQuit() {
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        Callback callback = new Callback(timer);
        GameController controller = newController(
                new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT), sequenceGenerator(TetrominoType.O)),
                view, timer, callback);
        controller.start();

        view.commandHandler.accept(GameCommand.QUIT_GAME);

        assertFalse(timer.running);
        assertEquals(EXPECTED_ONE_CALLBACK, callback.count);
        assertFalse(callback.runningWhenCalled);
    }

    @Test
    void stopTimerWithoutReturningToMenuOnGameOverCommand() {
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        Callback callback = new Callback();
        Game game = new Game(new Board(GAME_OVER_BOARD_WIDTH, GAME_OVER_BOARD_HEIGHT),
                sequenceGenerator(TetrominoType.O));
        GameController controller = newController(game, view, timer, callback);
        controller.start();

        view.commandHandler.accept(GameCommand.HARD_DROP);

        assertTrue(game.isGameOver());
        assertFalse(timer.running);
        assertEquals(0, callback.count);
    }

    @Test
    void stopTimerWhenTimerTaskCausesGameOver() {
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        Callback callback = new Callback();
        Game game = new Game(new Board(GAME_OVER_BOARD_WIDTH, GAME_OVER_BOARD_HEIGHT),
                sequenceGenerator(TetrominoType.O));
        GameController controller = newController(game, view, timer, callback);
        controller.start();
        timer.task.run();

        assertTrue(game.isGameOver());
        assertFalse(timer.running);
        assertEquals(0, callback.count);
    }

    @Test
    void ignoreCommandAfterQuit() {
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        Callback callback = new Callback();
        GameController controller = newController(
                new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT), sequenceGenerator(TetrominoType.O)),
                view, timer, callback);
        controller.start();
        view.commandHandler.accept(GameCommand.QUIT_GAME);
        int renderCountAfterQuit = view.renderCount;

        view.commandHandler.accept(GameCommand.MOVE_LEFT);

        assertEquals(EXPECTED_ONE_CALLBACK, callback.count);
        assertEquals(renderCountAfterQuit, view.renderCount);
    }

    private GameController newController(
            Game game, FakeGameView view, FakeGameTimer timer, Callback callback) {
        return new GameController(game, view, timer, callback, score -> 
            {}
        );
    }

    private TetrominoGenerator sequenceGenerator(TetrominoType... types) {
        return new TetrominoGenerator(new Random(0L)) {
            private int index;

            @Override
            public TetrominoType generateNext() {
                TetrominoType type = types[Math.min(index, types.length - 1)];
                index++;
                return type;
            }
        };
    }

    private static class FakeGameView implements GameView {
        private Consumer<GameCommand> commandHandler;
        private int renderCount;
        private boolean showCalled;

        @Override
        public void setCommandHandler(Consumer<GameCommand> commandHandler) {
            this.commandHandler = commandHandler;
        }

        @Override
        public void render(GameSnapshot snapshot) {
            renderCount++;
        }

        @Override
        public void show() {
            showCalled = true;
        }

        @Override
        public void close() {
        }
    }

    private static class FakeGameTimer implements GameTimer {
        private Runnable task;
        private int startCount;
        private boolean running;

        @Override
        public void start(Runnable task) {
            this.task = task;
            startCount++;
            running = true;
        }

        @Override
        public void stop() {
            running = false;
        }

        @Override
        public void setInterval(long intervalMillis) {
        }
    }

    private static class Callback implements Runnable {
        private int count;
        private boolean runningWhenCalled;
        private final FakeGameTimer timer;

        private Callback() {
            this(null);
        }

        private Callback(FakeGameTimer timer) {
            this.timer = timer;
        }

        @Override
        public void run() {
            count++;
            if (timer != null) {
                runningWhenCalled = timer.running;
            }
        }
    }
    // AI-assisted code end (테스트 코드)
}
