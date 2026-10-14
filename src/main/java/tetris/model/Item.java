package tetris.model;

public interface Item {
    boolean canRotate();
    void handleLock(Position position);
    void handleDestroy(Position position);
    void handleLineCleared(int line);
    void handleFirstLanding(Position position);
}
