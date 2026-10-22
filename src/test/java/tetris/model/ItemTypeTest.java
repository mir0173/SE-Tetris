package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ItemTypeTest {

    // AI-assisted code start
    private static final int UNKNOWN_ITEM_CODE = 0;
    private static final int OTHER_UNKNOWN_ITEM_CODE = 99;
    private static final int MINIMUM_ITEM_CODE = 0;

    @Test
    void findEveryItemTypeByItsCode() {
        for (ItemType type : ItemType.values()) {
            assertEquals(type, ItemType.findTypeByCode(type.getItemCode()));
        }
    }

    @Test
    void returnNullForUnknownItemCode() {
        assertNull(ItemType.findTypeByCode(UNKNOWN_ITEM_CODE));
        assertNull(ItemType.findTypeByCode(OTHER_UNKNOWN_ITEM_CODE));
    }

    @Test
    void mapDedicatedPieceTypes() {
        assertNull(ItemType.LINE_CLEAR.getDedicatedPieceType());
        assertNull(ItemType.BLOCK_GENERATE.getDedicatedPieceType());
        assertEquals(PieceType.SLOW, ItemType.SLOW.getDedicatedPieceType());
        assertEquals(PieceType.HEAVY_WEIGHT, ItemType.HEAVY_WEIGHT.getDedicatedPieceType());
        assertEquals(PieceType.SPECIAL_I, ItemType.SPECIAL_I.getDedicatedPieceType());
    }

    @Test
    void matchEmbeddedStateToDedicatedPieceMapping() {
        for (ItemType type : ItemType.values()) {
            assertEquals(type.getDedicatedPieceType() == null, type.isEmbedded());
        }
    }

    @Test
    void useDistinctPositiveItemCodes() {
        Set<Integer> itemCodes = new HashSet<>();

        for (ItemType type : ItemType.values()) {
            assertTrue(type.getItemCode() > MINIMUM_ITEM_CODE);
            assertTrue(itemCodes.add(type.getItemCode()));
        }
    }
    // AI-assisted code end
}
