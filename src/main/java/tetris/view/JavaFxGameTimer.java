package tetris.view;

import tetris.controller.GameTimer;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

/**
 * JavaFX Timeline으로 구현한 게임 타이머
 * 작업은 JavaFX 화면 스레드에서 실행된다.
 */
public class JavaFxGameTimer implements GameTimer {
    private long intervalMillis;
    private Runnable task;
    private Timeline timeline; // 돌고 있지 않을시 null

    public JavaFxGameTimer(long intervalMillis) {
        if (intervalMillis <= 0) {
            throw new IllegalArgumentException("intervalMillis must be positive : " + intervalMillis);
        }
        this.intervalMillis = intervalMillis;
    }

    @Override
    public void start(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task is null");
        }
        stop();
        this.task = task;
        this.timeline = createTimeline();
        timeline.play();
    }

    @Override
    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    @Override
    public void setInterval(long intervalMillis) {
        if (intervalMillis <= 0) {
            throw new IllegalArgumentException("intervalMillis must be positive : " + intervalMillis);
        }
        this.intervalMillis = intervalMillis;
        if (timeline != null) {
            stop();
            timeline = createTimeline();
            timeline.play();
        }
    }

    private Timeline createTimeline() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(intervalMillis), e -> task.run()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        return timeline;
    }
}
