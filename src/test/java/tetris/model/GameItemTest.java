package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class GameItemTest {

    // AI-assisted code start
    private static final int BOARD_WIDTH = 4;
    private static final int BOARD_HEIGHT = 8;
    private static final int ITEM_SPAWN_LINE_COUNT = 10;
    private static final int TETRIS_LINE_COUNT = 4;
    private static final int VERTICAL_COLUMN = 2;
    private static final int BOTTOM_ROW = BOARD_HEIGHT - 1;
    private static final int DRILL_DROP_INTERVAL = 50;
    private static final int SLOW_DURATION_MILLISECONDS = 10_000;
    private static final long RANDOM_SEED = 0L;

    @Test
    void spawnItemAfterTenthClearedLineInItemMode() {
        GameFixture fixture = itemModeGame(ItemType.SLOW, PieceType.I);
        Game game = fixture.game();
        game.start();

        hardDropAndClearLines(game, ITEM_SPAWN_LINE_COUNT);

        GameSnapshot snapshot = game.createSnapshot();
        assertEquals(List.of(PieceType.SLOW), snapshot.getNextPieces());
    }

    @Test
    void doNotSpawnItemAfterTenthClearedLineInNormalMode() {
        GameFixture fixture = normalModeGame(PieceType.I);
        Game game = fixture.game();
        game.start();

        hardDropAndClearLines(game, ITEM_SPAWN_LINE_COUNT);

        GameSnapshot snapshot = game.createSnapshot();
        assertEquals(List.of(PieceType.I), snapshot.getNextPieces());
        assertFalse(hasAnyItemCode(snapshot));
    }

    @Test
    void carryExcessClearedLinesTowardNextItem() {
        GameFixture fixture = itemModeGame(ItemType.SLOW, PieceType.I);
        Game game = fixture.game();
        game.start();
        hardDropAndClearLines(game, ITEM_SPAWN_LINE_COUNT - 1);

        prepareVerticalTetris(fixture.board(), VERTICAL_COLUMN);
        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);
        hardDropAndFinishClearing(game);
        assertEquals(List.of(PieceType.SLOW), game.createSnapshot().getNextPieces());

        hardDropAndFinishClearing(game);
        hardDropAndClearLines(game, 6);
        assertEquals(List.of(PieceType.I), game.createSnapshot().getNextPieces());

        hardDropAndFinishClearing(game);
        assertEquals(List.of(PieceType.SLOW), game.createSnapshot().getNextPieces());
    }

    @Test
    void clearLineContainingLineClearItemImmediately() {
        GameFixture fixture = itemModeGame(ItemType.LINE_CLEAR, PieceType.I);
        Game game = fixture.game();
        game.start();
        hardDropAndClearLines(game, ITEM_SPAWN_LINE_COUNT);
        hardDropAndFinishClearing(game);
        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);

        GameSnapshot before = game.createSnapshot();
        Position itemPosition = findItemPosition(before, ItemType.LINE_CLEAR);
        assertTrue(itemPosition.row() > 0);
        assertTrue(countNonEmptyCells(before, itemPosition.row()) < BOARD_WIDTH);
        int markerColumn = findEmptyColumn(before, itemPosition.row() - 1);
        fixture.board().lock(new Piece(PieceType.SLOW, 0, itemPosition.row() - 1, markerColumn));
        hardDropAndFinishClearing(game);

        assertEquals(1, game.getLastClearedLines());
        assertFalse(hasItemCode(game.createSnapshot(), ItemType.LINE_CLEAR));
        assertEquals(PieceType.SLOW.getCellId(), game.createSnapshot().getCells()[itemPosition.row()][markerColumn]);
    }

    @Test
    void drillWithHeavyWeightItemAndIgnoreInputUntilFloor() {
        GameFixture fixture = itemModeGame(ItemType.HEAVY_WEIGHT, PieceType.I);
        Game game = fixture.game();
        game.start();
        hardDropAndClearLines(game, ITEM_SPAWN_LINE_COUNT);
        hardDropAndFinishClearing(game);

        Position itemPosition = findItemPosition(game.createSnapshot(), ItemType.HEAVY_WEIGHT);
        for (int row = BOTTOM_ROW - 2; row <= BOTTOM_ROW; row++) {
            fixture.board().lock(new Piece(PieceType.SLOW, 0, row, itemPosition.column()));
        }
        long normalInterval = game.createSnapshot().getDropInterval();
        hardDropAndFinishClearing(game);
        assertEquals(DRILL_DROP_INTERVAL, game.createSnapshot().getDropInterval());
        long scoreDuringDrill = game.createSnapshot().getScore();
        int[][] beforeInput = game.createSnapshot().getCells();
        game.handleCommand(GameCommand.MOVE_LEFT);
        assertCellsEqual(beforeInput, game.createSnapshot().getCells());

        int previousMarkerCount = countCellId(game.createSnapshot(), PieceType.SLOW.getCellId());
        for (int tick = 0; tick < BOARD_HEIGHT; tick++) {
            game.tick();
            if (game.createSnapshot().getDropInterval() != DRILL_DROP_INTERVAL) {
                break;
            }
            assertEquals(scoreDuringDrill, game.createSnapshot().getScore());
            int markerCount = countCellId(game.createSnapshot(), PieceType.SLOW.getCellId());
            assertTrue(markerCount < previousMarkerCount);
            previousMarkerCount = markerCount;
        }

        assertEquals(normalInterval, game.createSnapshot().getDropInterval());
        assertTrue(game.getLockedPieceCount() > ITEM_SPAWN_LINE_COUNT);
    }

    @Test
    void doubleSpecialITetrisScoreIncreaseOnlyForThatPiece() {
        GameFixture specialFixture = prepareTetrisAfterTenLines(GameMode.ITEM, ItemType.SPECIAL_I);
        GameFixture normalFixture = prepareTetrisAfterTenLines(GameMode.NORMAL, null);
        Game specialGame = specialFixture.game();
        Game normalGame = normalFixture.game();

        long specialBefore = specialGame.createSnapshot().getScore();
        long normalBefore = normalGame.createSnapshot().getScore();
        assertTrue(hasItemCode(specialGame.createSnapshot(), ItemType.SPECIAL_I));
        specialGame.handleCommand(GameCommand.ROTATE_CLOCKWISE);
        normalGame.handleCommand(GameCommand.ROTATE_CLOCKWISE);
        hardDropAndFinishClearing(specialGame);
        hardDropAndFinishClearing(normalGame);

        long specialIncrease = specialGame.createSnapshot().getScore() - specialBefore;
        long normalIncrease = normalGame.createSnapshot().getScore() - normalBefore;
        assertEquals(normalIncrease * 2, specialIncrease);

        GameSnapshot nextSpecial = specialGame.createSnapshot();
        assertFalse(hasItemCode(nextSpecial, ItemType.SPECIAL_I));
    }

    @Test
    void doNotDoubleSpecialIScoreForFewerThanFourLines() {
        GameFixture specialFixture = prepareItemAfterTenLines(ItemType.SPECIAL_I);
        GameFixture normalFixture = prepareItemAfterTenLines(null);
        Game specialGame = specialFixture.game();
        Game normalGame = normalFixture.game();
        prepareFullBottomRowExcept(specialFixture.board(), 1);
        prepareFullBottomRowExcept(normalFixture.board(), 1);

        long specialBefore = specialGame.createSnapshot().getScore();
        long normalBefore = normalGame.createSnapshot().getScore();
        hardDropAndFinishClearing(specialGame);
        hardDropAndFinishClearing(normalGame);

        long specialIncrease = specialGame.createSnapshot().getScore() - specialBefore;
        long normalIncrease = normalGame.createSnapshot().getScore() - normalBefore;
        assertEquals(normalIncrease, specialIncrease);
    }

    @Test
    void slowItemDoublesIntervalAndExpiresWhilePausedTimeIsPreserved() {
        GameFixture fixture = itemModeGame(ItemType.SLOW, PieceType.I);
        Game game = fixture.game();
        game.start();
        hardDropAndClearLines(game, ITEM_SPAWN_LINE_COUNT);
        hardDropAndFinishClearing(game);
        prepareFullBottomRowExcept(fixture.board(), 1);
        long normalInterval = game.createSnapshot().getDropInterval();
        hardDropAndFinishClearing(game);

        long slowedInterval = game.createSnapshot().getDropInterval();
        assertEquals(normalInterval * 2, slowedInterval);
        int expirationTicks = (SLOW_DURATION_MILLISECONDS + (int) slowedInterval - 1) / (int) slowedInterval;
        game.handleCommand(GameCommand.PAUSE);
        repeatTick(game, expirationTicks);
        assertEquals(slowedInterval, game.createSnapshot().getDropInterval());
        game.handleCommand(GameCommand.PAUSE);
        repeatTick(game, expirationTicks - 1);
        assertEquals(slowedInterval, game.createSnapshot().getDropInterval());
        tickAndFinishClearing(game);
        assertEquals(normalInterval, game.createSnapshot().getDropInterval());
    }

    @Test
    void reserveSpecialBlockAfterBlockGenerateItemIsDestroyed() {
        GameFixture fixture = itemModeGame(ItemType.BLOCK_GENERATE, PieceType.I);
        Game game = fixture.game();
        game.start();
        hardDropAndClearLines(game, ITEM_SPAWN_LINE_COUNT - 1);

        hardDropAndFinishClearing(game);
        hardDropAndFinishClearing(game);
        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);
        GameSnapshot rotated = game.createSnapshot();
        int itemColumn = findItemPosition(rotated, ItemType.BLOCK_GENERATE).column();
        prepareVerticalTetris(fixture.board(), itemColumn);
        hardDropAndFinishClearing(game);

        assertEquals(List.of(PieceType.SPECIAL_BLOCK), game.createSnapshot().getNextPieces());
        hardDropAndFinishClearing(game);
        assertTrue(hasPieceCellId(game.createSnapshot(), PieceType.SPECIAL_BLOCK));
    }

    private GameFixture prepareTetrisAfterTenLines(GameMode mode, ItemType itemType) {
        GameFixture fixture = mode == GameMode.ITEM
                ? itemModeGame(itemType, PieceType.I)
                : normalModeGame(PieceType.I);
        Game game = fixture.game();
        game.start();
        hardDropAndClearLines(game, ITEM_SPAWN_LINE_COUNT);
        hardDropAndFinishClearing(game);
        prepareVerticalTetris(fixture.board(), VERTICAL_COLUMN);
        return fixture;
    }

    private GameFixture prepareItemAfterTenLines(ItemType itemType) {
        GameFixture fixture = itemType == null
                ? normalModeGame(PieceType.I)
                : itemModeGame(itemType, PieceType.I);
        Game game = fixture.game();
        game.start();
        hardDropAndClearLines(game, ITEM_SPAWN_LINE_COUNT);
        hardDropAndFinishClearing(game);
        return fixture;
    }

    private GameFixture itemModeGame(ItemType itemType, PieceType... sequence) {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        return new GameFixture(board, new Game(
                board,
                sequenceGenerator(sequence),
                itemGenerator(itemType),
                Difficulty.NORMAL,
                GameMode.ITEM
        ));
    }

    private GameFixture normalModeGame(PieceType... sequence) {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        return new GameFixture(board, new Game(
                board,
                sequenceGenerator(sequence),
                itemGenerator(ItemType.SLOW),
                Difficulty.NORMAL,
                GameMode.NORMAL
        ));
    }

    private ItemGenerator itemGenerator(ItemType selectedType) {
        Map<ItemType, Integer> weights = new EnumMap<>(ItemType.class);
        for (ItemType type : ItemType.values()) {
            weights.put(type, type == selectedType ? 1 : 0);
        }
        return new ItemGenerator(new Random(RANDOM_SEED), weights);
    }

    private TetrominoGenerator sequenceGenerator(PieceType... types) {
        return new TetrominoGenerator(new Random(RANDOM_SEED)) {
            private int index;

            @Override
            public PieceType generateNext() {
                return types[Math.min(index++, types.length - 1)];
            }
        };
    }

    private void hardDropAndClearLines(Game game, int count) {
        for (int index = 0; index < count; index++) {
            hardDropAndFinishClearing(game);
        }
    }

    private void prepareVerticalTetris(Board board, int filledColumn) {
        for (int row = BOTTOM_ROW - TETRIS_LINE_COUNT + 1; row <= BOTTOM_ROW; row++) {
            for (int column = 0; column < BOARD_WIDTH; column++) {
                if (column != filledColumn) {
                    board.lock(new Piece(PieceType.SLOW, 0, row, column));
                }
            }
        }
    }

    private void prepareFullBottomRowExcept(Board board, int emptyColumn) {
        for (int column = 0; column < BOARD_WIDTH; column++) {
            if (column != emptyColumn) {
                board.lock(new Piece(PieceType.SLOW, 0, BOTTOM_ROW, column));
            }
        }
    }

    private boolean hasItemCode(GameSnapshot snapshot, ItemType type) {
        int itemCode = type.getItemCode();
        for (int[] row : snapshot.getCells()) {
            for (int cell : row) {
                if (Board.extractItemCode(cell) == itemCode) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAnyItemCode(GameSnapshot snapshot) {
        for (ItemType type : ItemType.values()) {
            if (hasItemCode(snapshot, type)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPieceCellId(GameSnapshot snapshot, PieceType type) {
        int cellId = type.getCellId();
        for (int[] row : snapshot.getCells()) {
            for (int cell : row) {
                if (Board.extractCellId(cell) == cellId) {
                    return true;
                }
            }
        }
        return false;
    }

    private Position findItemPosition(GameSnapshot snapshot, ItemType type) {
        int itemCode = type.getItemCode();
        int[][] cells = snapshot.getCells();
        for (int row = 0; row < cells.length; row++) {
            for (int column = 0; column < cells[row].length; column++) {
                if (Board.extractItemCode(cells[row][column]) == itemCode) {
                    return new Position(row, column);
                }
            }
        }
        throw new AssertionError("Item is not present: " + type);
    }

    private int findEmptyColumn(GameSnapshot snapshot, int row) {
        int[] cells = snapshot.getCells()[row];
        for (int column = 0; column < cells.length; column++) {
            if (cells[column] == Board.EMPTY_CELL) {
                return column;
            }
        }
        throw new AssertionError("No empty column in row " + row);
    }

    private int countCellId(GameSnapshot snapshot, int cellId) {
        int count = 0;
        for (int[] row : snapshot.getCells()) {
            for (int cell : row) {
                if (Board.extractCellId(cell) == cellId && cell != Board.EMPTY_CELL) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countNonEmptyCells(GameSnapshot snapshot, int row) {
        int count = 0;
        for (int cell : snapshot.getCells()[row]) {
            if (cell != Board.EMPTY_CELL) {
                count++;
            }
        }
        return count;
    }

    private void assertCellsEqual(int[][] expected, int[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int row = 0; row < expected.length; row++) {
            assertArrayEquals(expected[row], actual[row], "row " + row);
        }
    }

    private void hardDropAndFinishClearing(Game game) {
        game.handleCommand(GameCommand.HARD_DROP);
        if (game.createSnapshot().getStatus() == GameStatus.CLEARING) {
            game.finishLineClear();
        }
    }

    private void tickAndFinishClearing(Game game) {
        game.tick();
        if (game.createSnapshot().getStatus() == GameStatus.CLEARING) {
            game.finishLineClear();
        }
    }

    private void repeatTick(Game game, int count) {
        for (int index = 0; index < count; index++) {
            tickAndFinishClearing(game);
        }
    }

    private record GameFixture(Board board, Game game) {
    }
    // AI-assisted code end
}
