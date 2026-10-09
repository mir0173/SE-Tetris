package tetris.model;

import java.util.List;

public class Game {

    private static final long INITIAL_SCORE = 0L;
    private static final long INITIAL_DROP_INTERVAL = 1_000L;

    private final Board board;
    private final TetrominoGenerator generator;
    private GameStatus status;
    private Tetromino currentPiece;
    private TetrominoType nextPiece;
    private TetrominoType heldPiece; // 홀드된 블록 종류
    private boolean holdUsed; // 이번 블록에서 홀드를 이미 썼는지
    private int lastClearedLines; // 마지막 고정 때 지운 줄 수 (T6 점수 계산용)
    private long lockedPieceCount; // 고정된 블록 수
    private long gravityDropCells; // 자동 낙하 칸 수
    private long softDropCells; // 소프트 드롭 칸 수
    private long hardDropCells; // 하드 드롭 칸 수

    public Game(Board board, TetrominoGenerator generator) {
        if (board == null || generator == null) {
            String nullobject = board == null ? "board" : "generator";
            throw new NullPointerException("param : " + nullobject + " is null");
        }
        this.board = board;
        this.generator = generator;
        this.status = GameStatus.READY;
    }

    /**
     * 게임 시작 후 고정된 블록의 누적 개수를 반환한다.
     */
    public long getLockedPieceCount() {
        return lockedPieceCount;
    }

    /**
     * 중력(tick)으로 실제로 내려간 칸 수의 누적값을 반환한다.
     */
    public long getGravityDropCells() {
        return gravityDropCells;
    }

    /**
     * 소프트 드롭으로 실제로 내려간 칸 수의 누적값을 반환한다.
     */
    public long getSoftDropCells() {
        return softDropCells;
    }

    /**
     * 하드 드롭으로 실제로 내려간 칸 수의 누적값을 반환한다.
     * 이미 바닥에 붙은 상태에서 누르면 0칸으로 센다.
     */
    public long getHardDropCells() {
        return hardDropCells;
    }

    /**
     * 가장 최근에 블록이 고정될 때 지운 줄 수를 반환한다. 점수 계산에 사용된다.
     *
     * @return 지운 줄 수 (0~4)
     */
    public int getLastClearedLines() {
        return lastClearedLines;
    }

    /**
     * 게임을 시작 가능한 상태에서 실행 상태로 변경하고, 첫 블록을 내보낸다.
     */
    public void start() {
        if (status != GameStatus.READY) {
            return;
        }
        status = GameStatus.PLAYING;
        nextPiece = generator.generateNext();
        spawnNextPiece();
    }

    /**
     * 사용자 명령을 현재 게임 상태에 적용한다.
     * 실행 중(PLAYING)이 아니면 무시하며, 이동과 회전은 놓을 수 없는 위치면 무시된다.
     *
     * @param command 적용할 명령
     */
    public void handleCommand(GameCommand command) {
        if (status != GameStatus.PLAYING) {
            return;
        }

        switch (command) {
            case MOVE_LEFT -> tryReplace(currentPiece.move(0, -1));
            case MOVE_RIGHT -> tryReplace(currentPiece.move(0, 1));
            case ROTATE_CLOCKWISE -> tryReplace(currentPiece.rotateClockwise());
            case ROTATE_COUNTERCLOCKWISE -> tryReplace(currentPiece.rotateCounterClockwise());
            case SOFT_DROP -> {
                if (softDrop()) {
                    softDropCells++;
                }
            }
            case HARD_DROP -> hardDrop();
            case HOLD -> hold();
            case QUIT_GAME -> status = GameStatus.QUIT;
            case PAUSE, NONE -> {
            } // PAUSE는 T6에서 처리
        }
    }

    /**
     * 중력에 의해 블록을 한 칸 내린다. 더 내려갈 수 없으면 고정하고 다음 블록을 내보낸다.
     * 타이머가 일정 간격으로 호출되며, 실행 중이 아니면 무시한다.
     */
    public void tick() {
        if (status != GameStatus.PLAYING) {
            return;
        }
        if (softDrop()) {
            gravityDropCells++;
        }
    }

    // 사용자가 게임을 종료했으면 true (GAMEOVER는 포함되지 않음)
    public boolean isFinished() {
        return status == GameStatus.QUIT;
    }

    // 새 블록이 스폰 위치에 놓이지 못해 게임이 끝났으면 true
    public boolean isGameOver() {
        return status == GameStatus.GAMEOVER;
    }

    /**
     * 화면 표시용으로 현재 게임 상태의 사본을 만든다.
     * 셀에는 고정된 블록과 떨어지는 블록이 함께 담기며, 게임오버이거나 시작 전이면 고정된 블록만 담긴다.
     *
     * @return 현재 상태의 스냅샷
     */
    public GameSnapshot createSnapshot() {
        int[][] cells;
        if (isGameOver() || currentPiece == null) {
            cells = board.copyCells();
        } else {
            cells = board.copyCellsWith(currentPiece);
        }
        List<TetrominoType> list;
        if (nextPiece != null) {
            list = List.of(nextPiece);
        } else {
            list = List.of();
        }
        return new GameSnapshot(
                cells,
                list,
                INITIAL_SCORE,
                status,
                INITIAL_DROP_INTERVAL);
    }

    // 후보가 놓일 수 있으면 현재 블록을 교체하고 true, 아니면 그대로 두고 false
    private boolean tryReplace(Tetromino candidate) {
        if (!board.canPlace(candidate)) {
            return false;
        } else {
            currentPiece = candidate;
            return true;
        }
    }

    // 한 칸 내렸으면 true, 못 내려가서 고정했으면 false
    private boolean softDrop() {
        if (tryReplace(currentPiece.move(1, 0))) {
            return true;
        } else {
            lockAndSpawnNext();
            return false;
        }
    }

    // 바닥까지 한번에 내린 뒤 바로 고정
    private void hardDrop() {
        while (tryReplace(currentPiece.move(1, 0))) {
            hardDropCells++;
        }
        lockAndSpawnNext();
    }

    // 현재 블록을 고정하고, 줄을 지우고, 다음 블록을 내보냄
    private void lockAndSpawnNext() {
        lockedPieceCount++;
        board.lock(currentPiece);
        lastClearedLines = board.clearFullLines();
        holdUsed = false;
        spawnNextPiece();
    }

    // nextPiece를 꺼내 스폰하고, nextPiece를 새로 채움
    private void spawnNextPiece() {
        TetrominoType localNextPiece = nextPiece;
        nextPiece = generator.generateNext();
        spawn(localNextPiece);
    }

    // 주어진 종류를 스폰 위치에 놓음. 놓을 수 없으면 게임오버
    private void spawn(TetrominoType type) {
        Tetromino candidate = Tetromino.spawn(type, board.getWidth());
        if (!board.canPlace(candidate)) {
            status = GameStatus.GAMEOVER;
            return;
        }

        currentPiece = candidate;
    }

    // 현재 블록을 홀드하거나 홀드된 블록과 바꾼다. 블록 하나당 한번만 가능하다.
    private void hold() {
        if (holdUsed) {
            return;
        }

        TetrominoType currentType = currentPiece.type();
        if (heldPiece == null) {
            heldPiece = currentType;
            spawnNextPiece();
        } else {
            TetrominoType spawnType = heldPiece;
            heldPiece = currentType;
            spawn(spawnType);
        }
        holdUsed = true;
    }
}