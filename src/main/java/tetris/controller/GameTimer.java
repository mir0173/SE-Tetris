package tetris.controller;

/**
 * 게임의 자동 낙하처럼 일정 간격으로 작업을 반복 실행하는 타이머
 */
public interface GameTimer {
    void start(Runnable task);
    
    void stop();

    void setInterval(long intervalMillis);
}
