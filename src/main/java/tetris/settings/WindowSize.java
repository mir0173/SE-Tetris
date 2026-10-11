package tetris.settings;

public enum WindowSize {
    SMALL(0.8),
    MEDIUM(1.0),
    LARGE(1.2);

    private final double scale;

    WindowSize(double scale) {
        this.scale = scale;
    }

    public double getScale() {
        return scale;
    }
}