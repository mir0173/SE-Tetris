package tetris.model;

import tetris.model.effect.LineClearEffect;
import tetris.model.effect.HeavyWeightEffect;
import tetris.model.effect.SpecialIEffect;
import tetris.model.effect.SlowEffect;
import tetris.model.effect.BlockGenerateEffect;

public enum ItemType {
    LINE_CLEAR(
            1,
            'L',
            ItemType.DEFAULT_WEIGHT,
            true,
            null,
            new LineClearEffect()),
    BLOCK_GENERATE(
            2,
            'B',
            ItemType.DEFAULT_WEIGHT,
            true,
            null,
            new BlockGenerateEffect()),
    SLOW(
            3,
            'S',
            ItemType.DEFAULT_WEIGHT,
            false,
            PieceType.SLOW,
            new SlowEffect()),
    HEAVY_WEIGHT(
            4,
            '\u25A3', // ▣
            ItemType.DEFAULT_WEIGHT,
            false,
            PieceType.HEAVY_WEIGHT,
            new HeavyWeightEffect()),
    SPECIAL_I(
            5,
            '\u2605', // ★
            ItemType.DEFAULT_WEIGHT,
            false,
            PieceType.SPECIAL_I,
            new SpecialIEffect());

    private static final int DEFAULT_WEIGHT = 10;
    /** 보드 인코딩에 쓰는 아이템 코드 */
    private final int itemCode;
    /** 화면에 출력할 아이템 심볼 */
    private final char symbol;
    /** 기본 블록의 한칸에 박히는 아이템인지 확인하는 변수 */
    private final boolean embedded;
    /** 생성 시 가중치 */
    private final int weight;
    /** 단독 아이템의 전용 모양, 박히는 아이템이면 null */
    private final PieceType pieceType;
    /** 고정, 파괴 등에 반응하는 이 아이템의 효과 */
    private final ItemEffect itemEffect;

    ItemType(int itemCode, char symbol, int weight, boolean embedded, PieceType pieceType, ItemEffect itemEffect) {
        this.itemCode = itemCode;
        this.symbol = symbol;
        this.weight = weight;
        this.embedded = embedded;
        this.pieceType = pieceType;
        this.itemEffect = itemEffect;
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

    public PieceType getDedicatedPieceType() {
        return pieceType;
    }

    public ItemEffect getItemEffect() {
        return itemEffect;
    }

    /**
     * itemCode에 해당하는 itemType을 반환하는 메서드. itemCode에 해당하는 Type이 없을 시 null을 반환한다.
     * 
     * @param itemCode itemType의 식별 코드
     * @return itemCode에 해당하는 itemType, 없을 시 null
     */
    public static ItemType findTypeByCode(int itemCode) {
        for (ItemType type : ItemType.values()) {
            if (type.getItemCode() == itemCode) {
                return type;
            }
        }
        return null;
    }

    /**
     * itemCode에 해당하는 Symbol을 반환하는 메서드. itemCode에 해당하는 Symbol이 없을 시 null을 반환한다.
     * 
     * @param itemCode itemType의 식별 코드
     * @return itemCode에 해당하는 itemType, 없을 시 null
     */
    public static String findSymbolByCode(int itemCode) {
        for (ItemType type : ItemType.values()) {
            if (type.getItemCode() == itemCode) {
                return String.valueOf(type.getSymbol());
            }
        }
        return null;
    }
}
