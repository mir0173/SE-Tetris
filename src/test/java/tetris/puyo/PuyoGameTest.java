package tetris.puyo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import tetris.model.Board;
import tetris.model.GameCommand;
import tetris.model.GameStatus;
import tetris.model.Position;

class PuyoGameTest {
    @Test
    void clearOnlyGroupsConnectedOrthogonally() {
        int[][] cells = new int[PuyoBoard.HEIGHT][PuyoBoard.WIDTH];
        cells[11] = new int[] {1, 1, 1, Board.GARBAGE_CELL, 0, 0};
        cells[10][3] = 1;
        assertTrue(new PuyoBoard(cells).findClear().cells().isEmpty());

        cells[10][2] = 1;
        var clear = new PuyoBoard(cells).findClear();
        assertEquals(List.of(5), clear.groups());
        assertTrue(clear.cells().contains(new Position(11, 3)));
    }

    @Test
    void settleAndScoreTwoChainsThenCancelIncomingGarbage() {
        int[][] cells = new int[PuyoBoard.HEIGHT][PuyoBoard.WIDTH];
        cells[11] = new int[] {1, 1, 1, 1, Board.GARBAGE_CELL, 0};
        cells[10] = new int[] {2, 2, 1, 0, 0, 0};
        cells[9] = new int[] {2, 0, 2, 0, 0, 0};
        PuyoGame game = new PuyoGame(1, cells);
        game.start();
        game.receiveGarbageRows(List.of(new int[] {Board.GARBAGE_CELL, Board.GARBAGE_CELL, 0, 0, 0, 0}));
        for (int column = 0; column < 3; column++) {
            game.handleCommand(GameCommand.MOVE_RIGHT);
        }
        game.handleCommand(GameCommand.HARD_DROP);

        assertEquals(GameStatus.CLEARING, game.getStatus());
        assertEquals(1, game.createSnapshot().getPuyo().chain());
        assertEquals(6, game.createSnapshot().getPuyo().clearing().size());
        assertTrue(game.takeAttackRows().isEmpty());
        assertEquals(480, game.clearStepDelayMillis(200));
        game.finishLineClear();

        assertEquals(1, game.createSnapshot().getPuyo().chain());
        assertTrue(game.createSnapshot().getPuyo().clearing().isEmpty());
        assertEquals(2, game.createSnapshot().getCells()[9][2]);
        assertEquals(75, game.clearStepDelayMillis(200));
        game.finishLineClear();
        assertEquals(2, game.createSnapshot().getCells()[10][2]);
        game.finishLineClear();
        assertEquals(2, game.createSnapshot().getCells()[11][2]);
        assertTrue(game.takeAttackRows().isEmpty());
        game.finishLineClear();

        assertEquals(150, game.clearStepDelayMillis(200));
        assertEquals(1, game.createSnapshot().getPuyo().chain());
        game.finishLineClear();

        assertEquals(2, game.createSnapshot().getPuyo().chain());
        assertEquals(4, game.createSnapshot().getPuyo().clearing().size());
        assertEquals(480, game.clearStepDelayMillis(200));
        game.finishLineClear();
        game.finishLineClear();
        game.finishLineClear();

        assertEquals(GameStatus.PLAYING, game.getStatus());
        assertEquals(442, game.getScore());
        assertEquals(0, game.createSnapshot().getPuyo().pendingGarbage());
        List<int[]> outgoing = game.takeAttackRows();
        assertEquals(1, outgoing.size());
        long garbage = Arrays.stream(outgoing.getFirst()).filter(cell -> cell == Board.GARBAGE_CELL).count();
        assertEquals(4, garbage);
        assertTrue(game.takeAttackRows().isEmpty());
        System.out.printf("[뿌요 연쇄] 2연쇄 점수 %d점, 상쇄 후 방해 블록 %d개%n", game.getScore(), garbage);
    }

    @Test
    void applyPendingGarbageOnlyAfterLock() {
        PuyoGame game = new PuyoGame(1);
        game.start();
        int[] garbage = new int[PuyoBoard.WIDTH];
        Arrays.fill(garbage, Board.GARBAGE_CELL);
        game.receiveGarbageRows(List.of(garbage));
        assertEquals(PuyoBoard.WIDTH, game.getPendingGarbageRowCount());
        assertEquals(0, garbageCells(game));

        game.handleCommand(GameCommand.HARD_DROP);

        assertEquals(0, game.getPendingGarbageRowCount());
        assertEquals(PuyoBoard.WIDTH, garbageCells(game));
    }

    @Test
    void preserveGroundedLockUntilResumeAndIgnoreOldCallback() {
        PuyoGame game = new PuyoGame(1);
        game.start();
        for (int row = 0; row < PuyoBoard.HEIGHT; row++) {
            game.tick();
        }
        long lock = game.getPendingLockDelayId();
        assertTrue(lock > 0);
        game.handleCommand(GameCommand.MOVE_LEFT);
        assertEquals(lock, game.getPendingLockDelayId());
        game.pause();
        assertFalse(game.finishLockDelay(lock));
        game.resume();
        assertTrue(game.finishLockDelay(lock));
        assertFalse(game.finishLockDelay(lock));
    }

    @Test
    void softDropLocksOnContactWithoutWaiting() {
        PuyoGame game = new PuyoGame(1);
        game.start();
        for (int row = 0; row < PuyoBoard.HEIGHT - 2; row++) {
            game.tick();
        }
        assertEquals(0, game.getPendingLockDelayId());
        assertEquals(0, game.createSnapshot().getCells()[11][2]);

        game.handleCommand(GameCommand.SOFT_DROP);

        assertEquals(0, game.getPendingLockDelayId());
        assertTrue(game.createSnapshot().getCells()[11][2] > 0);
        assertTrue(game.createSnapshot().getCells()[0][2] > 0);
    }

    @Test
    void gameOverWhenSpawnIsBlocked() {
        int[][] cells = new int[PuyoBoard.HEIGHT][PuyoBoard.WIDTH];
        cells[0][2] = 1;
        PuyoGame game = new PuyoGame(1, cells);
        game.start();
        assertTrue(game.isGameOver());
        assertEquals(0, game.getPendingLockDelayId());
    }

    private static long garbageCells(PuyoGame game) {
        return Arrays.stream(game.createSnapshot().getCells()).flatMapToInt(Arrays::stream)
                .filter(cell -> cell == Board.GARBAGE_CELL).count();
    }
}
