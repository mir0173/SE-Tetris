package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

// AI-assisted code start
class GameDifficultyTest {

    @ParameterizedTest
    @CsvSource({
        "EASY, 920, 2",
        "NORMAL, 900, 2",
        "HARD, 880, 3"
    })
    void applyLineSpeedAndDropScore(
            Difficulty difficulty,
            long expectedInterval,
            long expectedDropScore) {
        Game game = startedGame(
                4, 100, TetrominoType.I, difficulty);

        for (int index = 0; index < 5; index++) {
            game.handleCommand(GameCommand.HARD_DROP);
        }

        assertEquals(
                expectedInterval,
                game.createSnapshot().getDropInterval());

        long scoreBeforeTick = game.createSnapshot().getScore();
        game.tick();

        assertEquals(
                scoreBeforeTick + expectedDropScore,
                game.createSnapshot().getScore());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void keepPieceSpeedRuleAcrossDifficulties(
            Difficulty difficulty) {
        Game game = startedGame(
                10, 100, TetrominoType.O, difficulty);

        for (int index = 0; index < 8; index++) {
            game.handleCommand(GameCommand.HARD_DROP);
        }

        assertEquals(
                1_000L,
                game.createSnapshot().getDropInterval());

        game.handleCommand(GameCommand.HARD_DROP);

        assertEquals(
                900L,
                game.createSnapshot().getDropInterval());
    }

    @ParameterizedTest
    @CsvSource({
        "EASY, 820",
        "NORMAL, 800",
        "HARD, 780"
    })
    void addLineAndPieceSpeedChanges(
            Difficulty difficulty, long expectedInterval) {
        TetrominoGenerator generator =
                new TetrominoGenerator(new Random(0L)) {
            private int generated;

            @Override
            public TetrominoType generateNext() {
                int index = generated++;
                return index < 4 || index >= 8
                        ? TetrominoType.I
                        : TetrominoType.O;
            }
        };

        Game game = new Game(
                new Board(4, 100), generator, difficulty);
        game.start();

        for (int index = 0; index < 9; index++) {
            game.handleCommand(GameCommand.HARD_DROP);
        }

        assertEquals(
                expectedInterval,
                game.createSnapshot().getDropInterval());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void keepMinimumInterval(Difficulty difficulty) {
        Game game = startedGame(
                4, 100, TetrominoType.I, difficulty);

        for (int index = 0; index < 100; index++) {
            game.handleCommand(GameCommand.HARD_DROP);
        }

        assertEquals(
                100L,
                game.createSnapshot().getDropInterval());
    }

    @Test
    void rejectNullDifficulty() {
        assertThrows(
                NullPointerException.class,
                () -> new Game(
                        new Board(10, 20),
                        new TetrominoGenerator(new Random(0L)),
                        null));
    }

    private Game startedGame(
            int width, int height,
            TetrominoType type, Difficulty difficulty) {
        TetrominoGenerator generator =
                new TetrominoGenerator(new Random(0L)) {
            @Override
            public TetrominoType generateNext() {
                return type;
            }
        };

        Game game = new Game(
                new Board(width, height), generator, difficulty);
        game.start();
        return game;
    }
}
// AI-assisted code end