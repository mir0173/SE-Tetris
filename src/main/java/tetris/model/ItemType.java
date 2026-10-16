package tetris.model;

public enum ItemType {
    LINE_CLEAR(
            1,
            'L',
            ItemType.DEFAULT_WEIGHT,
            true),
    BLOCK_GENERATE(
            2,
            'B',
            ItemType.DEFAULT_WEIGHT,
            true),
    SLOW(
            3,
            'S',
            ItemType.DEFAULT_WEIGHT,
            false),
    HEAVY_WEIGHT(
            4,
            'O',
            ItemType.DEFAULT_WEIGHT,
            false),
    SPECIAL_I(
            5,
            '@',
            ItemType.DEFAULT_WEIGHT,
            false);

    private static final int DEFAULT_WEIGHT = 10;
    /** 보드 인코딩에 쓰는 아이템 코드 */
    private final int itemCode;
    /** 화면에 출력할 아이템 심볼 */
    private final char symbol;
    /** 기본 블록의 한칸에 박히는 아이템인지 확인하는 변수 */
    private final boolean embedded;
    /** 생성 시 가중치 */
    private final int weight;

    ItemType(int itemCode, char symbol, int weight, boolean embedded) {
        this.itemCode = itemCode;
        this.symbol = symbol;
        this.weight = weight;
        this.embedded = embedded;
    }

    public int getItemCode() {
        return itemCode;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public char getSymbol() {
        return symbol;
    }

    public int getWeight() {
        return weight;
    }
}
