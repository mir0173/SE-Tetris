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
        cells[0][0] = TetrominoType.T.getCellId();

        GameSnapshot snapshot = new GameSnapshot(
                cells,
                List.of(TetrominoType.I),
                0L,
                GameStatus.READY,
                1_000L
        );

        cells[0][0] = 0;

        int[][] returnedCells = snapshot.getCells();
        returnedCells[0][0] = 0;

        assertEquals(
                TetrominoType.T.getCellId(),
                snapshot.getCells()[0][0]
        );
    }

    @Test
    void preserveNextPiecesWhenExternalListChanges() {
        List<TetrominoType> nextPieces = new ArrayList<>();
        nextPieces.add(TetrominoType.I);

        GameSnapshot snapshot = new GameSnapshot(
                new int[20][10],
                nextPieces,
                0L,
                GameStatus.READY,
                1_000L
        );

        nextPieces.clear();

        assertEquals(
                List.of(TetrominoType.I),
                snapshot.getNextPieces()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.getNextPieces().clear()
        );
    }
    // AI-assisted code end (테스트 코드)
}