package tetris.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import tetris.model.Board;
import tetris.model.Difficulty;
import tetris.model.Game;
import tetris.model.GameCommand;
import tetris.model.GameMode;
import tetris.model.GameSnapshot;
import tetris.model.GameStatus;
import tetris.model.Piece;
import tetris.model.PieceType;
import tetris.model.TetrominoGenerator;
import tetris.view.GameView;

// AI-assisted code start
class GameLockDelayControllerTest {
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;

    @Test
    void retainFloorKickButLockAtOriginalDeadline() {
        Fixture fixture = new Fixture(PieceType.T);
        fixture.dropToFloor();
        fixture.clock.advanceBy(200);
        fixture.input(GameCommand.ROTATE_CLOCKWISE);
        assertEquals(PieceType.T.getCellId(), fixture.game.createSnapshot().getCells()[17][3]);
        fixture.input(GameCommand.MOVE_LEFT);
        fixture.input(GameCommand.MOVE_RIGHT);
        fixture.clock.advanceBy(299);
        assertEquals(0, fixture.game.getLockedPieceCount());

        fixture.clock.advanceBy(1);

        assertEquals(1, fixture.game.getLockedPieceCount());
        assertEquals(PieceType.T.getCellId(), fixture.board.getCell(17, 3));
        System.out.println("[락 지연 월킥] 착지 후 회전·이동 가능, 500ms 만료에 고정 PASS");
    }

    @Test
    void restartDelayOnlyAfterMovingOffLedgeAndLandingAgain() {
        Fixture fixture = new Fixture(PieceType.O);
        fixture.board.lock(new Piece(PieceType.SLOW, 0, 10, 4));
        fixture.drop(8);
        long firstContact = fixture.game.getPendingLockDelayId();
        assertTrue(firstContact > 0);
        fixture.clock.advanceBy(200);
        fixture.input(GameCommand.MOVE_RIGHT);
        assertEquals(0, fixture.game.getPendingLockDelayId());
        fixture.clock.advanceBy(300);
        assertEquals(0, fixture.game.getLockedPieceCount());

        fixture.drop(10);
        assertTrue(fixture.game.getPendingLockDelayId() > firstContact);
        fixture.clock.advanceBy(499);
        assertEquals(0, fixture.game.getLockedPieceCount());
        fixture.clock.advanceBy(1);
        assertEquals(1, fixture.game.getLockedPieceCount());
    }

    @Test
    void preserveRemainingDelayAndIgnoreCallbackFromBeforePause() {
        Fixture fixture = new Fixture(PieceType.O);
        fixture.dropToFloor();
        fixture.clock.advanceBy(200);
        fixture.input(GameCommand.PAUSE);
        assertEquals(GameStatus.PAUSED, fixture.game.getStatus());
        fixture.clock.advanceBy(100);
        fixture.input(GameCommand.PAUSE);
        fixture.clock.advanceBy(299);
        assertEquals(0, fixture.game.getLockedPieceCount());

        fixture.clock.advanceBy(1);

        assertEquals(1, fixture.game.getLockedPieceCount());
        System.out.println("[락 지연 일시정지] 재개 후 남은 300ms 유지, 이전 예약 무시 PASS");
    }

    @Test
    void oldDelayDoesNotLockNewPieceAfterHold() {
        Fixture fixture = new Fixture(PieceType.O);
        fixture.dropToFloor();
        fixture.clock.advanceBy(250);
        fixture.input(GameCommand.HOLD);
        fixture.dropToFloor();
        fixture.clock.advanceBy(250);
        assertEquals(0, fixture.game.getLockedPieceCount());

        fixture.clock.advanceBy(250);

        assertEquals(1, fixture.game.getLockedPieceCount());
    }

    @Test
    void hardDropLocksImmediatelyAndCancelsOldDelay() {
        Fixture fixture = new Fixture(PieceType.O);
        fixture.dropToFloor();
        fixture.input(GameCommand.HARD_DROP);
        assertEquals(1, fixture.game.getLockedPieceCount());

        fixture.clock.advanceBy(500);

        assertEquals(1, fixture.game.getLockedPieceCount());
        System.out.println("[락 지연 하드드롭] 즉시 고정 및 이전 예약 무시 PASS");
    }

    @Test
    void leavingBattleCancelsPendingLock() {
        Fixture fixture = new Fixture(PieceType.O);
        fixture.dropToFloor();
        fixture.input(GameCommand.QUIT_GAME);
        fixture.clock.advanceBy(500);

        assertEquals(GameStatus.QUIT, fixture.game.getStatus());
        assertEquals(0, fixture.game.getLockedPieceCount());
    }

    @Test
    void finishLockThenRunLineClearAnimationAndProduceAttack() {
        Board board = new Board(4, 6);
        board.lock(new Piece(PieceType.O, 0, 4, 2));
        Fixture fixture = new Fixture(PieceType.O, board);
        fixture.input(GameCommand.MOVE_LEFT);
        fixture.dropToFloor();
        assertEquals(GameStatus.PLAYING, fixture.game.getStatus());

        fixture.clock.advanceBy(500);

        assertEquals(GameStatus.CLEARING, fixture.game.getStatus());
        assertEquals(2, fixture.game.takeAttackRows().size());
        fixture.clock.advanceBy(GameController.MAX_LINE_CLEAR_DELAY_MILLIS);
        assertEquals(GameStatus.PLAYING, fixture.game.getStatus());
        assertEquals(2, fixture.game.getLastClearedLines());
        assertEquals(1, fixture.game.getLockedPieceCount());
    }

    private static final class Fixture implements GameView {
        private final Board board;
        private final Game game;
        private final ManualClock clock = new ManualClock();
        private Consumer<GameCommand> commandHandler;

        private Fixture(PieceType type) {
            this(type, new Board(BOARD_WIDTH, BOARD_HEIGHT));
        }

        private Fixture(PieceType type, Board board) {
            this.board = board;
            TetrominoGenerator generator = new TetrominoGenerator(new Random(1)) {
                @Override
                public PieceType generateNext() {
                    return type;
                }
            };
            game = new Game(board, generator, Difficulty.NORMAL, GameMode.NORMAL);
            GameTimer dropTimer = new GameTimer() {
                @Override public void start(Runnable task) { }
                @Override public void stop() { }
                @Override public void setInterval(long intervalMillis) { }
            };
            new GameController(game, this, dropTimer, clock, () -> { }, score -> { }, clock::nanoTime).start();
        }

        private void dropToFloor() {
            drop(board.getHeight() - 2);
        }

        private void drop(int count) {
            for (int i = 0; i < count; i++) {
                input(GameCommand.SOFT_DROP);
            }
        }

        private void input(GameCommand command) {
            commandHandler.accept(command);
        }

        @Override public void setCommandHandler(Consumer<GameCommand> handler) { commandHandler = handler; }
        @Override public void render(GameSnapshot snapshot) { }
        @Override public void show() { }
        @Override public void close() { }
    }

    private static final class ManualClock implements DelayScheduler {
        private final PriorityQueue<Scheduled> tasks = new PriorityQueue<>(Comparator.comparingLong(Scheduled::at));
        private long millis;

        @Override
        public void runAfter(long delayMillis, Runnable task) {
            tasks.add(new Scheduled(millis + delayMillis, task));
        }

        private long nanoTime() {
            return TimeUnit.MILLISECONDS.toNanos(millis);
        }

        private void advanceBy(long duration) {
            long target = millis + duration;
            while (!tasks.isEmpty() && tasks.peek().at() <= target) {
                Scheduled scheduled = tasks.remove();
                millis = scheduled.at();
                scheduled.task().run();
            }
            millis = target;
        }
    }

    private record Scheduled(long at, Runnable task) {
    }
}
// AI-assisted code end
