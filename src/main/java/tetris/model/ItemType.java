package tetris.model;

public enum ItemType {
    LINE_CLEAR(
            1,
            'L',
            ItemType.DEFAULT_WEIGHT,
            true,
            true),
    BLOCK_GENERATE(
            2,
            'B',
            ItemType.DEFAULT_WEIGHT,
            true,
            true),
    SLOW(
            3,
            'S',
            ItemType.DEFAULT_WEIGHT,
            false,
            true),
    HEAVY_WEIGHT(
            4,
            'O',
            ItemType.DEFAULT_WEIGHT,
            false,
            false),
    SPECIAL_I(
            5,
            '@',
            ItemType.DEFAULT_WEIGHT,
            false,
            true);

    private static final int DEFAULT_WEIGHT = 10;
    /** 아이템 식별자 */
    private final int itemId;
    /** 화면에 출력할 아이템 심볼 */
    private final char symbol;
    /** 블록형 아이템인지 셀형 아이템인지 확인용 */
    private final boolean embedded;
    /** 아이템이 회전 가능한지 확인용 */
    private final boolean rotatable;
    /** 생성 시 가중치 */
    private final int weight;

    ItemType(int itemId, char symbol, int weight, boolean embedded, boolean rotatable) {
        this.itemId = itemId;
        this.symbol = symbol;
        this.weight = weight;
        this.embedded = embedded;
        this.rotatable = rotatable;
    }

    public int getItemId() {
        return itemId;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public boolean canRotate() {
        return rotatable;
    }

}
