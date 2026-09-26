package tetris.model;
import java.util.List;
public class Game {

    private static final long INITIAL_SCORE = 0L;
    private static final long INITIAL_DROP_INTERVAL = 1_000L;
    
    private final Board board;
    private GameStatus status;

    public Game(Board board) {
        this.board = board;
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

        if (command == GameCommand.QUIT_GAME
        ) {
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
                INITIAL_DROP_INTERVAL
        );
    }
}