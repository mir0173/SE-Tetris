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
    // AI-assisted code end (테스트 코드)
}