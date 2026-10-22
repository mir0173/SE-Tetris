package tetris.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;

class BoardTest {

    // AI-assisted code start
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final int SMALL_BOARD_WIDTH = 8;
    private static final int SMALL_BOARD_HEIGHT = 6;
    private static final int FULL_LINE_ROW = 5;
    private static final int SECOND_FULL_LINE_ROW = 4;
    private static final int THIRD_FULL_LINE_ROW = 3;
    private static final int FOURTH_FULL_LINE_ROW = 2;
    private static final int PATTERN_ROW = 4;
    private static final int PATTERN_A_ROW = 4;
    private static final int PATTERN_B_ROW = 2;
    private static final int CELL_ID_T = 3;
    private static final int CELL_ID_I = 1;
    private static final int CELL_ID_O = 2;
    private static final int ITEM_CELL_INDEX = 2;
    private static final int ITEM_ROW = 2;
    private static final int ITEM_COLUMN = 1;
    private static final int CLEAR_TARGET_ROW = 2;
    private static final int SINGLE_CELL_BOARD_SIZE = 4;
    private static final int INVALID_ROW = -1;
    private static final int INVALID_COLUMN = BOARD_WIDTH;
    private static final int[] ROW_CELL_VALUES = {
        ItemType.SLOW.getItemCode() * Board.ITEM_CODE_MULTIPLIER + PieceType.SLOW.getCellId(),
        ItemType.HEAVY_WEIGHT.getItemCode() * Board.ITEM_CODE_MULTIPLIER + PieceType.SLOW.getCellId(),
        ItemType.SPECIAL_I.getItemCode() * Board.ITEM_CODE_MULTIPLIER + PieceType.SLOW.getCellId(),
        ItemType.LINE_CLEAR.getItemCode() * Board.ITEM_CODE_MULTIPLIER + PieceType.SLOW.getCellId()
    };

    @Test
    void throwWhenWidthIsZero() {
        assertThrows(IllegalArgumentException.class, () -> new Board(0, BOARD_HEIGHT));
    }

    @Test
    void throwWhenHeightIsZero() {
        assertThrows(IllegalArgumentException.class, () -> new Board(BOARD_WIDTH, 0));
    }

    @Test
    void throwWhenWidthIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new Board(-1, BOARD_HEIGHT));
    }

    @Test
    void createEmptyBoardWithExpectedSize() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        assertEquals(BOARD_WIDTH, board.getWidth());
        assertEquals(BOARD_HEIGHT, board.getHeight());
        for (int row = 0; row < BOARD_HEIGHT; row++) {
            for (int column = 0; column < BOARD_WIDTH; column++) {
                assertTrue(board.isEmpty(row, column), "row " + row + ", column " + column);
            }
        }
    }

    @Test
    void throwWhenReadingCellAboveBoard() {
        assertThrows(
            ArrayIndexOutOfBoundsException.class,
            () -> new Board(BOARD_WIDTH, BOARD_HEIGHT).getCell(-1, 0)
        );
    }

    @Test
    void throwWhenReadingCellBelowBoard() {
        assertThrows(
            ArrayIndexOutOfBoundsException.class,
            () -> new Board(BOARD_WIDTH, BOARD_HEIGHT).getCell(BOARD_HEIGHT, 0)
        );
    }

    @Test
    void throwWhenReadingCellRightOfBoard() {
        assertThrows(
            ArrayIndexOutOfBoundsException.class,
            () -> new Board(BOARD_WIDTH, BOARD_HEIGHT).getCell(0, BOARD_WIDTH)
        );
    }

    @Test
    void returnTrueForInsideCoordinates() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        assertTrue(board.isInside(0, 0));
        assertTrue(board.isInside(0, BOARD_WIDTH - 1));
        assertTrue(board.isInside(BOARD_HEIGHT - 1, 0));
        assertTrue(board.isInside(BOARD_HEIGHT - 1, BOARD_WIDTH - 1));
    }

    @Test
    void returnFalseForOutsideCoordinates() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        assertFalse(board.isInside(-1, 0));
        assertFalse(board.isInside(BOARD_HEIGHT, 0));
        assertFalse(board.isInside(0, -1));
        assertFalse(board.isInside(0, BOARD_WIDTH));
    }

    @Test
    void allowSpawnBlockOnEmptyBoard() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        assertTrue(board.canPlace(Piece.spawn(PieceType.T, BOARD_WIDTH)));
    }

    @Test
    void rejectBlockCrossingLeftWall() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        assertFalse(board.canPlace(new Piece(PieceType.T, 0, 0, -1)));
    }

    @Test
    void allowBlockAgainstLeftWall() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        assertTrue(board.canPlace(new Piece(PieceType.T, 0, 0, 0)));
    }

    @Test
    void rejectBlockCrossingRightWall() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        assertFalse(board.canPlace(new Piece(PieceType.T, 0, 0, 8)));
    }

    @Test
    void allowBlockAgainstRightWall() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        assertTrue(board.canPlace(new Piece(PieceType.T, 0, 0, 7)));
    }

    @Test
    void rejectBlockCrossingBottomWall() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        assertFalse(board.canPlace(new Piece(PieceType.T, 0, BOARD_HEIGHT - 1, 3)));
    }

    @Test
    void allowBlockAgainstBottomWall() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        assertTrue(board.canPlace(new Piece(PieceType.T, 0, 18, 3)));
    }

    @Test
    void rejectBlockCrossingTopWall() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        assertFalse(board.canPlace(new Piece(PieceType.T, 0, -1, 3)));
    }

    @Test
    void rejectBlockOverlappingLockedBlock() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        Piece block = Piece.spawn(PieceType.T, BOARD_WIDTH);
        board.lock(block);

        assertFalse(board.canPlace(block));
    }

    @Test
    void rejectBlockWithOneOverlappingCell() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        board.lock(Piece.spawn(PieceType.T, BOARD_WIDTH));

        assertFalse(board.canPlace(new Piece(PieceType.T, 0, 1, 3)));
    }

    @Test
    void allowBlockInUnoccupiedLocation() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        board.lock(Piece.spawn(PieceType.T, BOARD_WIDTH));

        assertTrue(board.canPlace(new Piece(PieceType.T, 0, 5, 3)));
    }

    @Test
    void lockTBlockIntoExpectedCells() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        board.lock(Piece.spawn(PieceType.T, BOARD_WIDTH));

        assertEquals(CELL_ID_T, board.getCell(0, 3));
        assertEquals(CELL_ID_T, board.getCell(0, 4));
        assertEquals(CELL_ID_T, board.getCell(0, 5));
        assertEquals(CELL_ID_T, board.getCell(1, 4));
        assertFalse(board.isEmpty(0, 3));
        assertFalse(board.isEmpty(0, 4));
        assertFalse(board.isEmpty(0, 5));
        assertFalse(board.isEmpty(1, 4));
    }

    @Test
    void leaveOtherCellsEmptyAfterLockingBlock() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        board.lock(Piece.spawn(PieceType.T, BOARD_WIDTH));

        assertEquals(Board.EMPTY_CELL, board.getCell(0, 0));
        assertEquals(Board.EMPTY_CELL, board.getCell(1, 0));
        assertEquals(Board.EMPTY_CELL, board.getCell(BOARD_HEIGHT - 1, BOARD_WIDTH - 1));
    }

    @Test
    void throwWhenLockingBlockOutsideBoard() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        assertThrows(
                IllegalArgumentException.class,
                () -> board.lock(new Piece(PieceType.T, 0, 0, -1))
        );
    }

    @Test
    void preserveExistingCellsWhenLockingOverlappingBlock() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        Piece block = Piece.spawn(PieceType.T, BOARD_WIDTH);
        board.lock(block);

        assertThrows(IllegalArgumentException.class, () -> board.lock(block));
        assertEquals(CELL_ID_T, board.getCell(0, 3));
        assertEquals(CELL_ID_T, board.getCell(0, 4));
        assertEquals(CELL_ID_T, board.getCell(0, 5));
        assertEquals(CELL_ID_T, board.getCell(1, 4));
    }

    @Test
    void lockEmbeddedItemIntoItsIndexedCellOnly() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        Item item = new Item(ItemType.LINE_CLEAR, ITEM_CELL_INDEX);
        Piece piece = new Piece(PieceType.I, item, 0, ITEM_ROW, ITEM_COLUMN);

        board.lock(piece);

        for (int index = 0; index < piece.getAbsolutePositions().size(); index++) {
            Position position = piece.getAbsolutePositions().get(index);
            int expectedCell = index == ITEM_CELL_INDEX
                    ? ItemType.LINE_CLEAR.getItemCode() * Board.ITEM_CODE_MULTIPLIER + PieceType.I.getCellId()
                    : PieceType.I.getCellId();
            assertEquals(expectedCell, board.getCell(position.row(), position.column()));
        }
    }

    @Test
    void lockStandaloneItemIntoEveryCell() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        Item item = new Item(ItemType.SLOW, Item.NOT_EMBEDDED);
        Piece piece = new Piece(PieceType.T, item, 0, ITEM_ROW, ITEM_COLUMN);
        int expectedCell = ItemType.SLOW.getItemCode() * Board.ITEM_CODE_MULTIPLIER + PieceType.T.getCellId();

        board.lock(piece);

        for (Position position : piece.getAbsolutePositions()) {
            assertEquals(expectedCell, board.getCell(position.row(), position.column()));
        }
    }

    @Test
    void roundTripCellIdAndItemCodeExtraction() {
        int cellValue = ItemType.SPECIAL_I.getItemCode() * Board.ITEM_CODE_MULTIPLIER + PieceType.O.getCellId();

        assertEquals(PieceType.O.getCellId(), Board.extractCellId(cellValue));
        assertEquals(ItemType.SPECIAL_I.getItemCode(), Board.extractItemCode(cellValue));
        assertEquals(Board.EMPTY_CELL, Board.extractItemCode(PieceType.O.getCellId()));
    }

    @Test
    void clearMiddleLineAndDropRows() {
        Board board = boardWithOneCellInEachRow();

        board.clearLine(CLEAR_TARGET_ROW);

        assertEquals(Board.EMPTY_CELL, board.getCell(0, 0));
        assertEquals(ROW_CELL_VALUES[0], board.getCell(1, 0));
        assertEquals(ROW_CELL_VALUES[1], board.getCell(CLEAR_TARGET_ROW, 0));
        assertEquals(ROW_CELL_VALUES[3], board.getCell(CLEAR_TARGET_ROW + 1, 0));
    }

    @Test
    void clearBottomLineAndDropRows() {
        Board board = boardWithOneCellInEachRow();

        board.clearLine(SINGLE_CELL_BOARD_SIZE - 1);

        assertEquals(ROW_CELL_VALUES[2], board.getCell(SINGLE_CELL_BOARD_SIZE - 1, 0));
        assertEquals(ROW_CELL_VALUES[1], board.getCell(SINGLE_CELL_BOARD_SIZE - 2, 0));
        assertEquals(ROW_CELL_VALUES[0], board.getCell(1, 0));
        assertEquals(Board.EMPTY_CELL, board.getCell(0, 0));
        assertEquals(Board.EMPTY_CELL, board.getCell(0, 0));
    }

    @Test
    void clearTopLineAndDropRows() {
        Board board = boardWithOneCellInEachRow();

        board.clearLine(0);

        assertEquals(Board.EMPTY_CELL, board.getCell(0, 0));
        assertEquals(ROW_CELL_VALUES[1], board.getCell(1, 0));
        assertEquals(ROW_CELL_VALUES[2], board.getCell(2, 0));
        assertEquals(ROW_CELL_VALUES[3], board.getCell(3, 0));
    }

    @Test
    void rejectClearingLineOutsideBoard() {
        Board board = new Board(SINGLE_CELL_BOARD_SIZE, SINGLE_CELL_BOARD_SIZE);

        assertThrows(IllegalArgumentException.class, () -> board.clearLine(INVALID_ROW));
        assertThrows(IllegalArgumentException.class, () -> board.clearLine(SINGLE_CELL_BOARD_SIZE));
    }

    @Test
    void clearOnlyTargetCellAndReturnPreviousValue() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        Piece piece = new Piece(PieceType.T, 0, ITEM_ROW, ITEM_COLUMN);
        Position target = piece.getAbsolutePositions().get(0);
        int previousValue = PieceType.T.getCellId();
        board.lock(piece);

        int clearedValue = board.clearCell(target.row(), target.column());

        assertEquals(previousValue, clearedValue);
        assertEquals(Board.EMPTY_CELL, board.getCell(target.row(), target.column()));
        assertEquals(PieceType.T.getCellId(), board.getCell(ITEM_ROW, ITEM_COLUMN + 1));
        assertEquals(Board.EMPTY_CELL, board.getCell(ITEM_ROW - 1, ITEM_COLUMN));
    }

    @Test
    void rejectClearingCellOutsideBoard() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        assertThrows(IllegalArgumentException.class, () -> board.clearCell(INVALID_ROW, 0));
        assertThrows(IllegalArgumentException.class, () -> board.clearCell(0, INVALID_COLUMN));
    }

    @Test
    void collectOnlyItemsFromFullLineThatWasDestroyed() {
        Board board = new Board(SINGLE_CELL_BOARD_SIZE, SINGLE_CELL_BOARD_SIZE);
        lockSingleCellRow(board, SINGLE_CELL_BOARD_SIZE - 1, 0, ItemType.LINE_CLEAR);
        lockSingleCellRow(board, SINGLE_CELL_BOARD_SIZE - 1, 1, null);
        lockSingleCellRow(board, SINGLE_CELL_BOARD_SIZE - 1, 2, null);
        lockSingleCellRow(board, SINGLE_CELL_BOARD_SIZE - 1, 3, null);
        lockSingleCellRow(board, SINGLE_CELL_BOARD_SIZE - 2, 0, ItemType.BLOCK_GENERATE);

        board.clearFullLines();

        assertEquals(List.of(ItemType.LINE_CLEAR.getItemCode()), board.takeDestroyedItemCodes());
    }

    @Test
    void collectOnlyItemsFromLineClearedByClearLine() {
        Board board = new Board(SINGLE_CELL_BOARD_SIZE, SINGLE_CELL_BOARD_SIZE);
        lockSingleCellRow(board, CLEAR_TARGET_ROW, 0, ItemType.LINE_CLEAR);
        lockSingleCellRow(board, CLEAR_TARGET_ROW, 1, null);
        lockSingleCellRow(board, CLEAR_TARGET_ROW, 2, null);
        lockSingleCellRow(board, CLEAR_TARGET_ROW, 3, null);
        lockSingleCellRow(board, CLEAR_TARGET_ROW - 1, 0, ItemType.BLOCK_GENERATE);

        board.clearLine(CLEAR_TARGET_ROW);

        assertEquals(List.of(ItemType.LINE_CLEAR.getItemCode()), board.takeDestroyedItemCodes());
    }

    @Test
    void collectDestroyedItemFromClearedCellButNotEmptyCell() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        Piece piece = new Piece(
                PieceType.T,
                new Item(ItemType.LINE_CLEAR, ITEM_CELL_INDEX),
                0,
                ITEM_ROW,
                ITEM_COLUMN
        );
        Position itemPosition = piece.getAbsolutePositions().get(ITEM_CELL_INDEX);
        board.lock(piece);
        board.clearCell(piece.getAbsolutePositions().get(0).row(), piece.getAbsolutePositions().get(0).column());
        board.takeDestroyedItemCodes();

        board.clearCell(itemPosition.row(), itemPosition.column());

        assertEquals(List.of(ItemType.LINE_CLEAR.getItemCode()), board.takeDestroyedItemCodes());
        assertEquals(List.of(), board.takeDestroyedItemCodes());
    }

    @Test
    void returnEmptyDestroyedItemListAfterTakingIt() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        Piece piece = new Piece(PieceType.T, new Item(ItemType.LINE_CLEAR, 0), 0, ITEM_ROW, ITEM_COLUMN);
        Position itemPosition = piece.getAbsolutePositions().get(0);
        board.lock(piece);
        board.clearCell(itemPosition.row(), itemPosition.column());

        assertEquals(List.of(ItemType.LINE_CLEAR.getItemCode()), board.takeDestroyedItemCodes());
        assertEquals(List.of(), board.takeDestroyedItemCodes());
    }

    @Test
    void returnEmptyDestroyedItemListWhenOnlyLockingBlocks() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        board.lock(Piece.spawn(PieceType.T, BOARD_WIDTH));

        assertEquals(List.of(), board.takeDestroyedItemCodes());
    }

    @Test
    void returnZeroAndPreserveBoardWhenNoLineIsFull() {
        Board board = new Board(SMALL_BOARD_WIDTH, SMALL_BOARD_HEIGHT);
        lockIPiece(board, FULL_LINE_ROW, 0);
        int[][] before = board.copyCells();

        assertEquals(0, board.clearFullLines());
        assertCellsEqual(before, board.copyCells());
    }

    @Test
    void clearOneFullLineAndDropRows() {
        Board board = new Board(SMALL_BOARD_WIDTH, SMALL_BOARD_HEIGHT);
        lockIPiece(board, FULL_LINE_ROW, 0);
        lockIPiece(board, FULL_LINE_ROW, 4);
        lockIPiece(board, PATTERN_ROW, 0);

        assertEquals(1, board.clearFullLines());
        assertPatternA(board, FULL_LINE_ROW);
        assertRowsEmpty(board, 0, FULL_LINE_ROW - 1);
    }

    @Test
    void clearTwoAdjacentFullLinesAndDropRows() {
        Board board = new Board(SMALL_BOARD_WIDTH, SMALL_BOARD_HEIGHT);
        lockIPiece(board, FULL_LINE_ROW, 0);
        lockIPiece(board, FULL_LINE_ROW, 4);
        lockIPiece(board, SECOND_FULL_LINE_ROW, 0);
        lockIPiece(board, SECOND_FULL_LINE_ROW, 4);
        lockIPiece(board, THIRD_FULL_LINE_ROW, 0);

        assertEquals(2, board.clearFullLines());
        assertPatternA(board, FULL_LINE_ROW);
        assertRowsEmpty(board, 0, FULL_LINE_ROW - 1);
    }

    @Test
    void clearTwoSeparatedFullLinesAndPreserveIntermediatePatterns() {
        Board board = new Board(SMALL_BOARD_WIDTH, SMALL_BOARD_HEIGHT);
        lockIPiece(board, FULL_LINE_ROW, 0);
        lockIPiece(board, FULL_LINE_ROW, 4);
        lockIPiece(board, PATTERN_A_ROW, 0);
        lockIPiece(board, THIRD_FULL_LINE_ROW, 0);
        lockIPiece(board, THIRD_FULL_LINE_ROW, 4);
        lockIPiece(board, PATTERN_B_ROW, 4);

        assertEquals(2, board.clearFullLines());
        assertPatternA(board, FULL_LINE_ROW);
        assertPatternB(board, SECOND_FULL_LINE_ROW);
        assertRowsEmpty(board, 0, THIRD_FULL_LINE_ROW);
    }

    @Test
    void clearFourFullLinesAndDropTopPattern() {
        Board board = new Board(SMALL_BOARD_WIDTH, SMALL_BOARD_HEIGHT);
        for (int row = FOURTH_FULL_LINE_ROW; row <= FULL_LINE_ROW; row++) {
            lockIPiece(board, row, 0);
            lockIPiece(board, row, 4);
        }
        lockIPiece(board, 1, 0);

        assertEquals(4, board.clearFullLines());
        assertPatternA(board, FULL_LINE_ROW);
        assertRowsEmpty(board, 0, FULL_LINE_ROW - 1);
    }

    @Test
    void returnZeroForEmptyBoard() {
        assertEquals(0, new Board(SMALL_BOARD_WIDTH, SMALL_BOARD_HEIGHT).clearFullLines());
    }

    @Test
    void reuseBoardAfterClearingLineWithoutSharingRows() {
        Board board = new Board(SMALL_BOARD_WIDTH, SMALL_BOARD_HEIGHT);
        lockIPiece(board, FULL_LINE_ROW, 0);
        lockIPiece(board, FULL_LINE_ROW, 4);
        board.clearFullLines();

        lockIPiece(board, 0, 0);

        assertPatternA(board, 0);
        assertRowsEmpty(board, 1, SMALL_BOARD_HEIGHT - 1);
    }

    @Test
    void preserveBoardWhenCopyIsModified() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        board.lock(Piece.spawn(PieceType.T, BOARD_WIDTH));
        int[][] copy = board.copyCells();
        copy[0][3] = Board.EMPTY_CELL;

        assertEquals(CELL_ID_T, board.getCell(0, 3));
    }

    @Test
    void drawSpawnBlockOnEmptyCopy() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        int[][] copy = board.copyCellsWith(Piece.spawn(PieceType.T, BOARD_WIDTH));

        assertEquals(CELL_ID_T, copy[0][3]);
        assertEquals(CELL_ID_T, copy[0][4]);
        assertEquals(CELL_ID_T, copy[0][5]);
        assertEquals(CELL_ID_T, copy[1][4]);
        assertOnlyTCells(copy, 0, 3, 1, 4);
    }

    @Test
    void preserveBoardAfterCopyingWithFallingBlock() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        board.copyCellsWith(Piece.spawn(PieceType.T, BOARD_WIDTH));

        assertBoardEmpty(board);
    }

    @Test
    void includeLockedAndFallingBlocksInCopy() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        board.lock(Piece.spawn(PieceType.T, BOARD_WIDTH));

        int[][] copy = board.copyCellsWith(new Piece(PieceType.O, 0, 3, 7));

        assertEquals(CELL_ID_T, copy[0][3]);
        assertEquals(CELL_ID_O, copy[3][7]);
        assertEquals(CELL_ID_O, copy[3][8]);
        assertEquals(CELL_ID_O, copy[4][7]);
        assertEquals(CELL_ID_O, copy[4][8]);
    }

    @Test
    void throwWhenCopyingWithBlockOutsideBoard() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        assertThrows(
                IllegalArgumentException.class,
                () -> board.copyCellsWith(new Piece(PieceType.T, 0, 0, -1))
        );
    }

    @Test
    void throwWhenCopyingWithOverlappingBlock() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        Piece block = Piece.spawn(PieceType.T, BOARD_WIDTH);
        board.lock(block);

        assertThrows(IllegalArgumentException.class, () -> board.copyCellsWith(block));
    }

    @Test
    void throwWhenHeightIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new Board(BOARD_WIDTH, -1));
    }

    @Test
    void throwWhenReadingCellLeftOfBoard() {
        assertThrows(
                ArrayIndexOutOfBoundsException.class,
                () -> new Board(BOARD_WIDTH, BOARD_HEIGHT).getCell(0, -1)
        );
    }

    @Test
    void throwWhenCheckingEmptyCellOutsideBoard() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> board.isEmpty(-1, 0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> board.isEmpty(BOARD_HEIGHT, 0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> board.isEmpty(0, -1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> board.isEmpty(0, BOARD_WIDTH));
    }

    @Test
    void keepBoardIndependentFromCopyWithResult() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        int[][] copy = board.copyCellsWith(Piece.spawn(PieceType.T, BOARD_WIDTH));
        copy[10][0] = 9;

        assertEquals(Board.EMPTY_CELL, board.getCell(10, 0));
    }

    private void lockIPiece(Board board, int targetRow, int startColumn) {
        board.lock(new Piece(PieceType.I, 0, targetRow - 1, startColumn));
    }

    private Board boardWithOneCellInEachRow() {
        Board board = new Board(SINGLE_CELL_BOARD_SIZE, SINGLE_CELL_BOARD_SIZE);
        for (int row = 0; row < SINGLE_CELL_BOARD_SIZE; row++) {
            board.lock(new Piece(PieceType.SLOW, itemForRow(row), 0, row, 0));
        }
        return board;
    }

    private Item itemForRow(int row) {
        return switch (row) {
            case 0 -> new Item(ItemType.SLOW, Item.NOT_EMBEDDED);
            case 1 -> new Item(ItemType.HEAVY_WEIGHT, Item.NOT_EMBEDDED);
            case 2 -> new Item(ItemType.SPECIAL_I, Item.NOT_EMBEDDED);
            case 3 -> new Item(ItemType.LINE_CLEAR, 0);
            default -> throw new IllegalArgumentException("Unexpected row: " + row);
        };
    }

    private void lockSingleCellRow(Board board, int row, int column, ItemType itemType) {
        Item item = itemType == null ? null : new Item(itemType, 0);
        board.lock(new Piece(PieceType.SLOW, item, 0, row, column));
    }

    private void assertPatternA(Board board, int row) {
        for (int column = 0; column < SMALL_BOARD_WIDTH / 2; column++) {
            assertEquals(CELL_ID_I, board.getCell(row, column), "row " + row + ", column " + column);
        }
        for (int column = SMALL_BOARD_WIDTH / 2; column < SMALL_BOARD_WIDTH; column++) {
            assertEquals(Board.EMPTY_CELL, board.getCell(row, column), "row " + row + ", column " + column);
        }
    }

    private void assertPatternB(Board board, int row) {
        for (int column = 0; column < SMALL_BOARD_WIDTH / 2; column++) {
            assertEquals(Board.EMPTY_CELL, board.getCell(row, column), "row " + row + ", column " + column);
        }
        for (int column = SMALL_BOARD_WIDTH / 2; column < SMALL_BOARD_WIDTH; column++) {
            assertEquals(CELL_ID_I, board.getCell(row, column), "row " + row + ", column " + column);
        }
    }

    private void assertRowsEmpty(Board board, int firstRow, int lastRow) {
        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = 0; column < board.getWidth(); column++) {
                assertEquals(Board.EMPTY_CELL, board.getCell(row, column), "row " + row + ", column " + column);
            }
        }
    }

    private void assertCellsEqual(int[][] expected, int[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int row = 0; row < expected.length; row++) {
            assertArrayEquals(expected[row], actual[row], "row " + row);
        }
    }

    private void assertOnlyTCells(int[][] cells, int firstRow, int firstColumn, int secondRow, int secondColumn) {
        for (int row = 0; row < cells.length; row++) {
            for (int column = 0; column < cells[row].length; column++) {
                boolean isTCell = (row == firstRow && column >= firstColumn && column <= firstColumn + 2)
                        || (row == secondRow && column == secondColumn);
                if (isTCell) {
                    assertEquals(CELL_ID_T, cells[row][column], "row " + row + ", column " + column);
                } else {
                    assertEquals(Board.EMPTY_CELL, cells[row][column], "row " + row + ", column " + column);
                }
            }
        }
    }

    private void assertBoardEmpty(Board board) {
        for (int row = 0; row < board.getHeight(); row++) {
            for (int column = 0; column < board.getWidth(); column++) {
                assertTrue(board.isEmpty(row, column), "row " + row + ", column " + column);
            }
        }
    }
    // AI-assisted code end (테스트 코드)
}
