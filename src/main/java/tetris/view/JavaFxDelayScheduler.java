package tetris.view;

import javafx.animation.PauseTransition;
import javafx.util.Duration;
import tetris.controller.DelayScheduler;

/**
 * JavaFX PauseTransition으로 구현한 지연 실행 장치
 * 작업은 JavaFX 화면 스레드에서 실행된다.
 */
// AI-assisted code start
public class JavaFxDelayScheduler implements DelayScheduler {

    @Override
    public void runAfter(long delayMillis, Runnable task) {
        if (task == null) {
            throw new NullPointerException("task is null");
        }
        if (delayMillis <= 0) {
            throw new IllegalArgumentException("delayMillis must be positive : " + delayMillis);
        }
        PauseTransition pause = new PauseTransition(Duration.millis(delayMillis));
        pause.setOnFinished(event -> task.run());
        pause.play();
    }

}
// AI-assisted code end