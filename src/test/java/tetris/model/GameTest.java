package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GameTest {

    // AI-assisted code start
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final int SMALL_BOARD_WIDTH = 4;
    private static final int SMALL_BOARD_HEIGHT = 6;
    private static final int GAME_OVER_HEIGHT = 2;
    private static final int CELL_ID_I = 1;
    private static final int CELL_ID_O = 2;
    private static final int CELL_ID_T = 3;
    private static final long INITIAL_DROP_INTERVAL = 1_000L;
    private static final long TWO_DROPS = 2L;
    private static final long ONE_DROP = 1L;
    private static final long FIRST_HARD_DROP = 18L;
    private static final long TWO_HARD_DROPS = 34L;
    private static final long ZERO_DROPS = 0L;
    private static final long HARD_DROP_COUNT = 18L;
    private static final int THIRD_LOCKED_PIECE_COUNT = 3;
    private static final int CLEARED_LINES = 2;

    @Test
    void rejectNullBoard() {
        assertThrows(NullPointerException.class, () -> new Game(null, sequenceGenerator(TetrominoType.O)));
    }

    @Test
    void rejectNullGenerator() {
        assertThrows(NullPointerException.class, () -> new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT), null));
    }

    @Test
    void createEmptyReadySnapshotBeforeStart() {
        GameSnapshot snapshot = newGame(TetrominoType.O).createSnapshot();

        assertEquals(GameStatus.READY, snapshot.getStatus());
        assertTrue(snapshot.getNextPieces().isEmpty());
        assertNull(snapshot.getHeldPiece());
        assertEmpty(snapshot.getCells());
    }

    @Test
    void ignoreCommandsAndTicksBeforeStart() {
        Game game = newGame(TetrominoType.O);

        game.handleCommand(GameCommand.MOVE_LEFT);
        game.tick();

        assertEquals(GameStatus.READY, game.createSnapshot().getStatus());
        assertEquals(0, game.getGravityDropCells());
    }

    @Test
    void startWithOBlock() {
        Game game = newGame(TetrominoType.O);
        game.start();

        assertEquals(GameStatus.PLAYING, game.createSnapshot().getStatus());
        assertCells(game.createSnapshot(), Set.of("0,4", "0,5", "1,4", "1,5"), CELL_ID_O);
        assertEquals(List.of(TetrominoType.O), game.createSnapshot().getNextPieces());
    }

    @Test
    void showNextBlockAfterStart() {
        Game game = newGame(TetrominoType.T, TetrominoType.I, TetrominoType.O);
        game.start();

        assertEquals(List.of(TetrominoType.I), game.createSnapshot().getNextPieces());
    }

    @Test
    void showFollowingBlockAfterHardDrop() {
        Game game = newGame(TetrominoType.T, TetrominoType.I, TetrominoType.O);
        game.start();
        game.handleCommand(GameCommand.HARD_DROP);

        assertEquals(List.of(TetrominoType.O), game.createSnapshot().getNextPieces());
        assertCellIds(
                game.createSnapshot(),
                Map.of(
                        "1,3", CELL_ID_I, "1,4", CELL_ID_I, "1,5", CELL_ID_I, "1,6", CELL_ID_I,
                        "18,3", CELL_ID_T, "18,4", CELL_ID_T, "18,5", CELL_ID_T, "19,4", CELL_ID_T));
    }

    @Test
    void ignoreSecondStart() {
        Game game = newGame(TetrominoType.O);
        game.start();
        game.handleCommand(GameCommand.MOVE_LEFT);
        game.start();

        assertCells(game.createSnapshot(), Set.of("0,3", "0,4", "1,3", "1,4"), CELL_ID_O);
    }

    @Test
    void returnInitialDropInterval() {
        assertEquals(INITIAL_DROP_INTERVAL, Game.getInitialDropInterval());
    }

    @Test
    void moveOBlockLeftOnce() {
        Game game = startedGame(TetrominoType.O);
        game.handleCommand(GameCommand.MOVE_LEFT);

        assertCells(game.createSnapshot(), Set.of("0,3", "0,4", "1,3", "1,4"), CELL_ID_O);
    }

    @Test
    void stopOBlockAtLeftWall() {
        Game game = startedGame(TetrominoType.O);
        repeatCommand(game, GameCommand.MOVE_LEFT, 5);

        assertCells(game.createSnapshot(), Set.of("0,0", "0,1", "1,0", "1,1"), CELL_ID_O);
    }

    @Test
    void stopOBlockAtRightWall() {
        Game game = startedGame(TetrominoType.O);
        repeatCommand(game, GameCommand.MOVE_RIGHT, 5);

        assertCells(game.createSnapshot(), Set.of("0,8", "0,9", "1,8", "1,9"), CELL_ID_O);
    }

    @Test
    void rotateIBlockClockwise() {
        Game game = startedGame(TetrominoType.I);
        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);

        assertCells(game.createSnapshot(), Set.of("0,5", "1,5", "2,5", "3,5"), CELL_ID_I);
    }

    @Test
    void moveRotatedIBlockToRightWall() {
        Game game = startedGame(TetrominoType.I);
        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);
        repeatCommand(game, GameCommand.MOVE_RIGHT, 5);

        assertCells(game.createSnapshot(), Set.of("0,9", "1,9", "2,9", "3,9"), CELL_ID_I);
    }

    @Test
    void ignoreRotationOutsideRightWall() {
        Game game = startedGame(TetrominoType.I);
        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);
        repeatCommand(game, GameCommand.MOVE_RIGHT, 5);
        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);

        assertCells(game.createSnapshot(), Set.of("0,9", "1,9", "2,9", "3,9"), CELL_ID_I);
    }

    @Test
    void rotateIBlockBackCounterClockwise() {
        Game game = startedGame(TetrominoType.I);
        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);
        game.handleCommand(GameCommand.ROTATE_COUNTERCLOCKWISE);

        assertCells(game.createSnapshot(), Set.of("1,3", "1,4", "1,5", "1,6"), CELL_ID_I);
    }

    @Test
    void countGravityDrops() {
        Game game = startedGame(TetrominoType.O);
        game.tick();
        game.tick();

        assertEquals(TWO_DROPS, game.getGravityDropCells());
        assertCells(game.createSnapshot(), Set.of("2,4", "2,5", "3,4", "3,5"), CELL_ID_O);
    }

    @Test
    void countSoftDrop() {
        Game game = startedGame(TetrominoType.O);
        game.handleCommand(GameCommand.SOFT_DROP);

        assertEquals(ONE_DROP, game.getSoftDropCells());
        assertEquals(0, game.getGravityDropCells());
    }

    @Test
    void hardDropAndSpawnNextBlock() {
        Game game = startedGame(TetrominoType.O);
        game.handleCommand(GameCommand.HARD_DROP);

        assertEquals(FIRST_HARD_DROP, game.getHardDropCells());
        assertEquals(1, game.getLockedPieceCount());
        assertCells(
                game.createSnapshot(),
                Set.of("0,4", "0,5", "1,4", "1,5", "18,4", "18,5", "19,4", "19,5"),
                CELL_ID_O);
    }

    @Test
    void keepDropCountersSeparateAfterHardDrop() {
        Game game = startedGame(TetrominoType.O);
        game.handleCommand(GameCommand.HARD_DROP);

        assertEquals(HARD_DROP_COUNT, game.getHardDropCells());
        assertEquals(ZERO_DROPS, game.getSoftDropCells());
        assertEquals(ZERO_DROPS, game.getGravityDropCells());
    }

    @Test
    void keepDropCountersSeparateAfterGravityAndSoftDrop() {
        Game game = startedGame(TetrominoType.O);
        game.tick();
        game.handleCommand(GameCommand.SOFT_DROP);

        assertEquals(ONE_DROP, game.getGravityDropCells());
        assertEquals(ONE_DROP, game.getSoftDropCells());
        assertEquals(ZERO_DROPS, game.getHardDropCells());
    }

    @Test
    void lockWhenSoftDropReachesFloor() {
        Game game = startedGame(TetrominoType.O);
        repeatCommand(game, GameCommand.SOFT_DROP, BOARD_HEIGHT - 2);
        long dropsBeforeLock = game.getSoftDropCells();
        game.handleCommand(GameCommand.SOFT_DROP);

        assertEquals(1, game.getLockedPieceCount());
        assertEquals(dropsBeforeLock, game.getSoftDropCells());
    }

    @Test
    void lockWhenTickReachesFloor() {
        Game game = startedGame(TetrominoType.O);
        repeatTick(game, BOARD_HEIGHT - 2);
        long dropsBeforeLock = game.getGravityDropCells();
        game.tick();

        assertEquals(1, game.getLockedPieceCount());
        assertEquals(dropsBeforeLock, game.getGravityDropCells());
    }

    @Test
    void stackTwoHardDroppedBlocks() {
        Game game = startedGame(TetrominoType.O);
        game.handleCommand(GameCommand.HARD_DROP);
        game.handleCommand(GameCommand.HARD_DROP);

        assertEquals(TWO_HARD_DROPS, game.getHardDropCells());
        assertEquals(2, game.getLockedPieceCount());
        assertCells(
                game.createSnapshot(),
                Set.of(
                        "0,4", "0,5", "1,4", "1,5",
                        "16,4", "16,5", "17,4", "17,5",
                        "18,4", "18,5", "19,4", "19,5"),
                CELL_ID_O);
    }

    @Test
    void clearTwoLinesAfterFillingSmallBoard() {
        Game game = new Game(new Board(SMALL_BOARD_WIDTH, SMALL_BOARD_HEIGHT), sequenceGenerator(TetrominoType.O));
        game.start();
        game.handleCommand(GameCommand.MOVE_LEFT);
        game.handleCommand(GameCommand.HARD_DROP);
        game.handleCommand(GameCommand.MOVE_RIGHT);
        game.handleCommand(GameCommand.HARD_DROP);

        assertEquals(CLEARED_LINES, game.getLastClearedLines());
        assertEquals(2, game.getLockedPieceCount());
        assertCells(game.createSnapshot(), Set.of("0,1", "0,2", "1,1", "1,2"), CELL_ID_O);
    }

    @Test
    void reportOnlyMostRecentClearedLines() {
        Game game = new Game(new Board(SMALL_BOARD_WIDTH, SMALL_BOARD_HEIGHT), sequenceGenerator(TetrominoType.O));
        game.start();
        game.handleCommand(GameCommand.MOVE_LEFT);
        game.handleCommand(GameCommand.HARD_DROP);
        game.handleCommand(GameCommand.MOVE_RIGHT);
        game.handleCommand(GameCommand.HARD_DROP);
        game.handleCommand(GameCommand.HARD_DROP);

        assertEquals(0, game.getLastClearedLines());
        assertEquals(THIRD_LOCKED_PIECE_COUNT, game.getLockedPieceCount());
    }

    @Test
    void reportNoClearedLinesWhenNoLineIsFull() {
        Game game = newGame(TetrominoType.O);
        game.start();
        game.handleCommand(GameCommand.HARD_DROP);

        assertEquals(0, game.getLastClearedLines());
    }

    @Test
    void holdFirstBlock() {
        Game game = newGame(TetrominoType.T, TetrominoType.I, TetrominoType.O);
        game.start();
        game.handleCommand(GameCommand.HOLD);

        assertEquals(TetrominoType.T, game.createSnapshot().getHeldPiece());
        assertCells(game.createSnapshot(), Set.of("1,3", "1,4", "1,5", "1,6"), CELL_ID_I);
    }

    @Test
    void ignoreSecondHoldBeforeLocking() {
        Game game = newGame(TetrominoType.T, TetrominoType.I, TetrominoType.O);
        game.start();
        game.handleCommand(GameCommand.HOLD);
        game.handleCommand(GameCommand.HOLD);

        assertEquals(TetrominoType.T, game.createSnapshot().getHeldPiece());
        assertCells(game.createSnapshot(), Set.of("1,3", "1,4", "1,5", "1,6"), CELL_ID_I);
    }

    @Test
    void holdAgainAfterLocking() {
        Game game = newGame(TetrominoType.T, TetrominoType.I, TetrominoType.O);
        game.start();
        game.handleCommand(GameCommand.HOLD);
        game.handleCommand(GameCommand.HARD_DROP);
        game.handleCommand(GameCommand.HOLD);

        assertEquals(TetrominoType.O, game.createSnapshot().getHeldPiece());
        assertCellIds(
                game.createSnapshot(),
                Map.of(
                        "0,3", CELL_ID_T, "0,4", CELL_ID_T, "0,5", CELL_ID_T, "1,4", CELL_ID_T,
                        "19,3", CELL_ID_I, "19,4", CELL_ID_I, "19,5", CELL_ID_I, "19,6", CELL_ID_I));
    }

    @Test
    void resetHeldBlockRotationWhenTakingItOut() {
        Game game = newGame(TetrominoType.I, TetrominoType.O);
        game.start();
        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);
        game.handleCommand(GameCommand.HOLD);
        game.handleCommand(GameCommand.HARD_DROP);
        game.handleCommand(GameCommand.HOLD);

        assertEquals(TetrominoType.O, game.createSnapshot().getHeldPiece());
        assertCellIds(
                        game.createSnapshot(),
                        Map.of(
                                "1,3", CELL_ID_I, "1,4", CELL_ID_I, "1,5", CELL_ID_I, "1,6", CELL_ID_I,
                                "18,4", CELL_ID_O, "18,5", CELL_ID_O,
                                "19,4", CELL_ID_O, "19,5", CELL_ID_O));
    }

    @Test
    void rejectMovementBlockedByLockedBlock() {
        Game game = startedGame(TetrominoType.O);
        game.handleCommand(GameCommand.HARD_DROP);
        game.handleCommand(GameCommand.MOVE_RIGHT);
        game.handleCommand(GameCommand.MOVE_RIGHT);
        repeatCommand(game, GameCommand.SOFT_DROP, BOARD_HEIGHT - 2);
        game.handleCommand(GameCommand.MOVE_LEFT);

        assertCells(
                        game.createSnapshot(),
                        Set.of("18,4", "18,5", "19,4", "19,5", "18,6", "18,7", "19,6", "19,7"),
                        CELL_ID_O);
        assertEquals(1, game.getLockedPieceCount());
    }

    @Test
    void becomeGameOverWhenNextSpawnIsBlocked() {
        Game game = new Game(new Board(SMALL_BOARD_WIDTH, GAME_OVER_HEIGHT), sequenceGenerator(TetrominoType.O));
        game.start();
        game.handleCommand(GameCommand.HARD_DROP);

        assertTrue(game.isGameOver());
        assertEquals(GameStatus.GAMEOVER, game.createSnapshot().getStatus());
        assertFalse(game.isFinished());
    }

    @Test
    void showOnlyLockedCellsAfterGameOver() {
        Game game = new Game(new Board(SMALL_BOARD_WIDTH, GAME_OVER_HEIGHT), sequenceGenerator(TetrominoType.O));
        game.start();
        game.handleCommand(GameCommand.HARD_DROP);

        assertCells(game.createSnapshot(), Set.of("0,1", "0,2", "1,1", "1,2"), CELL_ID_O);
    }

    @Test
    void ignoreCommandsAfterGameOver() {
        Game game = new Game(new Board(SMALL_BOARD_WIDTH, GAME_OVER_HEIGHT), sequenceGenerator(TetrominoType.O));
        game.start();
        game.handleCommand(GameCommand.HARD_DROP);
        long lockedPieces = game.getLockedPieceCount();
        game.handleCommand(GameCommand.MOVE_LEFT);
        game.handleCommand(GameCommand.HARD_DROP);
        game.tick();

        assertEquals(lockedPieces, game.getLockedPieceCount());
    }

    @Test
    void quitWithoutGameOver() {
        Game game = startedGame(TetrominoType.O);
        game.handleCommand(GameCommand.QUIT_GAME);

        assertTrue(game.isFinished());
        assertFalse(game.isGameOver());
    }

    @Test
    void ignoreCommandsAfterQuit() {
        Game game = startedGame(TetrominoType.O);
        game.handleCommand(GameCommand.QUIT_GAME);
        game.handleCommand(GameCommand.MOVE_LEFT);
        game.tick();

        assertTrue(game.isFinished());
        assertCells(game.createSnapshot(), Set.of("0,4", "0,5", "1,4", "1,5"), CELL_ID_O);
    }

    @Test
    void pauseWithoutMovingPiece() {
        Game game = startedGame(TetrominoType.O);
        game.handleCommand(GameCommand.PAUSE);
        game.handleCommand(GameCommand.NONE);

        assertCells(game.createSnapshot(), Set.of("0,4", "0,5", "1,4", "1,5"), CELL_ID_O);
        assertEquals(GameStatus.PAUSED, game.createSnapshot().getStatus());
    }

    private Game newGame(TetrominoType... types) {
        return new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT), sequenceGenerator(types));
    }

    private Game startedGame(TetrominoType... types) {
        Game game = newGame(types);
        game.start();
        return game;
    }

    private TetrominoGenerator sequenceGenerator(TetrominoType... types) {
        return new TetrominoGenerator(new Random(0L)) {
            private int index;

            @Override
            public TetrominoType generateNext() {
                TetrominoType type = types[Math.min(index, types.length - 1)];
                index++;
                return type;
            }
        };
    }

    private void repeatCommand(Game game, GameCommand command, int count) {
        for (int index = 0; index < count; index++) {
            game.handleCommand(command);
        }
    }

    private void repeatTick(Game game, int count) {
        for (int index = 0; index < count; index++) {
            game.tick();
        }
    }

    private void assertCells(GameSnapshot snapshot, Set<String> expectedFilled, int cellId) {
        int[][] cells = snapshot.getCells();
        Set<String> actualFilled = new HashSet<>();
        for (int row = 0; row < cells.length; row++) {
            for (int column = 0; column < cells[row].length; column++) {
                String coordinate = row + "," + column;
                if (expectedFilled.contains(coordinate)) {
                    assertEquals(cellId, cells[row][column], coordinate);
                }
                if (cells[row][column] != Board.EMPTY_CELL) {
                    actualFilled.add(coordinate);
                }
            }
        }
        assertEquals(Set.copyOf(expectedFilled), Set.copyOf(actualFilled));
    }

    private void assertCellIds(GameSnapshot snapshot, Map<String, Integer> expectedCells) {
        int[][] cells = snapshot.getCells();
        Set<String> actualFilled = new HashSet<>();
        for (int row = 0; row < cells.length; row++) {
            for (int column = 0; column < cells[row].length; column++) {
                String coordinate = row + "," + column;
                if (expectedCells.containsKey(coordinate)) {
                    assertEquals(expectedCells.get(coordinate), cells[row][column], coordinate);
                }
                if (cells[row][column] != Board.EMPTY_CELL) {
                    actualFilled.add(coordinate);
                }
            }
        }
        assertEquals(Set.copyOf(expectedCells.keySet()), Set.copyOf(actualFilled));
    }

    private void assertEmpty(int[][] cells) {
        for (int row = 0; row < cells.length; row++) {
            for (int column = 0; column < cells[row].length; column++) {
                assertEquals(Board.EMPTY_CELL, cells[row][column], "row " + row + ", column " + column);
            }
        }
    }
    // AI-assisted code end (테스트 코드)
}
