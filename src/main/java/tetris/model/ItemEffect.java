package tetris.model;

public interface ItemEffect {
    void handleLock(Position position);
    void handleDestroy(Position position);
    void handleLineCleared(int line);
    void handleFirstLanding(Position position);
}
