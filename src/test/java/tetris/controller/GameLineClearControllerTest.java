package tetris.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import tetris.model.Board;
import tetris.model.Difficulty;
import tetris.model.Game;
import tetris.model.GameCommand;
import tetris.model.GameMode;
import tetris.model.GameSnapshot;
import tetris.model.GameStatus;
import tetris.model.PieceType;
import tetris.model.TetrominoGenerator;
import tetris.view.GameView;

/**
 * 줄 삭제 연출 대기를 컨트롤러가 어떻게 다루는지 검증한다.
 * 줄이 완성되면 컨트롤러는 화면에 삭제 대기 상태를 보여 주고,
 * 일정 시간 뒤 Game에 삭제를 마무리하라고 알려야 한다.
 */
class GameLineClearControllerTest {

    // AI-assisted code start
    private static final int SMALL_BOARD_WIDTH = 4;
    private static final int SMALL_BOARD_HEIGHT = 6;
    private static final int NORMAL_BOARD_WIDTH = 10;
    private static final int NORMAL_BOARD_HEIGHT = 20;
    private static final int LINES_TO_REACH_MINIMUM_INTERVAL = 100;

    @Test
    void scheduleLineClearFinishWithMaximumDelayAtInitialSpeed() {
        // given
        Fixture fixture = fixtureWithSmallBoard();

        // when
        dropTwoPiecesToFillTwoLines(fixture);

        // then
        assertEquals(List.of(GameController.MAX_LINE_CLEAR_DELAY_MILLIS), fixture.scheduler.delays);
    }

    @Test
    void renderClearingSnapshotBeforeFinishing() {
        // given
        Fixture fixture = fixtureWithSmallBoard();

        // when
        dropTwoPiecesToFillTwoLines(fixture);

        // then: 화면에는 지워질 줄이 표시된 스냅샷이 전달된다
        GameSnapshot snapshot = fixture.view.lastSnapshot;
        assertEquals(GameStatus.CLEARING, snapshot.getStatus());
        assertEquals(List.of(SMALL_BOARD_HEIGHT - 2, SMALL_BOARD_HEIGHT - 1), snapshot.getClearingRows());
    }

    @Test
    void notFinishLineClearBeforeDelayPasses() {
        // given
        Fixture fixture = fixtureWithSmallBoard();

        // when
        dropTwoPiecesToFillTwoLines(fixture);

        // then: 예약된 작업이 실행되기 전에는 줄이 그대로 남아 있다
        assertEquals(GameStatus.CLEARING, fixture.game.createSnapshot().getStatus());
        assertEquals(0, fixture.game.getLastClearedLines());
    }

    @Test
    void finishLineClearWhenScheduledTaskRuns() {
        // given
        Fixture fixture = fixtureWithSmallBoard();
        dropTwoPiecesToFillTwoLines(fixture);

        // when
        fixture.scheduler.runNext();

        // then
        assertEquals(GameStatus.PLAYING, fixture.game.createSnapshot().getStatus());
        assertEquals(2, fixture.game.getLastClearedLines());
        assertEquals(GameStatus.PLAYING, fixture.view.lastSnapshot.getStatus());
        assertEquals(List.of(), fixture.view.lastSnapshot.getClearingRows());
    }

    @Test
    void stopGravityTimerWhileClearing() {
        // given
        Fixture fixture = fixtureWithSmallBoard();
        assertTrue(fixture.timer.running);

        // when
        dropTwoPiecesToFillTwoLines(fixture);

        // then
        assertFalse(fixture.timer.running);
    }

    @Test
    void restartGravityTimerAfterFinishing() {
        // given
        Fixture fixture = fixtureWithSmallBoard();
        dropTwoPiecesToFillTwoLines(fixture);

        // when
        fixture.scheduler.runNext();

        // then
        assertTrue(fixture.timer.running);
        assertEquals(2, fixture.timer.startCount);
    }

    @Test
    void scheduleOnlyOnceEvenIfCommandsArriveWhileClearing() {
        // given
        Fixture fixture = fixtureWithSmallBoard();
        dropTwoPiecesToFillTwoLines(fixture);

        // when
        fixture.view.commandHandler.accept(GameCommand.MOVE_LEFT);
        fixture.view.commandHandler.accept(GameCommand.ROTATE_CLOCKWISE);
        fixture.view.commandHandler.accept(GameCommand.HARD_DROP);

        // then
        assertEquals(1, fixture.scheduler.tasks.size());
        assertEquals(GameStatus.CLEARING, fixture.game.createSnapshot().getStatus());
    }

    @Test
    void ignoreStaleGravityTickWhileClearing() {
        // given
        Fixture fixture = fixtureWithSmallBoard();
        Runnable staleTick = fixture.timer.task;
        dropTwoPiecesToFillTwoLines(fixture);

        // when
        staleTick.run();

        // then
        assertEquals(GameStatus.CLEARING, fixture.game.createSnapshot().getStatus());
        assertEquals(1, fixture.scheduler.tasks.size());
    }

    @Test
    void scheduleAgainWhenNextLinesAreCompleted() {
        // given: 첫 삭제를 마무리한 뒤 같은 방식으로 한 번 더 줄을 채운다
        Fixture fixture = fixtureWithSmallBoard();
        dropTwoPiecesToFillTwoLines(fixture);
        fixture.scheduler.runNext();

        // when
        dropTwoPiecesToFillTwoLines(fixture);

        // then
        assertEquals(2, fixture.scheduler.delays.size());
        assertEquals(GameStatus.CLEARING, fixture.game.createSnapshot().getStatus());
    }

    @Test
    void notScheduleWhenNoLineIsFull() {
        // given
        Fixture fixture = fixture(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT);

        // when
        fixture.view.commandHandler.accept(GameCommand.HARD_DROP);

        // then
        assertEquals(List.of(), fixture.scheduler.delays);
        assertEquals(GameStatus.PLAYING, fixture.view.lastSnapshot.getStatus());
        assertTrue(fixture.timer.running);
    }

    @Test
    void notScheduleBeforeAnyLineIsCompleted() {
        // given & when
        Fixture fixture = fixtureWithSmallBoard();

        // then
        assertEquals(List.of(), fixture.scheduler.delays);
    }

    // ---------- 낙하 속도에 맞춘 지연 시간 ----------

    @Test
    void useMaximumDelayWhenDropIntervalIsLonger() {
        // given & when
        long delay = GameController.calculateLineClearDelay(GameController.MAX_LINE_CLEAR_DELAY_MILLIS * 5);

        // then
        assertEquals(GameController.MAX_LINE_CLEAR_DELAY_MILLIS, delay);
    }

    @Test
    void useMaximumDelayWhenDropIntervalEqualsMaximum() {
        // given & when
        long delay = GameController.calculateLineClearDelay(GameController.MAX_LINE_CLEAR_DELAY_MILLIS);

        // then
        assertEquals(GameController.MAX_LINE_CLEAR_DELAY_MILLIS, delay);
    }

    @Test
    void useDropIntervalAsDelayWhenBetweenMinimumAndMaximum() {
        // given
        long dropInterval = (GameController.MIN_LINE_CLEAR_DELAY_MILLIS + GameController.MAX_LINE_CLEAR_DELAY_MILLIS) / 2;

        // when
        long delay = GameController.calculateLineClearDelay(dropInterval);

        // then
        assertEquals(dropInterval, delay);
    }

    @Test
    void useMinimumDelayWhenDropIntervalEqualsMinimum() {
        // given & when
        long delay = GameController.calculateLineClearDelay(GameController.MIN_LINE_CLEAR_DELAY_MILLIS);

        // then
        assertEquals(GameController.MIN_LINE_CLEAR_DELAY_MILLIS, delay);
    }

    @Test
    void useMinimumDelayWhenDropIntervalIsShorter() {
        // given & when
        long delay = GameController.calculateLineClearDelay(1L);

        // then
        assertEquals(GameController.MIN_LINE_CLEAR_DELAY_MILLIS, delay);
    }

    @Test
    void keepMinimumNotLongerThanMaximum() {
        // given & when & then
        assertTrue(GameController.MIN_LINE_CLEAR_DELAY_MILLIS > 0);
        assertTrue(GameController.MIN_LINE_CLEAR_DELAY_MILLIS <= GameController.MAX_LINE_CLEAR_DELAY_MILLIS);
    }

    @Test
    void shortenScheduledDelayAsGameGetsFaster() {
        // given: 한 번 떨어뜨릴 때마다 한 줄이 지워지는 4칸 폭 보드
        Fixture fixture = fixture(SMALL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT, PieceType.I);

        // when: 최소 낙하 속도에 이를 때까지 줄을 계속 지운다
        for (int index = 0; index < LINES_TO_REACH_MINIMUM_INTERVAL; index++) {
            fixture.view.commandHandler.accept(GameCommand.HARD_DROP);
            fixture.scheduler.runNext();
        }

        // then: 처음에는 최대 지연이고, 갈수록 짧아지며, 마지막에는 최소 지연이다
        List<Long> delays = fixture.scheduler.delays;
        assertEquals(LINES_TO_REACH_MINIMUM_INTERVAL, delays.size());
        assertEquals(GameController.MAX_LINE_CLEAR_DELAY_MILLIS, delays.get(0));
        assertEquals(GameController.MIN_LINE_CLEAR_DELAY_MILLIS, delays.get(delays.size() - 1));
        for (int index = 1; index < delays.size(); index++) {
            assertTrue(delays.get(index) <= delays.get(index - 1), "index " + index);
        }
    }

    // ---------- 도우미 ----------

    private Fixture fixtureWithSmallBoard() {
        return fixture(SMALL_BOARD_WIDTH, SMALL_BOARD_HEIGHT);
    }

    private Fixture fixture(int width, int height) {
        return fixture(width, height, PieceType.O);
    }

    private Fixture fixture(int width, int height, PieceType type) {
        Game game = new Game(new Board(width, height), constantGenerator(type), Difficulty.NORMAL,
                GameMode.NORMAL);
        FakeView view = new FakeView();
        FakeTimer timer = new FakeTimer();
        FakeScheduler scheduler = new FakeScheduler();
        GameController controller = new GameController(game, view, timer, scheduler, () -> {
        }, score -> {
        });
        controller.start();
        return new Fixture(game, view, timer, scheduler);
    }

    // O 블록 두 개를 양쪽에 떨어뜨려 4칸 폭 보드의 아래 두 줄을 채운다
    private void dropTwoPiecesToFillTwoLines(Fixture fixture) {
        fixture.view.commandHandler.accept(GameCommand.MOVE_LEFT);
        fixture.view.commandHandler.accept(GameCommand.HARD_DROP);
        fixture.view.commandHandler.accept(GameCommand.MOVE_RIGHT);
        fixture.view.commandHandler.accept(GameCommand.HARD_DROP);
    }

    private TetrominoGenerator constantGenerator(PieceType type) {
        return new TetrominoGenerator(new Random(0L)) {
            @Override
            public PieceType generateNext() {
                return type;
            }
        };
    }

    private record Fixture(Game game, FakeView view, FakeTimer timer, FakeScheduler scheduler) {
    }

    private static class FakeView implements GameView {
        private Consumer<GameCommand> commandHandler;
        private GameSnapshot lastSnapshot;

        @Override
        public void setCommandHandler(Consumer<GameCommand> commandHandler) {
            this.commandHandler = commandHandler;
        }

        @Override
        public void render(GameSnapshot snapshot) {
            lastSnapshot = snapshot;
        }

        @Override
        public void show() {
        }

        @Override
        public void close() {
        }
    }

    private static class FakeTimer implements GameTimer {
        private Runnable task;
        private boolean running;
        private int startCount;

        @Override
        public void start(Runnable task) {
            this.task = task;
            running = true;
            startCount++;
        }

        @Override
        public void stop() {
            running = false;
        }

        @Override
        public void setInterval(long intervalMillis) {
        }
    }

    // 예약된 작업을 바로 실행하지 않고 모아 두었다가 테스트가 직접 실행한다
    private static class FakeScheduler implements DelayScheduler {
        private final List<Long> delays = new ArrayList<>();
        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void runAfter(long delayMillis, Runnable task) {
            delays.add(delayMillis);
            tasks.add(task);
        }

        private void runNext() {
            tasks.get(tasks.size() - 1).run();
        }
    }
    // AI-assisted code end
}
