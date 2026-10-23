package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameSnapshotTest {

    // AI-assisted code start
    @Test
    void preserveCellsWhenExternalArraysChange() {
        int[][] cells = new int[20][10];
        cells[0][0] = PieceType.T.getCellId();

        GameSnapshot snapshot = new GameSnapshot(
                cells,
                List.of(PieceType.I),
                null,
                0L,
                GameStatus.READY,
                1_000L
        );

        cells[0][0] = 0;

        int[][] returnedCells = snapshot.getCells();
        returnedCells[0][0] = 0;

        assertEquals(
                PieceType.T.getCellId(),
                snapshot.getCells()[0][0]
        );
    }

    @Test
    void preserveNextPiecesWhenExternalListChanges() {
        List<PieceType> nextPieces = new ArrayList<>();
        nextPieces.add(PieceType.I);

        GameSnapshot snapshot = new GameSnapshot(
                new int[20][10],
                nextPieces,
                null,
                0L,
                GameStatus.READY,
                1_000L
        );

        nextPieces.clear();

        assertEquals(
                List.of(PieceType.I),
                snapshot.getNextPieces()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.getNextPieces().clear()
        );
    }

    @Test
    void provideEmptyClearingRowsByDefault() {
        GameSnapshot snapshot = new GameSnapshot(
                new int[20][10],
                List.of(PieceType.I),
                null,
                0L,
                GameStatus.PLAYING,
                1_000L
        );

        assertEquals(List.of(), snapshot.getClearingRows());
    }

    @Test
    void provideClearingRowsGivenAtCreation() {
        GameSnapshot snapshot = new GameSnapshot(
                new int[20][10],
                List.of(PieceType.I),
                null,
                0L,
                GameStatus.CLEARING,
                1_000L,
                null,
                null,
                List.of(18, 19)
        );

        assertEquals(List.of(18, 19), snapshot.getClearingRows());
        assertEquals(GameStatus.CLEARING, snapshot.getStatus());
    }

    @Test
    void preserveClearingRowsWhenExternalListChanges() {
        List<Integer> clearingRows = new ArrayList<>();
        clearingRows.add(19);

        GameSnapshot snapshot = new GameSnapshot(
                new int[20][10],
                List.of(PieceType.I),
                null,
                0L,
                GameStatus.CLEARING,
                1_000L,
                null,
                null,
                clearingRows
        );

        clearingRows.clear();

        assertEquals(List.of(19), snapshot.getClearingRows());
    }

    @Test
    void rejectModificationOfClearingRows() {
        GameSnapshot snapshot = new GameSnapshot(
                new int[20][10],
                List.of(PieceType.I),
                null,
                0L,
                GameStatus.CLEARING,
                1_000L,
                null,
                null,
                List.of(19)
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.getClearingRows().add(18)
        );
    }
    // AI-assisted code end (테스트 코드)
}