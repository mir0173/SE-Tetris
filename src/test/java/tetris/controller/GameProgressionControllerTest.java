package tetris.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import tetris.model.Board;
import tetris.model.Game;
import tetris.model.GameCommand;
import tetris.model.GameSnapshot;
import tetris.model.TetrominoGenerator;
import tetris.model.TetrominoType;
import tetris.view.GameView;

// AI-assisted code start

class GameProgressionControllerTest {

    @Test
    void stopAndRestartTimerOnPauseAndResume() {
        Game game = newGame(10, 20, TetrominoType.O);
        FakeView view = new FakeView();
        FakeTimer timer = new FakeTimer();

        GameController controller = new GameController(
                game, view, timer, () -> {
                }, score -> {
                });

        controller.start();
        view.commandHandler.accept(GameCommand.PAUSE);

        assertFalse(timer.running);
        assertTrue(game.isPaused());

        view.commandHandler.accept(GameCommand.PAUSE);

        assertTrue(timer.running);
        assertEquals(2, timer.startCount);
    }

    @Test
    void returnToMenuOnceWhenQuittingWhilePaused() {
        Game game = newGame(10, 20, TetrominoType.O);
        FakeView view = new FakeView();
        FakeTimer timer = new FakeTimer();
        List<Boolean> runningAtMenu = new ArrayList<>();

        GameController controller = new GameController(
                game,
                view,
                timer,
                () -> runningAtMenu.add(timer.running),
                score -> {
                });

        controller.start();
        view.commandHandler.accept(GameCommand.PAUSE);
        view.commandHandler.accept(GameCommand.QUIT_GAME);
        view.commandHandler.accept(GameCommand.QUIT_GAME);

        assertEquals(List.of(false), runningAtMenu);
    }

    @Test
    void notifyGameOverOnceAfterStoppingTimer() {
        Game game = newGame(4, 4, TetrominoType.O);
        FakeView view = new FakeView();
        FakeTimer timer = new FakeTimer();
        List<Long> scores = new ArrayList<>();
        List<Boolean> runningAtGameOver = new ArrayList<>();

        GameController controller = new GameController(
                game,
                view,
                timer,
                () -> {
                },
                score -> {
                    scores.add(score);
                    runningAtGameOver.add(timer.running);
                });

        controller.start();
        view.commandHandler.accept(GameCommand.HARD_DROP);
        view.commandHandler.accept(GameCommand.HARD_DROP);

        view.commandHandler.accept(GameCommand.HARD_DROP);
        timer.task.run();

        assertEquals(List.of(2L), scores);
        assertEquals(List.of(false), runningAtGameOver);
        assertFalse(timer.running);
    }

    @Test
    void changeTimerIntervalOnlyWhenSpeedChanges() {
        Game game = newGame(4, 20, TetrominoType.I);
        FakeView view = new FakeView();
        FakeTimer timer = new FakeTimer();

        GameController controller = new GameController(
                game, view, timer, () -> {
                }, score -> {
                });

        controller.start();

        for (int index = 0; index < 10; index++) {
            view.commandHandler.accept(GameCommand.HARD_DROP);
        }

        assertEquals(900L, timer.interval);
        assertEquals(1, timer.intervalChangeCount);

        view.commandHandler.accept(GameCommand.MOVE_LEFT);
        view.commandHandler.accept(GameCommand.NONE);

        assertEquals(1, timer.intervalChangeCount);
    }

    private Game newGame(
            int width,
            int height,
            TetrominoType type) {

        TetrominoGenerator generator =
                new TetrominoGenerator(new Random(0L)) {
                    @Override
                    public TetrominoType generateNext() {
                        return type;
                    }
                };

        return new Game(new Board(width, height), generator);
    }

    private static class FakeView implements GameView {

        private Consumer<GameCommand> commandHandler;

        @Override
        public void setCommandHandler(
                Consumer<GameCommand> commandHandler) {
            this.commandHandler = commandHandler;
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

    private static class FakeTimer implements GameTimer {

        private Runnable task;
        private boolean running;
        private int startCount;
        private int intervalChangeCount;
        private long interval = Game.getInitialDropInterval();

        @Override
        public void start(Runnable task) {
            this.task = task;
            running = true;
            startCount++;
        }

        @Override
        public void stop() {
            running = false;
        }

        @Override
        public void setInterval(long intervalMillis) {
            interval = intervalMillis;
            intervalChangeCount++;
        }
    }
}

// AI-assisted code end