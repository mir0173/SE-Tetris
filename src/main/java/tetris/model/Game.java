package tetris.model;

import java.util.List;

public class Game {

    private static final long INITIAL_SCORE = 0L;
    private static final long INITIAL_DROP_INTERVAL = 1_000L;

    private final Board board;
    private GameStatus status;
    private Tetromino currentPiece;
    private TetrominoType nextPiece;
    private final TetrominoGenerator generator;

    public Game(Board board, TetrominoGenerator generator) {
        if(board == null || generator == null){
            String nullobject = board == null ? "board" : "generator";
            throw new NullPointerException("param : " + nullobject + " is null");
        }
        this.board = board;
        this.generator = generator;
        this.status = GameStatus.READY;
    }

    /**
     * 게임을 시작 가능한 상태에서 실행 상태로 변경한다.
     */
    public void start() {
        if (status != GameStatus.READY) {
            return;
        }
        status = GameStatus.PLAYING;
    }

    /**
     * 사용자 명령을 현재 게임 상태에 적용한다.
     */
    public void handleCommand(GameCommand command) {
        if (status != GameStatus.PLAYING) {
            return;
        }

        if (command == GameCommand.QUIT_GAME) {
            status = GameStatus.QUIT;
        }
    }

    public boolean isFinished() {
        return status == GameStatus.QUIT;
    }

    public GameSnapshot createSnapshot() {
        return new GameSnapshot(
                board.copyCells(),
                List.of(),
                INITIAL_SCORE,
                status,
                INITIAL_DROP_INTERVAL);
    }
}