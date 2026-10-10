package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

// AI-assisted code start

class GameProgressionTest {

    @Test
    void scoreActualDropsFromEveryDropType() {
        Game game = startedGame(10, 20, TetrominoType.O);

        game.tick();
        game.handleCommand(GameCommand.SOFT_DROP);
        game.handleCommand(GameCommand.HARD_DROP);

        long actualDrops = game.getGravityDropCells()
                + game.getSoftDropCells()
                + game.getHardDropCells();

        assertEquals(1L, game.getGravityDropCells());
        assertEquals(1L, game.getSoftDropCells());
        assertTrue(game.getHardDropCells() > 0);
        assertEquals(actualDrops, game.createSnapshot().getScore());
    }

    @Test
    void applyLineBonusOnlyOncePerLock() {
        Game game = startedGame(4, 20, TetrominoType.I);

        game.handleCommand(GameCommand.HARD_DROP);

        long expectedScore = game.getHardDropCells() + 100L;
        assertEquals(1, game.getLastClearedLines());
        assertEquals(expectedScore, game.createSnapshot().getScore());

        game.createSnapshot();
        game.createSnapshot();
        game.handleCommand(GameCommand.NONE);
        game.handleCommand(GameCommand.MOVE_LEFT);

        assertEquals(expectedScore, game.createSnapshot().getScore());
    }

    @Test
    void increaseSpeedAndDropScoreAfterTenClearedLines() {
        Game game = startedGame(4, 20, TetrominoType.I);

        for (int index = 0; index < 9; index++) {
            game.handleCommand(GameCommand.HARD_DROP);
        }

        assertEquals(1_000L, game.createSnapshot().getDropInterval());

        game.handleCommand(GameCommand.HARD_DROP);

        assertEquals(900L, game.createSnapshot().getDropInterval());

        long scoreBeforeTick = game.createSnapshot().getScore();
        game.tick();

        assertEquals(
                scoreBeforeTick + 2L,
                game.createSnapshot().getScore());
    }

    @Test
    void keepDropIntervalAtMinimum() {
        Game game = startedGame(4, 20, TetrominoType.I);

        for (int index = 0; index < 100; index++) {
            game.handleCommand(GameCommand.HARD_DROP);
        }

        assertEquals(100L, game.createSnapshot().getDropInterval());
    }

    @Test
    void ignoreMovementAndDropsWhilePaused() {
        Game game = startedGame(10, 20, TetrominoType.O);
        game.handleCommand(GameCommand.PAUSE);

        GameSnapshot before = game.createSnapshot();

        game.tick();
        game.handleCommand(GameCommand.MOVE_LEFT);
        game.handleCommand(GameCommand.HARD_DROP);
        game.handleCommand(GameCommand.HOLD);

        GameSnapshot after = game.createSnapshot();

        assertTrue(game.isPaused());
        assertTrue(Arrays.deepEquals(
                before.getCells(),
                after.getCells()));
        assertEquals(before.getScore(), after.getScore());
        assertEquals(0L, game.getLockedPieceCount());

        game.handleCommand(GameCommand.PAUSE);
        game.tick();

        assertEquals(GameStatus.PLAYING, game.createSnapshot().getStatus());
        assertEquals(1L, game.getGravityDropCells());
    }

    @Test
    void allowQuitWhilePaused() {
        Game game = startedGame(10, 20, TetrominoType.O);

        game.handleCommand(GameCommand.PAUSE);
        game.handleCommand(GameCommand.QUIT_GAME);

        assertTrue(game.isFinished());
        assertEquals(GameStatus.QUIT, game.createSnapshot().getStatus());
    }

    @Test
    void keepFinalScoreAfterGameOver() {
        Game game = startedGame(4, 4, TetrominoType.O);

        game.handleCommand(GameCommand.HARD_DROP);
        game.handleCommand(GameCommand.HARD_DROP);

        assertTrue(game.isGameOver());

        long finalScore = game.createSnapshot().getScore();
        assertEquals(2L, finalScore);

        game.tick();
        game.handleCommand(GameCommand.HARD_DROP);
        game.handleCommand(GameCommand.PAUSE);

        assertTrue(game.isGameOver());
        assertEquals(finalScore, game.createSnapshot().getScore());
    }

    private Game startedGame(
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

        Game game = new Game(new Board(width, height), generator);
        game.start();
        return game;
    }
}

// AI-assisted code end