package tetris.model;

import java.util.List;

/** 컨트롤러와 대전 플레이어가 공통으로 사용하는 게임 모델의 동작 */
public interface ControlledGame {
    default GameKind kind() { return GameKind.TETRIS; }
    void start();
    void tick();
    void handleCommand(GameCommand command);
    void pause();
    void resume();
    void stop();
    boolean isPaused();
    boolean isQuit();
    boolean isGameOver();
    GameStatus getStatus();
    long getScore();
    GameSnapshot createSnapshot();
    void finishLineClear();
    default long clearStepDelayMillis(long defaultDelayMillis) { return defaultDelayMillis; }
    long getPendingLockDelayId();
    /** 현재 고정 대기의 남은 시간이 줄어드는 상태인지 반환한다. */
    default boolean isLockDelayRunning() { return !isPaused(); }
    boolean finishLockDelay(long id);
    default long lockDelayMillis() { return 500L; }
    List<int[]> takeAttackRows();
    void receiveGarbageRows(List<int[]> rows);
    int getPendingGarbageRowCount();
}
