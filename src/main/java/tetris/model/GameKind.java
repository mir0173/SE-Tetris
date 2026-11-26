package tetris.model;

public enum GameKind {
    TETRIS("테트리스", 10, 20),
    PUYO("뿌요", 6, 12);

    private final String label;
    private final int width;
    private final int height;

    GameKind(String label, int width, int height) {
        this.label = label;
        this.width = width;
        this.height = height;
    }

    public int width() { return width; }
    public int height() { return height; }

    @Override
    public String toString() { return label; }
}
