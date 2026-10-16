package tetris.model;

public record Item(ItemType type, int cellIndex) {

    private static final int BASIC_PIECE_CELL_COUNT = 3;
    public static final int NOT_EMBEDDED = -1;

    public Item {
        if (type == null) {
            throw new NullPointerException("type is null");
        }
        if (type.isEmbedded()) {
            if (!(cellIndex >= 0 && cellIndex <= BASIC_PIECE_CELL_COUNT)) {
                throw new IllegalArgumentException("cellIndex is out of boundary, cellIndex : " + cellIndex);
            }
        }
        if (!type.isEmbedded()) {
            if (!(cellIndex == NOT_EMBEDDED)) {
                throw new IllegalArgumentException("cellIndex is not for not-embedded-item");
            }
        }
    }

    public boolean isEmbedded() {
        return type.isEmbedded();
    }
}
