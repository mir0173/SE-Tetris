package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// AI-assisted code start
class GameWallKickTest {
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;

    @Test
    void kickJRightWhenRotatingCounterclockwiseAtLeftWall() {
        Game game = startGame(new Board(BOARD_WIDTH, BOARD_HEIGHT), PieceType.J);
        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);
        repeat(game, GameCommand.MOVE_LEFT, BOARD_WIDTH);

        game.handleCommand(GameCommand.ROTATE_COUNTERCLOCKWISE);

        assertEquals(Set.of(new Position(0, 0), new Position(1, 0), new Position(1, 1), new Position(1, 2)),
                occupiedBy(game, PieceType.J));
    }

    @Test
    void kickLLeftWhenRotatingClockwiseAtRightWall() {
        Game game = startGame(new Board(BOARD_WIDTH, BOARD_HEIGHT), PieceType.L);
        game.handleCommand(GameCommand.ROTATE_COUNTERCLOCKWISE);
        repeat(game, GameCommand.MOVE_RIGHT, BOARD_WIDTH);

        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);

        assertEquals(Set.of(new Position(0, 9), new Position(1, 7), new Position(1, 8), new Position(1, 9)),
                occupiedBy(game, PieceType.L));
    }

    @Test
    void kickTUpAndLeftAtFloor() {
        Game game = startGame(new Board(BOARD_WIDTH, BOARD_HEIGHT), PieceType.T);
        repeat(game, GameCommand.SOFT_DROP, BOARD_HEIGHT - 2);

        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);

        assertEquals(Set.of(new Position(17, 3), new Position(18, 3), new Position(18, 4), new Position(19, 3)),
                occupiedBy(game, PieceType.T));
    }

    @ParameterizedTest
    @CsvSource({
            "I, ROTATE_CLOCKWISE, 6",
            "I, ROTATE_COUNTERCLOCKWISE, 3",
            "SPECIAL_I, ROTATE_CLOCKWISE, 6",
            "SPECIAL_I, ROTATE_COUNTERCLOCKWISE, 3"
    })
    void useIOffsetsAtFloorWithoutAddingDropScore(PieceType type, GameCommand rotation, int column) {
        Game game = startGame(new Board(BOARD_WIDTH, BOARD_HEIGHT), type);
        repeat(game, GameCommand.SOFT_DROP, BOARD_HEIGHT - 2);
        long score = game.getScore();

        game.handleCommand(rotation);

        Set<Position> expected = Set.of(new Position(16, column), new Position(17, column),
                new Position(18, column), new Position(19, column));
        assertEquals(expected, occupiedBy(game, type));
        assertEquals(score, game.getScore());
        System.out.printf("[SRS 바닥] %s %s, %d열 PASS%n", type, rotation, column);
    }

    @Test
    void useFirstValidOffsetWhenAnotherOffsetAlsoFits() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        Game game = startGame(board, PieceType.T);
        repeat(game, GameCommand.SOFT_DROP, 4);
        placeBlocker(board, 6, 4);

        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);

        assertEquals(Set.of(new Position(4, 3), new Position(5, 3), new Position(5, 4), new Position(6, 3)),
                occupiedBy(game, PieceType.T));
        System.out.println("[SRS 순서] 쌓인 블록 근처에서 첫 번째 유효 후보 선택 PASS");
    }

    @Test
    void preservePositionAndRotationWhenAllOffsetsAreBlocked() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        Game game = startGame(board, PieceType.T);
        repeat(game, GameCommand.SOFT_DROP, 4);
        placeBlocker(board, 6, 4);
        placeBlocker(board, 6, 3);
        placeBlocker(board, 3, 3);
        Set<Position> before = occupiedBy(game, PieceType.T);

        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);

        assertEquals(before, occupiedBy(game, PieceType.T));
        System.out.println("[SRS 충돌] 모든 후보가 막히면 기존 위치와 방향 유지 PASS");
    }

    @Test
    void preserveOSquaresWhenRotatingAtWall() {
        Game game = startGame(new Board(BOARD_WIDTH, BOARD_HEIGHT), PieceType.O);
        repeat(game, GameCommand.MOVE_LEFT, BOARD_WIDTH);
        Set<Position> before = occupiedBy(game, PieceType.O);

        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);
        assertEquals(before, occupiedBy(game, PieceType.O));
        game.handleCommand(GameCommand.ROTATE_COUNTERCLOCKWISE);
        assertEquals(before, occupiedBy(game, PieceType.O));
    }

    private static Game startGame(Board board, PieceType type) {
        TetrominoGenerator generator = new TetrominoGenerator(new Random(1)) {
            @Override
            public PieceType generateNext() {
                return type;
            }
        };
        Game game = new Game(board, generator, Difficulty.NORMAL, GameMode.NORMAL);
        game.start();
        return game;
    }

    private static void repeat(Game game, GameCommand command, int count) {
        for (int index = 0; index < count; index++) {
            game.handleCommand(command);
        }
    }

    private static void placeBlocker(Board board, int row, int column) {
        board.lock(new Piece(PieceType.SLOW, 0, row, column));
    }

    private static Set<Position> occupiedBy(Game game, PieceType type) {
        Set<Position> occupied = new HashSet<>();
        int[][] cells = game.createSnapshot().getCells();
        for (int row = 0; row < cells.length; row++) {
            for (int column = 0; column < cells[row].length; column++) {
                if (cells[row][column] == type.getCellId()) {
                    occupied.add(new Position(row, column));
                }
            }
        }
        return occupied;
    }
}
// AI-assisted code end
