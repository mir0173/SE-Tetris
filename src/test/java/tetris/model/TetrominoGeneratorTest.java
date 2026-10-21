package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

class TetrominoGeneratorTest {

    // AI-assisted code start
    private static final int BASIC_DRAW_COUNT = 100_000;
    private static final int EASY_DRAW_COUNT = 100_000;
    private static final int HARD_DRAW_COUNT = 100_000;
    private static final int ZERO_WEIGHT_DRAW_COUNT = 1_000;
    private static final int SAME_SEED_DRAW_COUNT = 100;
    private static final int EASY_I_WEIGHT = 12;
    private static final int HARD_I_WEIGHT = 8;
    private static final int NON_I_WEIGHT = 10;
    private static final int ONE_WEIGHT = 1;
    private static final double RELATIVE_TOLERANCE = 0.05;
    private static final long RANDOM_SEED = 20261009L;

    @Test
    void generateBalancedTypesWithinTolerance() {
        Map<PieceType, Integer> counts = countTypes(new TetrominoGenerator(new Random(RANDOM_SEED)), BASIC_DRAW_COUNT);

        for (PieceType type : basicTypes()) {
            assertWithinTolerance(
                    counts.get(type), BASIC_DRAW_COUNT, 1.0 / basicTypes().length);
        }
    }

    @Test
    void generateEasyWeightsWithinTolerance() {
        Map<PieceType, Integer> weights = weights(EASY_I_WEIGHT, NON_I_WEIGHT);
        Map<PieceType, Integer> counts = countTypes(
                new TetrominoGenerator(new Random(RANDOM_SEED), weights), EASY_DRAW_COUNT);

        double expectedIRatio = (double) EASY_I_WEIGHT
                / (EASY_I_WEIGHT + (basicTypes().length - 1) * NON_I_WEIGHT);
        double expectedOtherRatio = (double) NON_I_WEIGHT
                / (EASY_I_WEIGHT + (basicTypes().length - 1) * NON_I_WEIGHT);
        assertWithinTolerance(counts.get(PieceType.I), EASY_DRAW_COUNT, expectedIRatio);
        for (PieceType type : basicTypes()) {
            if (type != PieceType.I) {
                assertWithinTolerance(counts.get(type), EASY_DRAW_COUNT, expectedOtherRatio);
            }
        }
    }

    @Test
    void generateHardWeightsWithinTolerance() {
        Map<PieceType, Integer> counts = countTypes(
                new TetrominoGenerator(new Random(RANDOM_SEED), weights(HARD_I_WEIGHT, NON_I_WEIGHT)),
                HARD_DRAW_COUNT);

    double expectedIRatio = (double) HARD_I_WEIGHT
            / (HARD_I_WEIGHT + (basicTypes().length - 1) * NON_I_WEIGHT);
    double expectedOtherRatio = (double) NON_I_WEIGHT
            / (HARD_I_WEIGHT + (basicTypes().length - 1) * NON_I_WEIGHT);
    assertWithinTolerance(counts.get(PieceType.I), HARD_DRAW_COUNT, expectedIRatio);
    for (PieceType type : basicTypes()) {
        if (type != PieceType.I) {
            assertWithinTolerance(counts.get(type), HARD_DRAW_COUNT, expectedOtherRatio);
        }
    }
    }

    @Test
    void generateMoreEasyIBlocksThanOtherTypes() {
        Map<PieceType, Integer> counts = countTypes(
                new TetrominoGenerator(new Random(RANDOM_SEED), weights(EASY_I_WEIGHT, NON_I_WEIGHT)),
                EASY_DRAW_COUNT);

        for (PieceType type : basicTypes()) {
            if (type != PieceType.I) {
                assertTrue(counts.get(PieceType.I) > counts.get(type), type.name());
            }
        }
    }

    @Test
    void generateFewerHardIBlocksThanOtherTypes() {
        Map<PieceType, Integer> counts = countTypes(
                new TetrominoGenerator(new Random(RANDOM_SEED), weights(HARD_I_WEIGHT, NON_I_WEIGHT)),
                HARD_DRAW_COUNT);

        for (PieceType type : basicTypes()) {
            if (type != PieceType.I) {
                assertTrue(counts.get(PieceType.I) < counts.get(type), type.name());
            }
        }
    }

    @Test
    void neverGenerateZeroWeightType() {
        Map<PieceType, Integer> weights = weights(ONE_WEIGHT, 0);
        Map<PieceType, Integer> counts = countTypes(
                new TetrominoGenerator(new Random(RANDOM_SEED), weights), ZERO_WEIGHT_DRAW_COUNT);

        for (PieceType type : basicTypes()) {
            if (type != PieceType.I) {
                assertEquals(0, counts.get(type));
            }
        }
    }

    @Test
    void alwaysGenerateOnlyWeightedType() {
        Map<PieceType, Integer> weights = new EnumMap<>(PieceType.class);
        for (PieceType type : basicTypes()) {
            weights.put(type, type == PieceType.O ? ONE_WEIGHT : 0);
        }

        Map<PieceType, Integer> counts = countTypes(
                new TetrominoGenerator(new Random(RANDOM_SEED), weights), ZERO_WEIGHT_DRAW_COUNT);

        assertEquals(ZERO_WEIGHT_DRAW_COUNT, counts.get(PieceType.O));
    }

    @Test
    void generateSameSequenceForSameSeed() {
        TetrominoGenerator first = new TetrominoGenerator(new Random(RANDOM_SEED));
        TetrominoGenerator second = new TetrominoGenerator(new Random(RANDOM_SEED));

        for (int draw = 0; draw < SAME_SEED_DRAW_COUNT; draw++) {
            assertEquals(first.generateNext(), second.generateNext(), "draw " + draw);
        }
    }

    @Test
    void preserveWeightsAfterSourceMapChanges() {
        Map<PieceType, Integer> source = weights(ONE_WEIGHT, 0);
        TetrominoGenerator generator = new TetrominoGenerator(new Random(RANDOM_SEED), source);
        source.put(PieceType.I, 0);
        source.put(PieceType.O, ONE_WEIGHT);

        for (int draw = 0; draw < ZERO_WEIGHT_DRAW_COUNT; draw++) {
            assertEquals(PieceType.I, generator.generateNext(), "draw " + draw);
        }
    }

    @Test
    void rejectNullRandom() {
        assertThrows(NullPointerException.class, () -> new TetrominoGenerator(null));
    }

    @Test
    void rejectNullWeights() {
        assertThrows(
                NullPointerException.class,
                () -> new TetrominoGenerator(new Random(RANDOM_SEED), null));
    }

    @Test
    void rejectMissingType() {
        Map<PieceType, Integer> weights = weights(ONE_WEIGHT, ONE_WEIGHT);
        weights.remove(PieceType.L);

        assertThrows(
                IllegalArgumentException.class,
                () -> new TetrominoGenerator(new Random(RANDOM_SEED), weights));
    }

    @Test
    void rejectNegativeWeight() {
        Map<PieceType, Integer> weights = weights(ONE_WEIGHT, ONE_WEIGHT);
        weights.put(PieceType.I, -ONE_WEIGHT);

        assertThrows(
                IllegalArgumentException.class,
                () -> new TetrominoGenerator(new Random(RANDOM_SEED), weights));
    }

    @Test
    void rejectAllZeroWeights() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TetrominoGenerator(new Random(RANDOM_SEED), weights(0, 0)));
    }

    private Map<PieceType, Integer> countTypes(TetrominoGenerator generator, int drawCount) {
        Map<PieceType, Integer> counts = new EnumMap<>(PieceType.class);
        for (PieceType type : basicTypes()) {
            counts.put(type, 0);
        }
        for (int draw = 0; draw < drawCount; draw++) {
            PieceType type = generator.generateNext();
            counts.put(type, counts.get(type) + 1);
        }
        return counts;
    }

    private Map<PieceType, Integer> weights(int iWeight, int otherWeight) {
        Map<PieceType, Integer> weights = new EnumMap<>(PieceType.class);
        for (PieceType type : basicTypes()) {
            weights.put(type, type == PieceType.I ? iWeight : otherWeight);
        }
        return weights;
    }

    private PieceType[] basicTypes() {
        return Arrays.stream(PieceType.values())
                .filter(PieceType::isBasic)
                .toArray(PieceType[]::new);
    }

    private void assertWithinTolerance(int count, int total, double expectedRatio) {
        double actualRatio = (double) count / total;
        assertTrue(
                Math.abs(actualRatio - expectedRatio) <= expectedRatio * RELATIVE_TOLERANCE,
                "actual ratio " + actualRatio + ", expected " + expectedRatio);
    }
    // AI-assisted code end (테스트 코드)
}
