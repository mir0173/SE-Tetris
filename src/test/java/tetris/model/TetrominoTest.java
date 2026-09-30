package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TetrominoTest {

    // AI-assisted code start
    private static final int BOARD_WIDTH = 10;
    private static final int FIRST_ROTATION = 0;
    private static final int LAST_ROTATION = 3;
    private static final int ROTATION_COUNT = 4;
    private static final int SPAWN_ROW = 0;
    private static final int SPAWN_T_COLUMN = 3;
    private static final int SPAWN_O_COLUMN = 4;
    private static final int SPAWN_I_COLUMN = 3;

    @Test
    void spawnTAtTopCenter() {
        Tetromino block = Tetromino.spawn(TetrominoType.T, BOARD_WIDTH);

        assertEquals(new Tetromino(TetrominoType.T, 0, 0, SPAWN_T_COLUMN), block);
    }

    @Test
    void spawnOWithExpectedAbsolutePositions() {
        Tetromino block = Tetromino.spawn(TetrominoType.O, BOARD_WIDTH);
        Set<Position> expected = Set.of(
                new Position(SPAWN_ROW, SPAWN_O_COLUMN), new Position(SPAWN_ROW, SPAWN_O_COLUMN + 1),
                new Position(SPAWN_ROW + 1, SPAWN_O_COLUMN), new Position(SPAWN_ROW + 1, SPAWN_O_COLUMN + 1)
        );

        assertEquals(expected, Set.copyOf(block.getAbsolutePositions()));
    }

    @Test
    void spawnIWithExpectedAbsolutePositions() {
        Tetromino block = Tetromino.spawn(TetrominoType.I, BOARD_WIDTH);
        Set<Position> expected = Set.of(
                new Position(SPAWN_ROW + 1, SPAWN_I_COLUMN), new Position(SPAWN_ROW + 1, SPAWN_I_COLUMN + 1),
                new Position(SPAWN_ROW + 1, SPAWN_I_COLUMN + 2), new Position(SPAWN_ROW + 1, SPAWN_I_COLUMN + 3)
        );

        assertEquals(expected, Set.copyOf(block.getAbsolutePositions()));
    }

    @Test
    void spawnEveryTypeInsideBoardColumns() {
        for (TetrominoType type : TetrominoType.values()) {
            for (Position position : Tetromino.spawn(type, BOARD_WIDTH).getAbsolutePositions()) {
                assertTrue(
                    position.column() >= 0 && position.column() < BOARD_WIDTH,
                    type + " column " + position.column()
                );
                assertTrue(position.row() >= 0, type + " row " + position.row());
            }
        }
    }

    @Test
    void moveDownWithoutChangingOriginal() {
        Tetromino original = new Tetromino(TetrominoType.T, 0, 2, 3);

        Tetromino moved = original.move(1, 0);

        assertEquals(3, moved.row());
        assertEquals(2, original.row());
    }

    @Test
    void moveLeftByOneColumn() {
        Tetromino original = new Tetromino(TetrominoType.T, 0, 2, 3);

        assertEquals(2, original.move(0, -1).column());
    }

    @Test
    void rotateClockwiseWithoutChangingOriginalPositionOrType() {
        Tetromino original = new Tetromino(TetrominoType.T, 0, 2, 3);

        Tetromino rotated = original.rotateClockwise();

        assertEquals(1, rotated.rotation());
        assertEquals(original.type(), rotated.type());
        assertEquals(original.row(), rotated.row());
        assertEquals(original.column(), rotated.column());
        assertEquals(0, original.rotation());
    }

    @Test
    void returnOriginalAfterFourClockwiseRotations() {
        Tetromino original = new Tetromino(TetrominoType.T, 0, 2, 3);
        Tetromino rotated = original;

        for (int count = 0; count < ROTATION_COUNT; count++) {
            rotated = rotated.rotateClockwise();
        }

        assertEquals(original, rotated);
    }

    @Test
    void wrapClockwiseRotationFromThreeToZero() {
        Tetromino block = new Tetromino(TetrominoType.T, LAST_ROTATION, 2, 3);

        assertEquals(FIRST_ROTATION, block.rotateClockwise().rotation());
    }

    @Test
    void wrapCounterClockwiseRotationFromZeroToThree() {
        Tetromino block = new Tetromino(TetrominoType.T, FIRST_ROTATION, 2, 3);

        assertEquals(LAST_ROTATION, block.rotateCounterClockwise().rotation());
    }

    @Test
    void returnOriginalAfterClockwiseAndCounterClockwiseRotation() {
        Tetromino original = new Tetromino(TetrominoType.T, 0, 2, 3);

        assertEquals(original, original.rotateClockwise().rotateCounterClockwise());
    }

    @Test
    void rotateCounterClockwiseWithoutChangingOriginalPositionOrType() {
        Tetromino original = new Tetromino(TetrominoType.T, 1, 2, 3);

        Tetromino rotated = original.rotateCounterClockwise();

        assertEquals(0, rotated.rotation());
        assertEquals(original.type(), rotated.type());
        assertEquals(original.row(), rotated.row());
        assertEquals(original.column(), rotated.column());
        assertEquals(1, original.rotation());
    }

    @Test
    void calculateAbsolutePositionsFromOrigin() {
        Tetromino block = new Tetromino(TetrominoType.T, 0, 5, 3);
        Set<Position> expected = Set.of(
                new Position(5, 3), new Position(5, 4),
                new Position(5, 5), new Position(6, 4)
        );

        assertEquals(expected, Set.copyOf(block.getAbsolutePositions()));
    }

    @Test
    void throwWhenTypeIsNull() {
        assertThrows(NullPointerException.class, () -> new Tetromino(null, 0, 0, 0));
    }

    @Test
    void throwWhenRotationIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new Tetromino(TetrominoType.T, -1, 0, 0));
    }

    @Test
    void throwWhenRotationIsAboveRange() {
        assertThrows(IllegalArgumentException.class, () -> new Tetromino(TetrominoType.T, 4, 0, 0));
    }

    @Test
    void allowNegativeRowAndColumn() {
        Tetromino block = new Tetromino(TetrominoType.T, 0, -1, -2);

        assertEquals(-1, block.row());
        assertEquals(-2, block.column());
    }
    // AI-assisted code end (테스트 코드)
}
