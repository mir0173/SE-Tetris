package tetris.model;

public record Item(ItemType type, int cellIndex) {

    public static final int NOT_EMBEDDED = -1;
    private static final int MAX_CELL_INDEX = 3;

    public Item {
        if (type == null) {
            throw new NullPointerException("type is null");
        }
        if (type.isEmbedded()) {
            if (!(cellIndex >= 0 && cellIndex <= MAX_CELL_INDEX)) {
                throw new IllegalArgumentException("cellIndex is out of boundary, cellIndex : " + cellIndex);
            }
        }
        if (!type.isEmbedded()) {
            if (cellIndex != NOT_EMBEDDED) {
                throw new IllegalArgumentException("cellIndex is not for not-embedded-item");
            }
        }
    }

    public boolean isEmbedded() {
        return type.isEmbedded();
    }
}
