package tetris.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ItemTest {

    // AI-assisted code start
    private static final int FIRST_CELL_INDEX = 0;
    private static final int LAST_CELL_INDEX = Item.MAX_CELL_INDEX;
    private static final int INVALID_CELL_INDEX_BELOW = -2;
    private static final int INVALID_CELL_INDEX_ABOVE = Item.MAX_CELL_INDEX + 1;

    @Test
    void allowEmbeddedItemAtFirstCell() {
        assertDoesNotThrow(() -> new Item(ItemType.LINE_CLEAR, FIRST_CELL_INDEX));
    }

    @Test
    void allowEmbeddedItemAtLastCell() {
        assertDoesNotThrow(() -> new Item(ItemType.LINE_CLEAR, LAST_CELL_INDEX));
    }

    @Test
    void rejectEmbeddedItemOutsideCellRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Item(ItemType.LINE_CLEAR, INVALID_CELL_INDEX_BELOW)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new Item(ItemType.LINE_CLEAR, INVALID_CELL_INDEX_ABOVE)
        );
    }

    @Test
    void allowStandaloneItemWithoutEmbeddedCell() {
        assertDoesNotThrow(() -> new Item(ItemType.SLOW, Item.NOT_EMBEDDED));
    }

    @Test
    void rejectStandaloneItemWithEmbeddedCell() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Item(ItemType.SLOW, FIRST_CELL_INDEX)
        );
    }

    @Test
    void rejectNullItemType() {
        assertThrows(
                NullPointerException.class,
                () -> new Item(null, Item.NOT_EMBEDDED)
        );
    }
    // AI-assisted code end
}
