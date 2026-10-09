package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        Map<TetrominoType, Integer> counts = countTypes(new TetrominoGenerator(new Random(RANDOM_SEED)), BASIC_DRAW_COUNT);

        for (TetrominoType type : TetrominoType.values()) {
            assertWithinTolerance(
                    counts.get(type), BASIC_DRAW_COUNT, 1.0 / TetrominoType.values().length);
        }
    }

    @Test
    void generateEasyWeightsWithinTolerance() {
        Map<TetrominoType, Integer> weights = weights(EASY_I_WEIGHT, NON_I_WEIGHT);
        Map<TetrominoType, Integer> counts = countTypes(
                new TetrominoGenerator(new Random(RANDOM_SEED), weights), EASY_DRAW_COUNT);

        double expectedIRatio = (double) EASY_I_WEIGHT
                / (EASY_I_WEIGHT + (TetrominoType.values().length - 1) * NON_I_WEIGHT);
        double expectedOtherRatio = (double) NON_I_WEIGHT
                / (EASY_I_WEIGHT + (TetrominoType.values().length - 1) * NON_I_WEIGHT);
        assertWithinTolerance(counts.get(TetrominoType.I), EASY_DRAW_COUNT, expectedIRatio);
        for (TetrominoType type : TetrominoType.values()) {
            if (type != TetrominoType.I) {
                assertWithinTolerance(counts.get(type), EASY_DRAW_COUNT, expectedOtherRatio);
            }
        }
    }

    @Test
    void generateHardWeightsWithinTolerance() {
        Map<TetrominoType, Integer> counts = countTypes(
                new TetrominoGenerator(new Random(RANDOM_SEED), weights(HARD_I_WEIGHT, NON_I_WEIGHT)),
                HARD_DRAW_COUNT);

    double expectedIRatio = (double) HARD_I_WEIGHT
            / (HARD_I_WEIGHT + (TetrominoType.values().length - 1) * NON_I_WEIGHT);
    double expectedOtherRatio = (double) NON_I_WEIGHT
            / (HARD_I_WEIGHT + (TetrominoType.values().length - 1) * NON_I_WEIGHT);
    assertWithinTolerance(counts.get(TetrominoType.I), HARD_DRAW_COUNT, expectedIRatio);
    for (TetrominoType type : TetrominoType.values()) {
        if (type != TetrominoType.I) {
            assertWithinTolerance(counts.get(type), HARD_DRAW_COUNT, expectedOtherRatio);
        }
    }
    }

    @Test
    void generateMoreEasyIBlocksThanOtherTypes() {
        Map<TetrominoType, Integer> counts = countTypes(
                new TetrominoGenerator(new Random(RANDOM_SEED), weights(EASY_I_WEIGHT, NON_I_WEIGHT)),
                EASY_DRAW_COUNT);

        for (TetrominoType type : TetrominoType.values()) {
            if (type != TetrominoType.I) {
                assertTrue(counts.get(TetrominoType.I) > counts.get(type), type.name());
            }
        }
    }

    @Test
    void generateFewerHardIBlocksThanOtherTypes() {
        Map<TetrominoType, Integer> counts = countTypes(
                new TetrominoGenerator(new Random(RANDOM_SEED), weights(HARD_I_WEIGHT, NON_I_WEIGHT)),
                HARD_DRAW_COUNT);

        for (TetrominoType type : TetrominoType.values()) {
            if (type != TetrominoType.I) {
                assertTrue(counts.get(TetrominoType.I) < counts.get(type), type.name());
            }
        }
    }

    @Test
    void neverGenerateZeroWeightType() {
        Map<TetrominoType, Integer> weights = weights(ONE_WEIGHT, 0);
        Map<TetrominoType, Integer> counts = countTypes(
                new TetrominoGenerator(new Random(RANDOM_SEED), weights), ZERO_WEIGHT_DRAW_COUNT);

        for (TetrominoType type : TetrominoType.values()) {
            if (type != TetrominoType.I) {
                assertEquals(0, counts.get(type));
            }
        }
    }

    @Test
    void alwaysGenerateOnlyWeightedType() {
        Map<TetrominoType, Integer> weights = new EnumMap<>(TetrominoType.class);
        for (TetrominoType type : TetrominoType.values()) {
            weights.put(type, type == TetrominoType.O ? ONE_WEIGHT : 0);
        }

        Map<TetrominoType, Integer> counts = countTypes(
                new TetrominoGenerator(new Random(RANDOM_SEED), weights), ZERO_WEIGHT_DRAW_COUNT);

        assertEquals(ZERO_WEIGHT_DRAW_COUNT, counts.get(TetrominoType.O));
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
        Map<TetrominoType, Integer> source = weights(ONE_WEIGHT, 0);
        TetrominoGenerator generator = new TetrominoGenerator(new Random(RANDOM_SEED), source);
        source.put(TetrominoType.I, 0);
        source.put(TetrominoType.O, ONE_WEIGHT);

        for (int draw = 0; draw < ZERO_WEIGHT_DRAW_COUNT; draw++) {
            assertEquals(TetrominoType.I, generator.generateNext(), "draw " + draw);
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
        Map<TetrominoType, Integer> weights = weights(ONE_WEIGHT, ONE_WEIGHT);
        weights.remove(TetrominoType.L);

        assertThrows(
                IllegalArgumentException.class,
                () -> new TetrominoGenerator(new Random(RANDOM_SEED), weights));
    }

    @Test
    void rejectNegativeWeight() {
        Map<TetrominoType, Integer> weights = weights(ONE_WEIGHT, ONE_WEIGHT);
        weights.put(TetrominoType.I, -ONE_WEIGHT);

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

    private Map<TetrominoType, Integer> countTypes(TetrominoGenerator generator, int drawCount) {
        Map<TetrominoType, Integer> counts = new EnumMap<>(TetrominoType.class);
        for (TetrominoType type : TetrominoType.values()) {
            counts.put(type, 0);
        }
        for (int draw = 0; draw < drawCount; draw++) {
            TetrominoType type = generator.generateNext();
            counts.put(type, counts.get(type) + 1);
        }
        return counts;
    }

    private Map<TetrominoType, Integer> weights(int iWeight, int otherWeight) {
        Map<TetrominoType, Integer> weights = new EnumMap<>(TetrominoType.class);
        for (TetrominoType type : TetrominoType.values()) {
            weights.put(type, type == TetrominoType.I ? iWeight : otherWeight);
        }
        return weights;
    }

    private void assertWithinTolerance(int count, int total, double expectedRatio) {
        double actualRatio = (double) count / total;
        assertTrue(
                Math.abs(actualRatio - expectedRatio) <= expectedRatio * RELATIVE_TOLERANCE,
                "actual ratio " + actualRatio + ", expected " + expectedRatio);
    }
    // AI-assisted code end (테스트 코드)
}
