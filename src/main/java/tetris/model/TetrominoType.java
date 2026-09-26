package tetris.model;

public enum TetrominoType {

    I(1),
    O(2),
    T(3),
    S(4),
    Z(5),
    J(6),
    L(7);

    private final int cellId;

    TetrominoType(int cellId) {
        this.cellId = cellId;
    }

    public int getCellId() {
        return cellId;
    }
}