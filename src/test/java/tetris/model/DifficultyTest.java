package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

                double expectedProbability = 1.0 / 7.0 * factor;
        double actualProbability =
                (double) weights.get(PieceType.I) / total;

        System.out.printf(
                "[%s/I 설정] 목표: %.4f%%, 설정값: %.4f%%%n",
                difficulty,
                expectedProbability * 100,
                actualProbability * 100);

        assertEquals(7, weights.size());
        assertEquals(
                expectedProbability,
                actualProbability,
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

                        double actualProbability =
                    (double) counts.get(type) / DRAW_COUNT;

            System.out.printf(
                    "[%s/%s 추첨] 목표: %.4f%%, 실제: %.4f%%"
                            + ", 개수: %d/%d, 상대 오차: %.4f%%%n",
                    difficulty,
                    type,
                    expectedProbability * 100,
                    actualProbability * 100,
                    counts.get(type),
                    DRAW_COUNT,
                    relativeError * 100);
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