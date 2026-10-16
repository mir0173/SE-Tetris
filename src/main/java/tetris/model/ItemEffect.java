package tetris.model;

public interface ItemEffect {
    void handleLock(Position position);

    void handleDestroyed(Position position);

    void handleLinesCleared(int clearedLineCount);

    void handleFirstLanding(Position position);
}
