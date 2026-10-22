package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ItemGeneratorTest {

    // AI-assisted code start
    private static final int ONLY_WEIGHT = 1;
    private static final int ZERO_WEIGHT = 0;
    private static final int SAMPLE_COUNT = 100;
    private static final int RANDOM_SEED = 12345;
    private static final int EXPECTED_CELL_INDEX_COUNT = Item.MAX_CELL_INDEX + 1;
    private static final int NEGATIVE_WEIGHT = -1;

    @ParameterizedTest
    @EnumSource(ItemType.class)
    void generateOnlyWeightedItemType(ItemType selectedType) {
        ItemGenerator generator = new ItemGenerator(new Random(RANDOM_SEED), weightsWithOnly(selectedType, ONLY_WEIGHT));

        for (int count = 0; count < SAMPLE_COUNT; count++) {
            assertEquals(selectedType, generator.generateNext().type());
        }
    }

    @Test
    void generateEveryEmbeddedCellIndexOverEnoughSamples() {
        ItemGenerator generator = new ItemGenerator(
                new Random(RANDOM_SEED),
                weightsWithOnlyEmbeddedItems()
        );
        boolean[] seen = new boolean[EXPECTED_CELL_INDEX_COUNT];

        for (int count = 0; count < SAMPLE_COUNT; count++) {
            int cellIndex = generator.generateNext().cellIndex();
            assertTrue(cellIndex >= 0 && cellIndex <= Item.MAX_CELL_INDEX);
            seen[cellIndex] = true;
        }

        for (boolean cellIndexSeen : seen) {
            assertTrue(cellIndexSeen);
        }
    }

    @Test
    void generateStandaloneItemWithoutEmbeddedCell() {
        ItemGenerator generator = new ItemGenerator(
                new Random(RANDOM_SEED),
                weightsWithOnly(ItemType.SLOW, ONLY_WEIGHT)
        );

        for (int count = 0; count < SAMPLE_COUNT; count++) {
            assertEquals(Item.NOT_EMBEDDED, generator.generateNext().cellIndex());
        }
    }

    @Test
    void produceSameSequenceForSameSeed() {
        ItemGenerator first = new ItemGenerator(new Random(RANDOM_SEED));
        ItemGenerator second = new ItemGenerator(new Random(RANDOM_SEED));

        for (int count = 0; count < SAMPLE_COUNT; count++) {
            assertEquals(first.generateNext(), second.generateNext());
        }
    }

    @Test
    void rejectNullRandom() {
        assertThrows(NullPointerException.class, () -> new ItemGenerator(null, validWeights()));
    }

    @Test
    void rejectNullWeights() {
        assertThrows(NullPointerException.class, () -> new ItemGenerator(new Random(RANDOM_SEED), null));
    }

    @Test
    void rejectMissingItemTypeWeight() {
        Map<ItemType, Integer> weights = validWeights();
        weights.remove(ItemType.SPECIAL_I);

        assertThrows(
                IllegalArgumentException.class,
                () -> new ItemGenerator(new Random(RANDOM_SEED), weights)
        );
    }

    @Test
    void rejectNegativeWeight() {
        Map<ItemType, Integer> weights = validWeights();
        weights.put(ItemType.SLOW, NEGATIVE_WEIGHT);

        assertThrows(
                IllegalArgumentException.class,
                () -> new ItemGenerator(new Random(RANDOM_SEED), weights)
        );
    }

    @Test
    void rejectZeroTotalWeight() {
        Map<ItemType, Integer> weights = zeroWeights();

        assertThrows(
                IllegalArgumentException.class,
                () -> new ItemGenerator(new Random(RANDOM_SEED), weights)
        );
    }

    private Map<ItemType, Integer> validWeights() {
        Map<ItemType, Integer> weights = zeroWeights();
        weights.put(ItemType.LINE_CLEAR, ONLY_WEIGHT);
        return weights;
    }

    private Map<ItemType, Integer> zeroWeights() {
        Map<ItemType, Integer> weights = new EnumMap<>(ItemType.class);
        for (ItemType type : ItemType.values()) {
            weights.put(type, ZERO_WEIGHT);
        }
        return weights;
    }

    private Map<ItemType, Integer> weightsWithOnly(ItemType selectedType, int weight) {
        Map<ItemType, Integer> weights = zeroWeights();
        weights.put(selectedType, weight);
        return weights;
    }

    private Map<ItemType, Integer> weightsWithOnlyEmbeddedItems() {
        Map<ItemType, Integer> weights = zeroWeights();
        weights.put(ItemType.LINE_CLEAR, ONLY_WEIGHT);
        weights.put(ItemType.BLOCK_GENERATE, ONLY_WEIGHT);
        return weights;
    }
    // AI-assisted code end
}
