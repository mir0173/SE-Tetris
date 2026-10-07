package tetris.model;

import java.util.List;
import java.util.ArrayList;

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
     */
    public void handleCommand(GameCommand command) {
        if (status != GameStatus.PLAYING) {
            return;
        }

        if(command == GameCommand.QUIT_GAME){
            status = GameStatus.QUIT;
        }

        switch (command) {
            case MOVE_LEFT -> tryReplace(currentPiece.move(0, -1));
            case MOVE_RIGHT -> tryReplace(currentPiece.move(0, 1));
            case ROTATE_CLOCKWISE -> tryReplace(currentPiece.rotateClockwise());
            case ROTATE_COUNTERCLOCKWISE -> tryReplace(currentPiece.rotateCounterClockwise());
            case SOFT_DROP -> softDrop();
            case HARD_DROP -> hardDrop();
            case HOLD -> hold();
            case QUIT_GAME -> status = GameStatus.QUIT;
            case PAUSE, NONE -> {
            } // Todo in T6
        }
    }

    public void tick() {
        if (status != GameStatus.PLAYING) {
            return;
        }
        softDrop();
    }

    public boolean isFinished() {
        return status == GameStatus.QUIT;
    }

    public boolean isGameOver() {
        return status == GameStatus.GAMEOVER;
    }

    // 마지막으로 고정된 블록이 지운 줄 수 (T6 점수 계산용)
    public int getLastClearedLines() {
        return lastClearedLines;
    }

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

    // 후보가 놓일 수 있으면현재 블록을 교체하고 true, 아니면 그대로 두고 false
    private boolean tryReplace(Tetromino candidate) {
        if (!board.canPlace(candidate)) {
            return false;
        } else {
            currentPiece = candidate;
            return true;
        }
    }

    // 한 칸 내리기. 더 내려갈 수 없으면 고정
    private void softDrop() {
        if (!tryReplace(currentPiece.move(1, 0))) {
            lockAndSpawnNext();
        }

    }

    // 바닥까지 한번에 내린 뒤 바로 고정
    private void hardDrop() {
        while (tryReplace(currentPiece.move(1, 0))) {
        }
        lockAndSpawnNext();
    }

    // 현재 블록을 고정하고, 줄을 지우고, 다음 블록을 내보냄
    private void lockAndSpawnNext() {
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