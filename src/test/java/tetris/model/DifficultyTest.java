package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// AI-assisted code start
class DifficultyTest {

    private static final int DRAW_COUNT = 100_000;
    private static final long RANDOM_SEED = 20261018L;
    private static final double RELATIVE_TOLERANCE = 0.05;

    @ParameterizedTest
    @CsvSource({
        "EASY, 1.2",
        "NORMAL, 1.0",
        "HARD, 0.8"
    })
    void changeActualIProbabilityByTwentyPercent(
            Difficulty difficulty, double factor) {
        Map<PieceType, Integer> weights =
                difficulty.createWeights();

        int total = weights.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        assertEquals(7, weights.size());
        assertEquals(
                1.0 / 7.0 * factor,
                (double) weights.get(PieceType.I) / total,
                1.0e-12);
    }

    @ParameterizedTest
    @CsvSource({
        "EASY, 1.2",
        "NORMAL, 1.0",
        "HARD, 0.8"
    })
    void generateEveryTypeWithinFivePercentRelativeError(
            Difficulty difficulty, double factor) {
        TetrominoGenerator generator = new TetrominoGenerator(
                new Random(RANDOM_SEED),
                difficulty.createWeights());

        Map<PieceType, Integer> counts =
                new EnumMap<>(PieceType.class);

        for (PieceType type : basicTypes()) {
            counts.put(type, 0);
        }

        for (int index = 0; index < DRAW_COUNT; index++) {
            counts.merge(generator.generateNext(), 1, Integer::sum);
        }

        double iProbability = 1.0 / 7.0 * factor;
        double otherProbability = (1.0 - iProbability) / 6.0;

        for (PieceType type : basicTypes()) {
            double expectedProbability =
                    type == PieceType.I
                            ? iProbability
                            : otherProbability;

            double expectedCount =
                    DRAW_COUNT * expectedProbability;

            double relativeError =
                    Math.abs(counts.get(type) - expectedCount)
                    / expectedCount;

            assertTrue(
                    relativeError <= RELATIVE_TOLERANCE,
                    difficulty + "/" + type
                            + ": relativeError=" + relativeError);
        }
    }

    private PieceType[] basicTypes() {
        return Arrays.stream(PieceType.values())
                .filter(PieceType::isBasic)
                .toArray(PieceType[]::new);
    }

    @Test
    void preventExternalWeightChanges() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> Difficulty.EASY.createWeights()
                        .put(PieceType.I, 0));
    }
}
// AI-assisted code end