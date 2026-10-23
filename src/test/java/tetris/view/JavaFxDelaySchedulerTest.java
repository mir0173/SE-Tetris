package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JavaFxDelaySchedulerTest {

    // AI-assisted code start
    private static final long SHORT_DELAY_MILLIS = 50L;
    private static final long WAIT_SECONDS = 5L;

    @BeforeAll
    static void initializeJavaFx() {
        try {
            javafx.application.Platform.startup(() -> {
            });
        } catch (IllegalStateException alreadyStarted) {

        }
        javafx.application.Platform.setImplicitExit(false);
    }

    @Test
    void runTaskOnceAfterDelay() throws Exception {
        // given
        JavaFxDelayScheduler scheduler = new JavaFxDelayScheduler();
        AtomicInteger runCount = new AtomicInteger();
        CountDownLatch finished = new CountDownLatch(1);

        // when
        javafx.application.Platform.runLater(() -> scheduler.runAfter(SHORT_DELAY_MILLIS, () -> {
            runCount.incrementAndGet();
            finished.countDown();
        }));

        // then
        assertTrue(finished.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertEquals(1, runCount.get());
    }

    @Test
    void notRunTaskImmediately() throws Exception {
        // given
        JavaFxDelayScheduler scheduler = new JavaFxDelayScheduler();
        AtomicBoolean ranBeforeReturning = new AtomicBoolean(true);
        CountDownLatch scheduled = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);

        // when
        javafx.application.Platform.runLater(() -> {
            AtomicBoolean ran = new AtomicBoolean(false);
            scheduler.runAfter(SHORT_DELAY_MILLIS, () -> {
                ran.set(true);
                finished.countDown();
            });
            ranBeforeReturning.set(ran.get());
            scheduled.countDown();
        });

        // then: 예약한 직후에는 실행되지 않고, 시간이 지나야 실행된다
        assertTrue(scheduled.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertFalse(ranBeforeReturning.get());
        assertTrue(finished.await(WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    void runTaskOnJavaFxApplicationThread() throws Exception {
        // given
        JavaFxDelayScheduler scheduler = new JavaFxDelayScheduler();
        AtomicBoolean onFxThread = new AtomicBoolean(false);
        CountDownLatch finished = new CountDownLatch(1);

        // when
        javafx.application.Platform.runLater(() -> scheduler.runAfter(SHORT_DELAY_MILLIS, () -> {
            onFxThread.set(javafx.application.Platform.isFxApplicationThread());
            finished.countDown();
        }));

        // then
        assertTrue(finished.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertTrue(onFxThread.get());
    }

    @Test
    void rejectNullTask() {
        // given
        JavaFxDelayScheduler scheduler = new JavaFxDelayScheduler();

        // when & then
        assertThrows(NullPointerException.class, () -> scheduler.runAfter(SHORT_DELAY_MILLIS, null));
    }

    @Test
    void rejectNonPositiveDelay() {
        // given
        JavaFxDelayScheduler scheduler = new JavaFxDelayScheduler();

        // when & then
        assertThrows(IllegalArgumentException.class, () -> scheduler.runAfter(0L, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> scheduler.runAfter(-1L, () -> {
        }));
    }
    // AI-assisted code end
}
