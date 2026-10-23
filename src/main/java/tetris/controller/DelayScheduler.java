package tetris.controller;

/**
 * 일정 시간이 지난 뒤 작업을 한 번 실행하는 장치
 * 줄 삭제 연출처럼 잠깐 기다렸다가 이어서 처리할 때 사용한다
 */
public interface DelayScheduler {
    /**
     * 지정한 시간이 지난 뒤 작업을 한 번 실행
     *
     * @param delayMillis 기다릴 시간(밀리초)
     * @param task        실행할 작업
     */
    void runAfter(long delayMillis, Runnable task);
}
