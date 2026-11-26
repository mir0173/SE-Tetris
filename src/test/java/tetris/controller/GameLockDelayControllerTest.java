package tetris.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import tetris.puyo.PuyoGame;
import tetris.view.GameView;

// AI-assisted code start
class GameLockDelayControllerTest {
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;

    @Test
    void restartDelayAfterSuccessfulFloorKick() {
        Fixture fixture = new Fixture(PieceType.T);
        fixture.dropToFloor();
        long initialLock = fixture.game.getPendingLockDelayId();
        fixture.clock.advanceBy(200);
        fixture.input(GameCommand.ROTATE_CLOCKWISE);
        assertEquals(PieceType.T.getCellId(), fixture.game.createSnapshot().getCells()[17][3]);
        assertTrue(fixture.game.getPendingLockDelayId() > initialLock);
        fixture.clock.advanceBy(499);
        assertEquals(0, fixture.game.getLockedPieceCount());

        fixture.clock.advanceBy(1);

        assertEquals(1, fixture.game.getLockedPieceCount());
        assertEquals(PieceType.T.getCellId(), fixture.board.getCell(17, 3));
        System.out.println("[테트리스 락 지연] 월킥 성공 후 500ms를 새로 세어 고정 PASS");
    }

    @Test
    void startResetDelayOnRelandingAfterSuccessfulMove() {
        Fixture fixture = new Fixture(PieceType.O);
        fixture.board.lock(new Piece(PieceType.SLOW, 0, 10, 4));
        fixture.drop(8);
        long firstContact = fixture.game.getPendingLockDelayId();
        assertTrue(firstContact > 0);
        fixture.clock.advanceBy(200);
        fixture.input(GameCommand.MOVE_RIGHT);
        assertFalse(fixture.game.isLockDelayRunning());
        long reset = fixture.game.getPendingLockDelayId();
        assertTrue(reset > firstContact);
        fixture.clock.advanceBy(300);
        assertEquals(0, fixture.game.getLockedPieceCount());

        fixture.drop(10);
        assertEquals(reset, fixture.game.getPendingLockDelayId());
        assertTrue(fixture.game.isLockDelayRunning());
        fixture.clock.advanceBy(499);
        assertEquals(0, fixture.game.getLockedPieceCount());
        fixture.clock.advanceBy(1);
        assertEquals(1, fixture.game.getLockedPieceCount());
    }

    @Test
    void allowExactlyFifteenGroundedMovesToResetTheDeadline() {
        Fixture fixture = new Fixture(PieceType.O);
        fixture.dropToFloor();
        long lockId = fixture.game.getPendingLockDelayId();
        for (int used = 0; used < 15; used++) {
            fixture.clock.advanceBy(400);
            fixture.input(used % 2 == 0 ? GameCommand.MOVE_LEFT : GameCommand.MOVE_RIGHT);
            long next = fixture.game.getPendingLockDelayId();
            assertTrue(next > lockId);
            lockId = next;
        }

        fixture.clock.advanceBy(400);
        fixture.input(GameCommand.MOVE_RIGHT);
        assertEquals(lockId, fixture.game.getPendingLockDelayId());
        fixture.clock.advanceBy(99);
        assertEquals(0, fixture.game.getLockedPieceCount());
        fixture.clock.advanceBy(1);
        assertEquals(1, fixture.game.getLockedPieceCount());
        System.out.println("[테트리스 락 지연] 15회 리셋, 16번째 이동 뒤 기존 만료 시각에 고정 PASS");

        fixture.dropToFloor();
        long newPieceLock = fixture.game.getPendingLockDelayId();
        fixture.input(GameCommand.MOVE_LEFT);
        assertTrue(fixture.game.getPendingLockDelayId() > newPieceLock);
    }

    @Test
    void blockedMoveCannotExtendTheDeadline() {
        Fixture fixture = new Fixture(PieceType.O);
        for (int column = 0; column < BOARD_WIDTH; column++) {
            fixture.input(GameCommand.MOVE_LEFT);
        }
        fixture.dropToFloor();
        long lockId = fixture.game.getPendingLockDelayId();
        fixture.clock.advanceBy(400);
        fixture.input(GameCommand.MOVE_LEFT);

        assertEquals(lockId, fixture.game.getPendingLockDelayId());
        fixture.clock.advanceBy(100);
        assertEquals(1, fixture.game.getLockedPieceCount());
    }

    @Test
    void keepTheSpentResetBudgetWhenMovingOffALedge() {
        Fixture fixture = new Fixture(PieceType.O);
        fixture.board.lock(new Piece(PieceType.SLOW, 0, 10, 4));
        fixture.drop(8);
        for (int used = 0; used < 15; used++) {
            fixture.input(GameCommand.ROTATE_CLOCKWISE);
        }
        fixture.clock.advanceBy(200);
        long lockId = fixture.game.getPendingLockDelayId();
        fixture.input(GameCommand.MOVE_RIGHT);
        assertFalse(fixture.game.isLockDelayRunning());
        assertEquals(lockId, fixture.game.getPendingLockDelayId());

        fixture.clock.advanceBy(800);
        fixture.drop(10);
        fixture.input(GameCommand.MOVE_RIGHT);
        assertTrue(fixture.game.isLockDelayRunning());
        assertEquals(lockId, fixture.game.getPendingLockDelayId());
        fixture.clock.advanceBy(299);
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

    @Test
    void puyoCountsOnlyGroundedTimeAcrossMovementAndRelanding() {
        PuyoGame game = new PuyoGame(1);
        ManualClock clock = new ManualClock();
        GameView view = new GameView() {
            @Override public void setCommandHandler(Consumer<GameCommand> handler) { }
            @Override public void render(GameSnapshot snapshot) { }
            @Override public void show() { }
            @Override public void close() { }
        };
        GameTimer timer = new GameTimer() {
            @Override public void start(Runnable action) { }
            @Override public void stop() { }
            @Override public void setInterval(long intervalMillis) { }
        };
        GameController controller = new GameController(game, view, timer, clock, () -> { }, score -> { },
                clock::nanoTime);
        controller.start();

        game.handleCommand(GameCommand.HARD_DROP);
        for (int row = 0; row < 9; row++) {
            game.tick();
        }
        controller.refresh();
        long id = game.getPendingLockDelayId();
        assertTrue(id > 0);
        clock.advanceBy(100);
        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);
        controller.refresh();
        clock.advanceBy(100);
        game.handleCommand(GameCommand.ROTATE_COUNTERCLOCKWISE);
        controller.refresh();
        game.handleCommand(GameCommand.MOVE_LEFT);
        controller.refresh();
        assertFalse(game.isLockDelayRunning());
        assertEquals(id, game.getPendingLockDelayId());

        clock.advanceBy(800);
        assertEquals(id, game.getPendingLockDelayId());
        game.tick();
        game.tick();
        controller.refresh();
        assertTrue(game.isLockDelayRunning());
        clock.advanceBy(332);
        assertEquals(id, game.getPendingLockDelayId());

        clock.advanceBy(1);

        assertEquals(0, game.getPendingLockDelayId());
        System.out.println("[뿌요 고정] 첫 착지 200ms, 공중 800ms 제외, 재착지 333ms 후 고정 PASS");
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
