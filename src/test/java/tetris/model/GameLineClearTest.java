package tetris.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * 줄 삭제 애니메이션을 위한 삭제 대기 상태(CLEARING)를 검증한다.
 * 줄이 완성되면 바로 지우지 않고 지워질 줄을 표시한 채 멈추고,
 * finishLineClear()가 호출되어야 줄 삭제, 점수, 다음 블록 생성이 이어진다.
 */
class GameLineClearTest {

    // AI-assisted code start
    private static final int SMALL_BOARD_WIDTH = 4;
    private static final int SMALL_BOARD_HEIGHT = 6;
    private static final int TETRIS_BOARD_HEIGHT = 8;
    private static final int TETRIS_LINE_COUNT = 4;
    private static final int VERTICAL_COLUMN = 2;
    private static final int BOTTOM_ROW = SMALL_BOARD_HEIGHT - 1;
    private static final int TETRIS_BOTTOM_ROW = TETRIS_BOARD_HEIGHT - 1;
    private static final int NORMAL_BOARD_WIDTH = 10;
    private static final int NORMAL_BOARD_HEIGHT = 20;
    private static final long LINE_CLEAR_BONUS = 100L;
    private static final long TWO_LINES_BONUS = LINE_CLEAR_BONUS * 2 * 2;
    private static final long FOUR_LINES_BONUS = LINE_CLEAR_BONUS * 4 * 4;

    // ---------- 삭제 대기 상태로 들어가기 ----------

    @Test
    void enterClearingWhenLockCompletesFullLines() {
        // given
        Game game = gameWithTwoFullLinesPending();

        // when
        GameSnapshot snapshot = game.createSnapshot();

        // then
        assertEquals(GameStatus.CLEARING, snapshot.getStatus());
        assertEquals(List.of(BOTTOM_ROW - 1, BOTTOM_ROW), snapshot.getClearingRows());
    }

    @Test
    void keepFullLinesOnBoardWhileClearing() {
        // given
        Game game = gameWithTwoFullLinesPending();

        // when
        int[][] cells = game.createSnapshot().getCells();

        // then: 지워질 줄이 그대로 남아 있고, 새 블록은 아직 나오지 않았다
        assertEquals(SMALL_BOARD_WIDTH, countNonEmptyCells(cells, BOTTOM_ROW));
        assertEquals(SMALL_BOARD_WIDTH, countNonEmptyCells(cells, BOTTOM_ROW - 1));
        assertEquals(SMALL_BOARD_WIDTH * 2, countNonEmptyCells(cells));
    }

    @Test
    void reportOnlyFullLinesWhenSeparatedByIncompleteLine() {
        // given: 7행과 5행은 한 칸(2열)만 비어 있고, 6행은 대부분 비어 있다
        Board board = new Board(SMALL_BOARD_WIDTH, TETRIS_BOARD_HEIGHT);
        fillRowExcept(board, TETRIS_BOTTOM_ROW, VERTICAL_COLUMN);
        fillRowExcept(board, TETRIS_BOTTOM_ROW - 2, VERTICAL_COLUMN);
        board.lock(new Piece(PieceType.SLOW, 0, TETRIS_BOTTOM_ROW - 1, 0));
        Game game = startedGame(board, PieceType.I);

        // when: 세로 I를 2열에 떨어뜨린다
        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);
        game.handleCommand(GameCommand.HARD_DROP);

        // then
        GameSnapshot snapshot = game.createSnapshot();
        assertEquals(GameStatus.CLEARING, snapshot.getStatus());
        assertEquals(List.of(TETRIS_BOTTOM_ROW - 2, TETRIS_BOTTOM_ROW), snapshot.getClearingRows());
    }

    @Test
    void reportFourFullLinesAtOnce() {
        // given
        Game game = gameWithFourFullLinesPending();

        // when
        GameSnapshot snapshot = game.createSnapshot();

        // then
        assertEquals(GameStatus.CLEARING, snapshot.getStatus());
        assertEquals(
                List.of(TETRIS_BOTTOM_ROW - 3, TETRIS_BOTTOM_ROW - 2, TETRIS_BOTTOM_ROW - 1, TETRIS_BOTTOM_ROW),
                snapshot.getClearingRows());
    }

    @Test
    void skipClearingWhenNoLineIsFull() {
        // given
        Board board = new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT);
        Game game = startedGame(board, PieceType.O);

        // when
        game.handleCommand(GameCommand.HARD_DROP);

        // then: 지금처럼 바로 다음 블록이 나온다
        GameSnapshot snapshot = game.createSnapshot();
        assertEquals(GameStatus.PLAYING, snapshot.getStatus());
        assertEquals(List.of(), snapshot.getClearingRows());
        assertEquals(1, game.getLockedPieceCount());
        assertTrue(countNonEmptyCells(snapshot.getCells(), 0) > 0);
    }

    @Test
    void reportNoClearingRowsOnNormalSnapshots() {
        // given
        Game game = startedGame(new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT), PieceType.O);

        // when & then
        assertEquals(List.of(), game.createSnapshot().getClearingRows());
    }

    @Test
    void delayLineClearBonusWhileClearing() {
        // given
        Game game = gameWithTwoFullLinesPending();

        // when
        long scoreWhileClearing = game.createSnapshot().getScore();

        // then: 낙하 점수만 있고 줄 삭제 보너스는 아직 없다
        assertEquals(game.getHardDropCells(), scoreWhileClearing);
        assertEquals(0, game.getLastClearedLines());
    }

    // ---------- 삭제 대기 중에는 입력과 낙하를 무시한다 ----------

    @ParameterizedTest
    @EnumSource(value = GameCommand.class, names = { "PAUSE", "QUIT_GAME" }, mode = EnumSource.Mode.EXCLUDE)
    void ignoreCommandsWhileClearing(GameCommand command) {
        // given
        Game game = gameWithTwoFullLinesPending();
        GameSnapshot before = game.createSnapshot();

        // when
        game.handleCommand(command);

        // then
        GameSnapshot after = game.createSnapshot();
        assertEquals(GameStatus.CLEARING, after.getStatus());
        assertEquals(before.getScore(), after.getScore());
        assertEquals(before.getClearingRows(), after.getClearingRows());
        assertCellsEqual(before.getCells(), after.getCells());
    }

    @Test
    void ignoreTickWhileClearing() {
        // given
        Game game = gameWithTwoFullLinesPending();
        GameSnapshot before = game.createSnapshot();
        long gravityDropCells = game.getGravityDropCells();

        // when
        game.tick();

        // then
        GameSnapshot after = game.createSnapshot();
        assertEquals(GameStatus.CLEARING, after.getStatus());
        assertEquals(gravityDropCells, game.getGravityDropCells());
        assertEquals(before.getScore(), after.getScore());
        assertCellsEqual(before.getCells(), after.getCells());
    }

    @Test
    void ignorePauseWhileClearing() {
        // given
        Game game = gameWithTwoFullLinesPending();

        // when
        game.handleCommand(GameCommand.PAUSE);

        // then
        assertFalse(game.isPaused());
        assertEquals(GameStatus.CLEARING, game.createSnapshot().getStatus());
    }

    @Test
    void ignoreQuitWhileClearing() {
        // given
        Game game = gameWithTwoFullLinesPending();

        // when
        game.handleCommand(GameCommand.QUIT_GAME);

        // then
        assertFalse(game.isFinished());
        assertEquals(GameStatus.CLEARING, game.createSnapshot().getStatus());
    }

    // ---------- finishLineClear() ----------

    @Test
    void removeFullLinesWhenFinished() {
        // given
        Game game = gameWithTwoFullLinesPending();

        // when
        game.finishLineClear();

        // then: 꽉 찬 두 줄이 지워지고 보드 맨 아래에는 새 블록 자리밖에 남지 않는다
        GameSnapshot snapshot = game.createSnapshot();
        assertEquals(GameStatus.PLAYING, snapshot.getStatus());
        assertEquals(List.of(), snapshot.getClearingRows());
        assertEquals(2, game.getLastClearedLines());
        assertEquals(0, countNonEmptyCells(snapshot.getCells(), BOTTOM_ROW));
        assertEquals(0, countNonEmptyCells(snapshot.getCells(), BOTTOM_ROW - 1));
    }

    @Test
    void spawnNextPieceWhenFinished() {
        // given
        Game game = gameWithTwoFullLinesPending();

        // when
        game.finishLineClear();

        // then: 새 O 블록이 맨 위에 나온다
        int[][] cells = game.createSnapshot().getCells();
        assertEquals(PieceType.O.getCellId(), cells[0][1]);
        assertEquals(PieceType.O.getCellId(), cells[0][2]);
        assertEquals(PieceType.O.getCellId(), cells[1][1]);
        assertEquals(PieceType.O.getCellId(), cells[1][2]);
        assertEquals(SMALL_BOARD_WIDTH, countNonEmptyCells(cells));
    }

    @Test
    void applyLineClearBonusWhenFinished() {
        // given
        Game game = gameWithTwoFullLinesPending();
        long scoreWhileClearing = game.createSnapshot().getScore();

        // when
        game.finishLineClear();

        // then: 지금과 같은 점수 규칙(보너스 = 100 × 줄 수²)
        assertEquals(scoreWhileClearing + TWO_LINES_BONUS, game.createSnapshot().getScore());
    }

    @Test
    void countLockedPieceWhenFinished() {
        // given
        Game game = gameWithTwoFullLinesPending();

        // when
        game.finishLineClear();

        // then
        assertEquals(2, game.getLockedPieceCount());
    }

    @Test
    void applyLineClearBonusOnlyOnceWhenFinishIsRepeated() {
        // given
        Game game = gameWithTwoFullLinesPending();
        game.finishLineClear();
        GameSnapshot before = game.createSnapshot();

        // when
        game.finishLineClear();

        // then
        GameSnapshot after = game.createSnapshot();
        assertEquals(before.getScore(), after.getScore());
        assertEquals(2, game.getLockedPieceCount());
        assertCellsEqual(before.getCells(), after.getCells());
    }

    @Test
    void ignoreFinishWhenNotClearing() {
        // given
        Game game = startedGame(new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT), PieceType.O);
        GameSnapshot before = game.createSnapshot();

        // when
        game.finishLineClear();

        // then
        GameSnapshot after = game.createSnapshot();
        assertEquals(GameStatus.PLAYING, after.getStatus());
        assertEquals(before.getScore(), after.getScore());
        assertCellsEqual(before.getCells(), after.getCells());
    }

    @Test
    void ignoreFinishBeforeStart() {
        // given
        Game game = new Game(new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT), constantGenerator(PieceType.O),
                Difficulty.NORMAL, GameMode.NORMAL);

        // when
        game.finishLineClear();

        // then
        assertEquals(GameStatus.READY, game.createSnapshot().getStatus());
    }

    @Test
    void ignoreFinishWhilePaused() {
        // given
        Game game = startedGame(new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT), PieceType.O);
        game.handleCommand(GameCommand.PAUSE);

        // when
        game.finishLineClear();

        // then
        assertTrue(game.isPaused());
    }

    @Test
    void resumeNormalPlayAfterFinished() {
        // given
        Game game = gameWithTwoFullLinesPending();
        game.finishLineClear();
        int[][] spawned = game.createSnapshot().getCells();

        // when
        game.handleCommand(GameCommand.MOVE_LEFT);
        game.tick();

        // then: 입력과 자동 낙하가 다시 동작한다
        int[][] moved = game.createSnapshot().getCells();
        assertEquals(GameStatus.PLAYING, game.createSnapshot().getStatus());
        assertEquals(PieceType.O.getCellId(), spawned[0][1]);
        assertEquals(PieceType.O.getCellId(), moved[1][0]);
        assertEquals(1, game.getGravityDropCells());
    }

    @Test
    void clearFourLinesAndApplyBonusWhenFinished() {
        // given
        Game game = gameWithFourFullLinesPending();
        long scoreWhileClearing = game.createSnapshot().getScore();

        // when
        game.finishLineClear();

        // then
        assertEquals(TETRIS_LINE_COUNT, game.getLastClearedLines());
        assertEquals(scoreWhileClearing + FOUR_LINES_BONUS, game.createSnapshot().getScore());
        assertEquals(GameStatus.PLAYING, game.createSnapshot().getStatus());
    }

    // ---------- 도우미 ----------

    // O 블록 두 개로 4칸 폭 보드의 아래 두 줄을 채운 삭제 대기 상태
    private Game gameWithTwoFullLinesPending() {
        Game game = startedGame(new Board(SMALL_BOARD_WIDTH, SMALL_BOARD_HEIGHT), PieceType.O);
        game.handleCommand(GameCommand.MOVE_LEFT);
        game.handleCommand(GameCommand.HARD_DROP);
        game.handleCommand(GameCommand.MOVE_RIGHT);
        game.handleCommand(GameCommand.HARD_DROP);
        return game;
    }

    // 4줄을 2열만 비우고 세로 I를 떨어뜨린 삭제 대기 상태
    private Game gameWithFourFullLinesPending() {
        Board board = new Board(SMALL_BOARD_WIDTH, TETRIS_BOARD_HEIGHT);
        for (int row = TETRIS_BOTTOM_ROW - TETRIS_LINE_COUNT + 1; row <= TETRIS_BOTTOM_ROW; row++) {
            fillRowExcept(board, row, VERTICAL_COLUMN);
        }
        Game game = startedGame(board, PieceType.I);
        game.handleCommand(GameCommand.ROTATE_CLOCKWISE);
        game.handleCommand(GameCommand.HARD_DROP);
        return game;
    }

    private void fillRowExcept(Board board, int row, int emptyColumn) {
        for (int column = 0; column < board.getWidth(); column++) {
            if (column != emptyColumn) {
                board.lock(new Piece(PieceType.SLOW, 0, row, column));
            }
        }
    }

    private Game startedGame(Board board, PieceType type) {
        Game game = new Game(board, constantGenerator(type), Difficulty.NORMAL, GameMode.NORMAL);
        game.start();
        return game;
    }

    private TetrominoGenerator constantGenerator(PieceType type) {
        return new TetrominoGenerator(new Random(0L)) {
            @Override
            public PieceType generateNext() {
                return type;
            }
        };
    }

    private int countNonEmptyCells(int[][] cells) {
        int count = 0;
        for (int row = 0; row < cells.length; row++) {
            count += countNonEmptyCells(cells, row);
        }
        return count;
    }

    private int countNonEmptyCells(int[][] cells, int row) {
        int count = 0;
        for (int cell : cells[row]) {
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
    // AI-assisted code end
}
