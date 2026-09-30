package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TetrominoTypeTest {

    // AI-assisted code start
    private static final int FIRST_ROTATION = 0;
    private static final int LAST_ROTATION = 3;
    private static final int CELL_COUNT = 4;

    @Test
    void returnFourCellsForEveryTypeAndRotation() {
        for (TetrominoType type : TetrominoType.values()) {
            for (int rotation = FIRST_ROTATION; rotation <= LAST_ROTATION; rotation++) {
                assertEquals(
                    CELL_COUNT,
                    type.getCellsAt(rotation).size(),
                    type + " rotation " + rotation
                );
            }
        }
    }

    @Test
    void returnCellsInsideBoxForEveryTypeAndRotation() {
        for (TetrominoType type : TetrominoType.values()) {
            for (int rotation = FIRST_ROTATION; rotation <= LAST_ROTATION; rotation++) {
                for (Position position : type.getCellsAt(rotation)) {
                        assertTrue(
                            position.row() >= 0 && position.row() < type.getBoxSize(),
                            type + " rotation " + rotation + " row " + position.row()
                        );
                        assertTrue(
                            position.column() >= 0 && position.column() < type.getBoxSize(),
                            type + " rotation " + rotation + " column " + position.column()
                        );
                }
            }
        }
    }

    @Test
    void preserveOShapeAcrossRotations() {
        Set<Position> expected = Set.of(
                new Position(0, 0), new Position(0, 1),
                new Position(1, 0), new Position(1, 1)
        );

        for (int rotation = FIRST_ROTATION; rotation <= LAST_ROTATION; rotation++) {
            assertEquals(expected, Set.copyOf(TetrominoType.O.getCellsAt(rotation)), "O rotation " + rotation);
        }
    }

    @Test
    void returnTShapeAtRotationZero() {
        Set<Position> expected = Set.of(
                new Position(0, 0), new Position(0, 1),
                new Position(0, 2), new Position(1, 1)
        );

        assertEquals(expected, Set.copyOf(TetrominoType.T.getCellsAt(0)));
    }

    @Test
    void returnTShapeAtRotationOne() {
        Set<Position> expected = Set.of(
                new Position(0, 2), new Position(1, 2),
                new Position(2, 2), new Position(1, 1)
        );

        assertEquals(expected, Set.copyOf(TetrominoType.T.getCellsAt(1)));
    }

    @Test
    void returnIShapeAtRotationZero() {
        Set<Position> expected = Set.of(
                new Position(1, 0), new Position(1, 1),
                new Position(1, 2), new Position(1, 3)
        );

        assertEquals(expected, Set.copyOf(TetrominoType.I.getCellsAt(0)));
    }

    @Test
    void returnIShapeAtRotationOne() {
        Set<Position> expected = Set.of(
                new Position(0, 2), new Position(1, 2),
                new Position(2, 2), new Position(3, 2)
        );

        assertEquals(expected, Set.copyOf(TetrominoType.I.getCellsAt(1)));
    }

    @Test
    void returnSShapeAtRotationZero() {
        Set<Position> expected = Set.of(
                new Position(0, 1), new Position(0, 2),
                new Position(1, 0), new Position(1, 1)
        );

        assertEquals(expected, Set.copyOf(TetrominoType.S.getCellsAt(0)));
    }

    @Test
    void returnZShapeAtRotationZero() {
        Set<Position> expected = Set.of(
                new Position(0, 0), new Position(0, 1),
                new Position(1, 1), new Position(1, 2)
        );

        assertEquals(expected, Set.copyOf(TetrominoType.Z.getCellsAt(0)));
    }

    @Test
    void returnJShapeAtRotationZero() {
        Set<Position> expected = Set.of(
                new Position(0, 0), new Position(1, 0),
                new Position(1, 1), new Position(1, 2)
        );

        assertEquals(expected, Set.copyOf(TetrominoType.J.getCellsAt(0)));
    }

    @Test
    void returnLShapeAtRotationZero() {
        Set<Position> expected = Set.of(
                new Position(0, 2), new Position(1, 0),
                new Position(1, 1), new Position(1, 2)
        );

        assertEquals(expected, Set.copyOf(TetrominoType.L.getCellsAt(0)));
    }

    @Test
    void returnExpectedCellIdForEachType() {
        assertEquals(1, TetrominoType.I.getCellId());
        assertEquals(2, TetrominoType.O.getCellId());
        assertEquals(3, TetrominoType.T.getCellId());
        assertEquals(4, TetrominoType.S.getCellId());
        assertEquals(5, TetrominoType.Z.getCellId());
        assertEquals(6, TetrominoType.J.getCellId());
        assertEquals(7, TetrominoType.L.getCellId());
    }

    @Test
    void returnDistinctNonEmptyCellIdsForEveryType() {
        Set<Integer> cellIds = new java.util.HashSet<>();

        for (TetrominoType type : TetrominoType.values()) {
            assertTrue(cellIds.add(type.getCellId()), type + " cellId " + type.getCellId());
            assertTrue(type.getCellId() != Board.EMPTY_CELL, type + " uses EMPTY_CELL");
        }
    }

    @Test
    void returnExpectedBoxSizeForEachType() {
        assertEquals(4, TetrominoType.I.getBoxSize());
        assertEquals(2, TetrominoType.O.getBoxSize());

        for (TetrominoType type : new TetrominoType[] {
                TetrominoType.T, TetrominoType.S, TetrominoType.Z,
                TetrominoType.J, TetrominoType.L
        }) {
            assertEquals(3, type.getBoxSize(), type + " box size");
        }
    }

    @Test
    void throwWhenRotationIsBelowRange() {
        assertThrows(IllegalArgumentException.class, () -> TetrominoType.T.getCellsAt(-1));
    }

    @Test
    void throwWhenRotationIsAboveRange() {
        assertThrows(IllegalArgumentException.class, () -> TetrominoType.T.getCellsAt(4));
    }

    @Test
    void rejectModificationOfReturnedCells() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> TetrominoType.T.getCellsAt(0).clear()
        );
    }
    // AI-assisted code end (테스트 코드)
}
