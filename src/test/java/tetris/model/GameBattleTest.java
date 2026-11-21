package tetris.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * 줄 삭제 공격, 쓰레기 행 적용, 게임 상태 전환과 스냅샷 복사 규칙을 검증한다.
 */
class GameBattleTest {

    // AI-assisted code start
    // 공격 행과 테스트 보드 구성을 위한 공통 값
    private static final int BOARD_WIDTH = 4;
    private static final int BOARD_HEIGHT = 6;
    private static final int NORMAL_BOARD_WIDTH = 10;
    private static final int NORMAL_BOARD_HEIGHT = 20;
    private static final int GARBAGE_VALUE = Board.GARBAGE_CELL;
    private static final int EXPECTED_TWO_ROWS = 2;
    private static final int EXPECTED_ONE_ROW = 1;
    private static final int PIECE_CELL_MARKER = 1;
    private static final int MAX_PENDING_ROWS = 10;
    private static final int DISTINCT_ROW_MARKER_COLUMN = 0;
    private static final int FIRST_ROW_MARKER = 1;
    private static final int FIRST_DISCARDED_ROW_MARKER = 11;
    private static final int NINE_PENDING_ROWS = 9;
    private static final int ONE_NEW_ROW = 1;
    private static final int THREE_NEW_ROWS = 3;
    private static final int SEVEN_PENDING_ROWS = 7;
    private static final int FIVE_NEW_ROWS = 5;
    private static final int TWELVE_RECEIVED_ROWS = 12;
    private static final int NEXT_MARKER_OFFSET = 100;
    private static final int FIRST_ROW_LIST_INDEX = 0;
    private static final int THREE_KEPT_NEW_ROWS = 3;

    @Test // 두 줄을 지운 마지막 블록의 정확한 칸만 비우고 나머지를 공격 칸으로 기록하는지 확인하는 테스트
    void recordAttackRowsOnTwoLineClear() {
        // given: 첫 O를 왼쪽에 고정하고, 그 시점의 아래 행에서 첫 O의 칸을 기록한다.
        Game game = startedGame(new Board(BOARD_WIDTH, BOARD_HEIGHT), PieceType.O);
        game.handleCommand(GameCommand.MOVE_LEFT);
        game.handleCommand(GameCommand.HARD_DROP);
        int[] firstPieceColumns = new int[BOARD_WIDTH];
        int[][] cellsAfterFirstDrop = game.createSnapshot().getCells();
        for (int column = 0; column < BOARD_WIDTH; column++) {
            if (cellsAfterFirstDrop[BOARD_HEIGHT - 1][column] != Board.EMPTY_CELL) {
                firstPieceColumns[column] = PIECE_CELL_MARKER;
            }
        }
        // when: 두 번째 O를 오른쪽에 고정하고 줄 삭제를 끝내 공격 행을 확정한다.
        game.handleCommand(GameCommand.MOVE_RIGHT);
        game.handleCommand(GameCommand.HARD_DROP);
        game.finishLineClear();

        List<int[]> attackRows = game.takeAttackRows();

        // then: 첫 O의 칸은 쓰레기이고, 첫 O가 아닌 마지막 O의 칸은 비어 있다.
        assertEquals(EXPECTED_TWO_ROWS, attackRows.size());
        for (int[] row : attackRows) {
            for (int column = 0; column < BOARD_WIDTH; column++) {
                if (firstPieceColumns[column] == PIECE_CELL_MARKER) {
                    assertEquals(Board.GARBAGE_CELL, row[column]);
                } else {
                    assertEquals(Board.EMPTY_CELL, row[column]);
                }
            }
        }
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-공격] 2줄 삭제 → 공격 2줄, 마지막 블록 칸 제외 PASS");
    }

    @Test // 줄을 하나도 지우지 않은 고정에서는 보낼 공격 행을 만들지 않는지 확인하는 테스트
    void doNotRecordAttackRowsWhenNoLineCleared() {
        // given: 빈 보드에서 O 블록 하나를 시작한다.
        Game game = startedGame(new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT), PieceType.O);

        // when: 줄을 완성하지 않는 블록을 고정한다.
        game.handleCommand(GameCommand.HARD_DROP);

        // then: 공격 행 목록은 비어 있다.
        assertTrue(game.takeAttackRows().isEmpty());
    }

    @Test // 공격 행을 꺼내면 내부 목록이 비워지고 반환 배열이 이후 결과와 분리되는지 확인하는 테스트
    void emptyAttackRowsAfterTaking() {
        // given: 두 줄 삭제로 공격 행을 만든다.
        Game game = gameWithTwoLinesPending();
        game.finishLineClear();

        // when: 공격 행을 한 번 꺼낸다.
        game.takeAttackRows();

        // then: 내부 목록이 비워져 두 번째 호출에서는 아무 행도 나오지 않는다.
        assertTrue(game.takeAttackRows().isEmpty());
    }

    @Test // 받은 쓰레기 행이 다음 블록을 고정하기 전에는 보드에 적용되지 않는지 확인하는 테스트
    void waitToApplyReceivedRowsUntilNextLock() {
        // given: 빈 보드와 적용 대기 중인 쓰레기 행을 준비한다.
        Game game = startedGame(new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT), PieceType.O);
        int[][] before = game.createSnapshot().getCells();
        int[] garbage = garbageRow();

        game.receiveGarbageRows(List.of(garbage)); // 행은 먼저 대기 목록에만 저장된다.

        // then: 아직 고정 전이므로 보드와 대기 행 수가 유지된다.
        assertEquals(EXPECTED_ONE_ROW, game.getPendingGarbageRowCount());
        assertArrayEquals(before[NORMAL_BOARD_HEIGHT - 1], game.createSnapshot().getCells()[NORMAL_BOARD_HEIGHT - 1]);

        // when: 다음 블록을 HARD_DROP하여 고정한다.
        game.handleCommand(GameCommand.HARD_DROP);

        // then: 고정이 끝난 뒤 대기 행이 하단에 적용된다.
        assertEquals(0, game.getPendingGarbageRowCount());
        assertArrayEquals(garbage, game.createSnapshot().getCells()[NORMAL_BOARD_HEIGHT - 1]);
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-공격] 수신 행 1개가 다음 블록 고정 후 하단 적용 PASS");
    }

    @Test // 상단에 블록이 있는 상태에서 쓰레기 행을 밀어 넣으면 게임오버가 되는지 확인하는 테스트
    void becomeGameOverWhenGarbageOverflows() {
        // given: row 0에 블록을 두어 한 행을 밀면 블록이 보드 밖으로 나가게 한다.
        Board board = new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT);
        board.lock(new Piece(PieceType.SLOW, 0, 0, 0));
        Game game = startedGame(board, PieceType.O);
        Board controlBoard = new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT);
        controlBoard.lock(new Piece(PieceType.SLOW, 0, 0, 0));
        Game controlGame = startedGame(controlBoard, PieceType.O);

        // when: 비교 게임은 쓰레기 없이 고정하고, 검증 게임에는 쓰레기를 받은 뒤 고정한다.
        controlGame.handleCommand(GameCommand.HARD_DROP);
        game.receiveGarbageRows(List.of(garbageRow()));
        game.handleCommand(GameCommand.HARD_DROP); // 다음 고정 후 쓰레기 행을 적용한다.

        // then: 쓰레기가 없으면 게임오버가 아니고, row 0의 기존 블록은 쓰레기로 밀려나가 게임오버가 된다.
        assertNotEquals(GameStatus.GAMEOVER, controlGame.createSnapshot().getStatus());
        assertEquals(GameStatus.GAMEOVER, game.createSnapshot().getStatus());
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-공격] 상단 블록이 있는 상태에서 쓰레기 1행 → GAMEOVER PASS");
    }

    @Test // 잘못된 쓰레기 행이 하나라도 있으면 전체 입력을 거부하고 대기 목록을 바꾸지 않는지 확인하는 테스트
    void rejectInvalidGarbageRowsWithoutQueueing() {
        // given: 올바른 행 뒤에 길이가 잘못된 행을 함께 준비한다.
        Game game = startedGame(new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT), PieceType.O);
        List<int[]> rows = List.of(new int[NORMAL_BOARD_WIDTH], new int[NORMAL_BOARD_WIDTH - 1]);

        // when: 유효한 행이 먼저 있어도 전체 입력을 검증한다.
        assertThrows(IllegalArgumentException.class, () -> game.receiveGarbageRows(rows));
        // then: 실패한 입력은 아무 행도 남기지 않으며 null도 거부한다.
        assertEquals(0, game.getPendingGarbageRowCount());
        assertThrows(NullPointerException.class, () -> game.receiveGarbageRows(null));
    }

    @Test // 일시정지, 재개, 종료 전환과 게임오버 뒤 종료 불변성을 확인하는 테스트
    void transitionPauseResumeAndStop() {
        // given: 진행 중인 게임을 준비한다.
        Game game = startedGame(new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT), PieceType.O);

        // when: 일시정지하고 재개한 뒤 외부 종료한다.
        game.pause();
        assertEquals(GameStatus.PAUSED, game.createSnapshot().getStatus());
        game.resume();
        assertEquals(GameStatus.PLAYING, game.createSnapshot().getStatus());
        game.stop();
        assertEquals(GameStatus.QUIT, game.createSnapshot().getStatus());

        // row 0 블록은 한 행의 쓰레기를 밀어낼 때 overflow를 일으킨다.
        Board gameOverBoard = new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT);
        gameOverBoard.lock(new Piece(PieceType.SLOW, 0, 0, 0));
        Game gameOver = startedGame(gameOverBoard, PieceType.O);
        gameOver.receiveGarbageRows(List.of(garbageRow()));
        gameOver.handleCommand(GameCommand.HARD_DROP);
        gameOver.stop(); // 이미 게임오버이므로 상태를 바꾸지 않아야 한다.
        // then: 일반 게임은 QUIT가 되고 게임오버 게임은 GAMEOVER를 유지한다.
        assertEquals(GameStatus.GAMEOVER, gameOver.createSnapshot().getStatus());
    }

    @Test // 대기 쓰레기 행 스냅샷과 그 내부 배열 및 목록이 원본과 분리되는지 확인하는 테스트
    void snapshotContainsCopiesOfPendingRows() {
        // given: 쓰레기 행 하나를 대기시킨다.
        Game game = startedGame(new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT), PieceType.O);
        game.receiveGarbageRows(List.of(garbageRow()));

        // when: 스냅샷의 배열을 바꾸고 목록에도 추가를 시도한다.
        GameSnapshot snapshot = game.createSnapshot();
        List<int[]> rows = snapshot.getPendingGarbageRows();
        rows.get(0)[0] = Board.EMPTY_CELL;
        List<int[]> secondSnapshotRows = game.createSnapshot().getPendingGarbageRows();

        // then: 새 스냅샷은 원래 값을 유지하고 목록은 수정할 수 없다.
        assertEquals(EXPECTED_ONE_ROW, secondSnapshotRows.size());
        assertEquals(GARBAGE_VALUE, secondSnapshotRows.get(0)[0]);
        assertThrows(UnsupportedOperationException.class, () -> rows.add(new int[NORMAL_BOARD_WIDTH]));
    }

    // AI-assisted code start
    @Test // 대기 행이 9줄일 때 한 줄을 더 받으면 최대 10줄로 제한되는지 확인하는 테스트
    void capPendingRowsAtTenWhenNineReceiveOne() {
        // given: 정상 10폭 보드에 9개의 서로 다른 대기 행을 준비한다.
        Game game = startedGame(new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT), PieceType.O);
        List<int[]> existingRows = distinctGarbageRows(NINE_PENDING_ROWS, FIRST_ROW_MARKER);
        game.receiveGarbageRows(existingRows);

        // when: 한 줄을 추가로 받는다.
        game.receiveGarbageRows(distinctGarbageRows(ONE_NEW_ROW, NEXT_MARKER_OFFSET));

        // then: 대기 행 수는 최대값 10줄을 넘지 않는다.
        assertEquals(MAX_PENDING_ROWS, game.getPendingGarbageRowCount());
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-공격] 대기 9줄 + 1줄 → 10줄 PASS");
    }

    @Test // 대기열이 10줄로 꽉 찼을 때 새 한 줄을 무시하는지 확인하는 테스트
    void ignoreNewRowsWhenPendingIsFull() {
        // given: 값 A1부터 A10까지 서로 다른 대기 행을 준비한다.
        Game game = startedGame(new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT), PieceType.O);
        List<int[]> existingRows = distinctGarbageRows(MAX_PENDING_ROWS, FIRST_ROW_MARKER);
        game.receiveGarbageRows(existingRows);

        // when: 값 B인 새 한 줄을 받는다.
        int[] newRow = distinctGarbageRows(ONE_NEW_ROW, NEXT_MARKER_OFFSET).get(0);
        game.receiveGarbageRows(List.of(newRow));

        // then: 기존 10줄만 위에서 아래로 순서대로 유지된다.
        List<int[]> pendingRows = game.createSnapshot().getPendingGarbageRows();
        assertEquals(MAX_PENDING_ROWS, game.getPendingGarbageRowCount());
        assertEquals(MAX_PENDING_ROWS, pendingRows.size());
        assertDistinctRowsInOrder(existingRows, pendingRows);
        assertTrue(pendingRows.stream().noneMatch(row -> row[DISTINCT_ROW_MARKER_COLUMN] == newRow[DISTINCT_ROW_MARKER_COLUMN]));
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-공격] 대기 10줄 + 1줄 → 10줄 유지, 새 줄 무시 PASS");
    }

    @Test // 꽉 찬 대기열에 여러 새 줄을 받으면 모두 무시되는지 확인하는 테스트
    void ignoreAllNewRowsWhenFullAndReceivingSeveral() {
        // given: 기존 10줄을 대기시킨다.
        Game game = startedGame(new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT), PieceType.O);
        List<int[]> existingRows = distinctGarbageRows(MAX_PENDING_ROWS, FIRST_ROW_MARKER);
        game.receiveGarbageRows(existingRows);

        // when: 새 3줄을 한 번에 받는다.
        game.receiveGarbageRows(distinctGarbageRows(THREE_NEW_ROWS, NEXT_MARKER_OFFSET));

        // then: 대기열은 기존 10줄로 그대로 유지된다.
        List<int[]> pendingRows = game.createSnapshot().getPendingGarbageRows();
        assertEquals(MAX_PENDING_ROWS, game.getPendingGarbageRowCount());
        assertDistinctRowsInOrder(existingRows, pendingRows);
    }

    @Test // 대기열이 초과될 때 새 행의 아래쪽 두 줄이 잘리는지 확인하는 테스트
    void cutBottomOfNewRowsWhenOverflowing() {
        // given: 기존 A1부터 A7까지 7줄을 대기시킨다.
        Game game = startedGame(new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT), PieceType.O);
        List<int[]> existingRows = distinctGarbageRows(SEVEN_PENDING_ROWS, FIRST_ROW_MARKER);
        game.receiveGarbageRows(existingRows);

        // when: B1부터 B5까지 5줄을 추가로 받는다.
        List<int[]> newRows = distinctGarbageRows(FIVE_NEW_ROWS, NEXT_MARKER_OFFSET);
        game.receiveGarbageRows(newRows);

        // then: A1부터 A7과 B1부터 B3만 남고 B4와 B5는 없다.
        List<int[]> pendingRows = game.createSnapshot().getPendingGarbageRows();
        assertEquals(MAX_PENDING_ROWS, game.getPendingGarbageRowCount());
        assertEquals(MAX_PENDING_ROWS, pendingRows.size());
        assertDistinctRowsInOrder(existingRows, pendingRows.subList(0, SEVEN_PENDING_ROWS));
        assertDistinctRowsInOrder(newRows.subList(FIRST_ROW_LIST_INDEX, THREE_KEPT_NEW_ROWS),
                pendingRows.subList(SEVEN_PENDING_ROWS, MAX_PENDING_ROWS));
        assertTrue(pendingRows.stream()
                .noneMatch(row -> row[DISTINCT_ROW_MARKER_COLUMN] == newRows.get(THREE_KEPT_NEW_ROWS)[DISTINCT_ROW_MARKER_COLUMN]));
        assertTrue(pendingRows.stream()
                .noneMatch(row -> row[DISTINCT_ROW_MARKER_COLUMN] == newRows.get(THREE_KEPT_NEW_ROWS + 1)[DISTINCT_ROW_MARKER_COLUMN]));
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-공격] 대기 7줄 + 5줄 → 10줄, 새 줄 아래 2줄 잘림 PASS");
    }

    @Test // 한 번에 12줄을 받아도 앞의 10줄만 대기열에 남는지 확인하는 테스트
    void receiveTenRowsAtOnce() {
        // given: 정상 10폭 보드의 게임을 준비한다.
        Game game = startedGame(new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT), PieceType.O);
        List<int[]> receivedRows = distinctGarbageRows(TWELVE_RECEIVED_ROWS, FIRST_ROW_MARKER);

        // when: 12줄을 한 번에 받는다.
        game.receiveGarbageRows(receivedRows);

        // then: 앞의 10줄만 위에서 아래로 보존된다.
        List<int[]> pendingRows = game.createSnapshot().getPendingGarbageRows();
        assertEquals(MAX_PENDING_ROWS, game.getPendingGarbageRowCount());
        assertDistinctRowsInOrder(receivedRows.subList(0, MAX_PENDING_ROWS), pendingRows);
    }

    @Test // 대기 행 12줄을 받은 뒤 고정하면 보존된 10줄만 적용되는지 확인하는 테스트
    void applyAtMostTenRowsAfterLock() {
        // given: 12줄을 대기시키고 보존된 앞의 10줄을 준비한다.
        Game game = startedGame(new Board(NORMAL_BOARD_WIDTH, NORMAL_BOARD_HEIGHT), PieceType.O);
        List<int[]> receivedRows = distinctGarbageRows(TWELVE_RECEIVED_ROWS, FIRST_ROW_MARKER);
        game.receiveGarbageRows(receivedRows);
        List<int[]> keptRows = game.createSnapshot().getPendingGarbageRows();

        // when: 다음 블록을 HARD_DROP으로 고정한다.
        game.handleCommand(GameCommand.HARD_DROP);

        // then: 대기열은 비고 보드 맨 아래 10줄이 보존 순서대로 적용된다.
        assertEquals(0, game.getPendingGarbageRowCount());
        int[][] boardCells = game.createSnapshot().getCells();
        for (int rowIndex = 0; rowIndex < MAX_PENDING_ROWS; rowIndex++) {
            assertArrayEquals(keptRows.get(rowIndex),
                    boardCells[NORMAL_BOARD_HEIGHT - MAX_PENDING_ROWS + rowIndex]);
        }
    }
    // AI-assisted code end

    // 4칸 폭에서 O 블록 두 개가 아래 두 줄을 채우므로 마지막 O의 2칸씩이 공격 행에서 비워진다.
    private Game gameWithTwoLinesPending() {
        Game game = startedGame(new Board(BOARD_WIDTH, BOARD_HEIGHT), PieceType.O);
        game.handleCommand(GameCommand.MOVE_LEFT);
        game.handleCommand(GameCommand.HARD_DROP);
        game.handleCommand(GameCommand.MOVE_RIGHT);
        game.handleCommand(GameCommand.HARD_DROP);
        return game;
    }

    // 보드 폭만큼 쓰레기 값이 채워진 공격 행을 만든다.
    private int[] garbageRow() {
        int[] row = new int[NORMAL_BOARD_WIDTH];
        for (int column = 0; column < row.length; column++) {
            row[column] = GARBAGE_VALUE;
        }
        return row;
    }

    // 지정한 개수만큼 서로 다른 값의 쓰레기 행을 만든다.
    private List<int[]> distinctGarbageRows(int rowCount, int firstMarker) {
        List<int[]> rows = new ArrayList<>();
        for (int index = 0; index < rowCount; index++) {
            int[] row = new int[NORMAL_BOARD_WIDTH];
            for (int column = 0; column < row.length; column++) {
                row[column] = GARBAGE_VALUE;
            }
            row[DISTINCT_ROW_MARKER_COLUMN] = firstMarker + index;
            rows.add(row);
        }
        return rows;
    }

    // 실제 행들이 기대한 순서와 배열 값으로 일치하는지 확인한다.
    private void assertDistinctRowsInOrder(List<int[]> expectedRows, List<int[]> actualRows) {
        assertEquals(expectedRows.size(), actualRows.size());
        for (int index = 0; index < expectedRows.size(); index++) {
            assertArrayEquals(expectedRows.get(index), actualRows.get(index));
        }
    }

    // 지정한 보드와 블록 생성기로 시작 상태의 게임을 만든다.
    private Game startedGame(Board board, PieceType type) {
        Game game = new Game(board, constantGenerator(type), Difficulty.NORMAL, GameMode.NORMAL);
        game.start();
        return game;
    }

    // 테스트 중 다음 블록 종류가 달라지지 않도록 고정 생성기를 만든다.
    private TetrominoGenerator constantGenerator(PieceType type) {
        return new TetrominoGenerator(new Random(0L)) {
            @Override
            public PieceType generateNext() {
                return type;
            }
        };
    }
    // AI-assisted code end
}
