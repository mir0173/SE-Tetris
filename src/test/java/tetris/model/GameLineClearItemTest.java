package tetris.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * L(줄 삭제) 아이템이 박힌 줄도 일반 줄과 같이 삭제 대기(CLEARING)를 거치는지 검증한다.
 * L 줄은 꽉 차지 않았어도 지워지며, 꽉 찬 줄과 겹치면 한 번만 센다.
 */
class GameLineClearItemTest {

    // AI-assisted code start
    private static final int BOARD_WIDTH = 4;
    private static final int BOARD_HEIGHT = 8;
    private static final int BOTTOM_ROW = BOARD_HEIGHT - 1;
    private static final int ITEM_SPAWN_LINE_COUNT = 10;
    private static final int VERTICAL_COLUMN = 2;
    private static final long LINE_CLEAR_BONUS = 100L;
    private static final long RANDOM_SEED = 0L;

    @Test
    void enterClearingWhenLineClearItemIsLockedOnNotFullRow() {
        // given
        Fixture fixture = fixtureWithVerticalLineClearPiece();

        // when
        fixture.game().handleCommand(GameCommand.HARD_DROP);

        // then
        assertEquals(GameStatus.CLEARING, fixture.game().createSnapshot().getStatus());
    }

    @Test
    void reportOnlyItemRowAsClearingRowWhenNoRowIsFull() {
        // given
        Fixture fixture = fixtureWithVerticalLineClearPiece();

        // when
        fixture.game().handleCommand(GameCommand.HARD_DROP);

        // then: 세로 I는 어느 줄도 꽉 채우지 못하므로 L이 박힌 줄 하나만 지워질 줄이다
        GameSnapshot snapshot = fixture.game().createSnapshot();
        int itemRow = findItemRow(snapshot);
        assertEquals(List.of(itemRow), snapshot.getClearingRows());
        assertEquals(1, countNonEmptyCells(snapshot.getCells()[itemRow]));
    }

    @Test
    void keepItemRowOnBoardWhileClearing() {
        // given
        Fixture fixture = fixtureWithVerticalLineClearPiece();

        // when
        fixture.game().handleCommand(GameCommand.HARD_DROP);

        // then: 대기 중에는 아직 아무 줄도 사라지지 않았다
        assertEquals(4, countNonEmptyCellsInColumn(fixture.game().createSnapshot().getCells(), VERTICAL_COLUMN, 0));
    }

    @Test
    void removeItemRowWhenFinished() {
        // given
        Fixture fixture = fixtureWithVerticalLineClearPiece();
        fixture.game().handleCommand(GameCommand.HARD_DROP);

        // when
        fixture.game().finishLineClear();

        // then
        assertEquals(GameStatus.PLAYING, fixture.game().createSnapshot().getStatus());
        assertEquals(1, fixture.game().getLastClearedLines());
        assertEquals(3, countNonEmptyCellsInColumn(fixture.game().createSnapshot().getCells(), VERTICAL_COLUMN, BOARD_HEIGHT / 2));
        assertFalse(hasItemCode(fixture.game().createSnapshot(), ItemType.LINE_CLEAR));
    }

    @Test
    void delayItemRowBonusUntilFinished() {
        // given
        Fixture fixture = fixtureWithVerticalLineClearPiece();
        fixture.game().handleCommand(GameCommand.HARD_DROP);
        long scoreWhileClearing = fixture.game().createSnapshot().getScore();

        // when
        fixture.game().finishLineClear();

        // then: 줄 하나를 지운 보너스(100 × 현재 레벨)가 마무리 때 한 번 붙는다
        long bonus = fixture.game().createSnapshot().getScore() - scoreWhileClearing;
        assertTrue(bonus >= LINE_CLEAR_BONUS);
        assertEquals(0, bonus % LINE_CLEAR_BONUS);
    }

    @Test
    void ignoreInputWhileItemRowIsClearing() {
        // given
        Fixture fixture = fixtureWithVerticalLineClearPiece();
        fixture.game().handleCommand(GameCommand.HARD_DROP);
        int[][] before = fixture.game().createSnapshot().getCells();

        // when
        fixture.game().handleCommand(GameCommand.MOVE_LEFT);
        fixture.game().tick();

        // then
        assertEquals(GameStatus.CLEARING, fixture.game().createSnapshot().getStatus());
        assertBoardEquals(before, fixture.game().createSnapshot().getCells());
    }

    @Test
    void countRowOnlyOnceWhenItemRowIsAlsoFull() {
        // given: 가로 I가 바닥 줄을 꽉 채우므로 L 줄이 곧 꽉 찬 줄이다
        Fixture fixture = fixtureWithLineClearPiece();

        // when
        fixture.game().handleCommand(GameCommand.HARD_DROP);

        // then
        GameSnapshot snapshot = fixture.game().createSnapshot();
        assertEquals(GameStatus.CLEARING, snapshot.getStatus());
        assertEquals(List.of(BOTTOM_ROW), snapshot.getClearingRows());

        fixture.game().finishLineClear();
        assertEquals(1, fixture.game().getLastClearedLines());
    }

    @Test
    void clearItemRowAndFullRowTogether() {
        // given: 7행은 2열만 비어 있고, 세로 I가 2열에 떨어지면 7행이 꽉 찬다
        Fixture fixture = fixtureWithVerticalLineClearPiece();
        fillRowExcept(fixture.board(), BOTTOM_ROW, VERTICAL_COLUMN);

        // when
        fixture.game().handleCommand(GameCommand.HARD_DROP);

        // then: L 줄과 꽉 찬 줄(겹치면 하나)이 함께 지워질 줄이다
        GameSnapshot snapshot = fixture.game().createSnapshot();
        int itemRow = findItemRow(snapshot);
        List<Integer> expectedRows = new ArrayList<>(new TreeSet<>(List.of(itemRow, BOTTOM_ROW)));
        assertEquals(expectedRows, snapshot.getClearingRows());

        fixture.game().finishLineClear();
        assertEquals(expectedRows.size(), fixture.game().getLastClearedLines());
    }

    @Test
    void applyBonusForAllRowsClearedTogether() {
        // given: L 줄 하나만 지울 때의 보너스를 기준으로 삼는다 (같은 진행 상황이라 레벨이 같다)
        long singleRowBonus = bonusOfFinishing(fixtureWithVerticalLineClearPiece(), false);

        Fixture fixture = fixtureWithVerticalLineClearPiece();
        fillRowExcept(fixture.board(), BOTTOM_ROW, VERTICAL_COLUMN);
        fixture.game().handleCommand(GameCommand.HARD_DROP);
        int clearedRows = fixture.game().createSnapshot().getClearingRows().size();
        long scoreWhileClearing = fixture.game().createSnapshot().getScore();

        // when
        fixture.game().finishLineClear();

        // then: 보너스 = 기준 보너스 × 줄 수² (줄이 겹치면 1줄, 아니면 2줄)
        long bonus = fixture.game().createSnapshot().getScore() - scoreWhileClearing;
        assertEquals(singleRowBonus * clearedRows * clearedRows, bonus);
    }

    @Test
    void spawnNextPieceAfterItemRowIsRemoved() {
        // given
        Fixture fixture = fixtureWithVerticalLineClearPiece();
        fixture.game().handleCommand(GameCommand.HARD_DROP);

        // when
        fixture.game().finishLineClear();

        // then: 새 블록이 맨 위에 나온다
        assertTrue(countNonEmptyCells(fixture.game().createSnapshot().getCells()[0]) > 0
                || countNonEmptyCells(fixture.game().createSnapshot().getCells()[1]) > 0);
    }

    // ---------- 도우미 ----------

    // 줄을 10개 지워 L 아이템이 붙은 I 블록이 막 나온 상태 (가로 방향)
    private Fixture fixtureWithLineClearPiece() {
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        Game game = new Game(board, constantGenerator(PieceType.I), itemGenerator(ItemType.LINE_CLEAR),
                Difficulty.NORMAL, GameMode.ITEM);
        game.start();
        for (int index = 0; index < ITEM_SPAWN_LINE_COUNT; index++) {
            hardDropAndFinishClearing(game);
        }
        hardDropAndFinishClearing(game);
        return new Fixture(board, game);
    }

    // 위 상태에서 블록을 세로로 세워 2열에 놓은 상태
    private Fixture fixtureWithVerticalLineClearPiece() {
        Fixture fixture = fixtureWithLineClearPiece();
        fixture.game().handleCommand(GameCommand.ROTATE_CLOCKWISE);
        return fixture;
    }

    // 블록을 떨어뜨리고 삭제를 마무리했을 때 늘어난 보너스 점수를 반환한다
    private long bonusOfFinishing(Fixture fixture, boolean withFullRow) {
        if (withFullRow) {
            fillRowExcept(fixture.board(), BOTTOM_ROW, VERTICAL_COLUMN);
        }
        fixture.game().handleCommand(GameCommand.HARD_DROP);
        long scoreWhileClearing = fixture.game().createSnapshot().getScore();
        fixture.game().finishLineClear();
        return fixture.game().createSnapshot().getScore() - scoreWhileClearing;
    }

    private void hardDropAndFinishClearing(Game game) {
        game.handleCommand(GameCommand.HARD_DROP);
        if (game.createSnapshot().getStatus() == GameStatus.CLEARING) {
            game.finishLineClear();
        }
    }

    private void fillRowExcept(Board board, int row, int emptyColumn) {
        for (int column = 0; column < board.getWidth(); column++) {
            if (column != emptyColumn) {
                board.lock(new Piece(PieceType.SLOW, 0, row, column));
            }
        }
    }

    private int findItemRow(GameSnapshot snapshot) {
        int itemCode = ItemType.LINE_CLEAR.getItemCode();
        int[][] cells = snapshot.getCells();
        for (int row = 0; row < cells.length; row++) {
            for (int cell : cells[row]) {
                if (Board.extractItemCode(cell) == itemCode) {
                    return row;
                }
            }
        }
        throw new AssertionError("L item is not present");
    }

    private boolean hasItemCode(GameSnapshot snapshot, ItemType type) {
        for (int[] row : snapshot.getCells()) {
            for (int cell : row) {
                if (Board.extractItemCode(cell) == type.getItemCode()) {
                    return true;
                }
            }
        }
        return false;
    }

    private int countNonEmptyCells(int[] row) {
        int count = 0;
        for (int cell : row) {
            if (cell != Board.EMPTY_CELL) {
                count++;
            }
        }
        return count;
    }

    // firstRow 이상 행에서 해당 열의 채워진 칸 수를 센다 (맨 위에 새로 나온 블록을 빼려고 시작 행을 받는다)
    private int countNonEmptyCellsInColumn(int[][] cells, int column, int firstRow) {
        int count = 0;
        for (int row = firstRow; row < cells.length; row++) {
            if (cells[row][column] != Board.EMPTY_CELL) {
                count++;
            }
        }
        return count;
    }

    private void assertBoardEquals(int[][] expected, int[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int row = 0; row < expected.length; row++) {
            assertEquals(List.of(toList(expected[row])), List.of(toList(actual[row])), "row " + row);
        }
    }

    private List<Integer> toList(int[] values) {
        List<Integer> list = new ArrayList<>();
        for (int value : values) {
            list.add(value);
        }
        return list;
    }

    private ItemGenerator itemGenerator(ItemType selectedType) {
        Map<ItemType, Integer> weights = new EnumMap<>(ItemType.class);
        for (ItemType type : ItemType.values()) {
            weights.put(type, type == selectedType ? 1 : 0);
        }
        return new ItemGenerator(new Random(RANDOM_SEED), weights);
    }

    private TetrominoGenerator constantGenerator(PieceType type) {
        return new TetrominoGenerator(new Random(RANDOM_SEED)) {
            @Override
            public PieceType generateNext() {
                return type;
            }
        };
    }

    private record Fixture(Board board, Game game) {
    }
    // AI-assisted code end
}
