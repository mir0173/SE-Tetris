package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PieceTypeTest {

    // AI-assisted code start
    private static final int FIRST_ROTATION = 0;
    private static final int LAST_ROTATION = 3;
    private static final int CELL_COUNT = 4;

    @Test
    void returnFourCellsForEveryBasicTypeAndRotation() {
        for (PieceType type : basicTypes()) {
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
        for (PieceType type : PieceType.values()) {
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
            assertEquals(expected, Set.copyOf(PieceType.O.getCellsAt(rotation)), "O rotation " + rotation);
        }
    }

    @Test
    void returnTShapeAtRotationZero() {
        Set<Position> expected = Set.of(
                new Position(0, 0), new Position(0, 1),
                new Position(0, 2), new Position(1, 1)
        );

        assertEquals(expected, Set.copyOf(PieceType.T.getCellsAt(0)));
    }

    @Test
    void returnTShapeAtRotationOne() {
        Set<Position> expected = Set.of(
                new Position(0, 2), new Position(1, 2),
                new Position(2, 2), new Position(1, 1)
        );

        assertEquals(expected, Set.copyOf(PieceType.T.getCellsAt(1)));
    }

    @Test
    void returnIShapeAtRotationZero() {
        Set<Position> expected = Set.of(
                new Position(1, 0), new Position(1, 1),
                new Position(1, 2), new Position(1, 3)
        );

        assertEquals(expected, Set.copyOf(PieceType.I.getCellsAt(0)));
    }

    @Test
    void returnIShapeAtRotationOne() {
        Set<Position> expected = Set.of(
                new Position(0, 2), new Position(1, 2),
                new Position(2, 2), new Position(3, 2)
        );

        assertEquals(expected, Set.copyOf(PieceType.I.getCellsAt(1)));
    }

    @Test
    void returnSShapeAtRotationZero() {
        Set<Position> expected = Set.of(
                new Position(0, 1), new Position(0, 2),
                new Position(1, 0), new Position(1, 1)
        );

        assertEquals(expected, Set.copyOf(PieceType.S.getCellsAt(0)));
    }

    @Test
    void returnZShapeAtRotationZero() {
        Set<Position> expected = Set.of(
                new Position(0, 0), new Position(0, 1),
                new Position(1, 1), new Position(1, 2)
        );

        assertEquals(expected, Set.copyOf(PieceType.Z.getCellsAt(0)));
    }

    @Test
    void returnJShapeAtRotationZero() {
        Set<Position> expected = Set.of(
                new Position(0, 0), new Position(1, 0),
                new Position(1, 1), new Position(1, 2)
        );

        assertEquals(expected, Set.copyOf(PieceType.J.getCellsAt(0)));
    }

    @Test
    void returnLShapeAtRotationZero() {
        Set<Position> expected = Set.of(
                new Position(0, 2), new Position(1, 0),
                new Position(1, 1), new Position(1, 2)
        );

        assertEquals(expected, Set.copyOf(PieceType.L.getCellsAt(0)));
    }

    @Test
    void returnExpectedCellIdForEachType() {
        assertEquals(1, PieceType.I.getCellId());
        assertEquals(2, PieceType.O.getCellId());
        assertEquals(3, PieceType.T.getCellId());
        assertEquals(4, PieceType.S.getCellId());
        assertEquals(5, PieceType.Z.getCellId());
        assertEquals(6, PieceType.J.getCellId());
        assertEquals(7, PieceType.L.getCellId());
    }

    @Test
    void returnDistinctNonEmptyCellIdsForEveryType() {
        Set<Integer> cellIds = new java.util.HashSet<>();

        for (PieceType type : PieceType.values()) {
            assertTrue(cellIds.add(type.getCellId()), type + " cellId " + type.getCellId());
            assertTrue(type.getCellId() != Board.EMPTY_CELL, type + " uses EMPTY_CELL");
        }
    }

    @Test
    void returnExpectedBoxSizeForEachType() {
        assertEquals(4, PieceType.I.getBoxSize());
        assertEquals(2, PieceType.O.getBoxSize());

        for (PieceType type : new PieceType[] {
                PieceType.T, PieceType.S, PieceType.Z,
                PieceType.J, PieceType.L
        }) {
            assertEquals(3, type.getBoxSize(), type + " box size");
        }
    }

    @Test
    void throwWhenRotationIsBelowRange() {
        assertThrows(IllegalArgumentException.class, () -> PieceType.T.getCellsAt(-1));
    }

    @Test
    void throwWhenRotationIsAboveRange() {
        assertThrows(IllegalArgumentException.class, () -> PieceType.T.getCellsAt(4));
    }

    @Test
    void rejectModificationOfReturnedCells() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> PieceType.T.getCellsAt(0).clear()
        );
    }

    private PieceType[] basicTypes() {
        return Arrays.stream(PieceType.values())
                .filter(PieceType::isBasic)
                .toArray(PieceType[]::new);
    }
    // AI-assisted code end (테스트 코드)
}
