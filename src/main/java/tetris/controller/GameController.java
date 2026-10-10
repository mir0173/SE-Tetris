package tetris.controller;

import java.util.function.Consumer;

import tetris.model.Game;
import tetris.model.GameCommand;
import tetris.model.GameSnapshot;
import tetris.view.GameView;

/**
 * 게임 모델(Game)과 화면(GameView), 타이머(GameTimer)를 연결한다.
 * 화면에서 들어온 명령과 자동 낙하를 게임에 전달하고, 바뀐 상태를 화면에 다시 그린다
 */
public class GameController {

    private final Game game;                // 게임 모델
    private final GameView view;            // 게임 화면
    private final GameTimer timer;          // 블록을 떨어트리는 게임 타이머
    private final Runnable onReturnToMenu;  // 사용자가 게임을 종료했을 때 실행할 동작
    private final Consumer<Long> onGameOver;

    private long appliedDropInterval = Game.getInitialDropInterval(); 
    private boolean timerRunning;
    private boolean started;
    private boolean terminalHandled;         
    /**
     * 게임, 화면, 타이머를 연결하는 컨트롤러를 생성한다.
     * 
     * @param game              진행할 게임
     * @param view              게임을 표시할 화면
     * @param timer             자동 낙하에 사용할 타이머
     * @param onReturnToMenu    게임 종료(QUIT) 시 실행할 동작
     * @param onGameOver        게임 오버 시 최종 점수를 받을 동작
     */
    public GameController(Game game, GameView view, GameTimer timer, Runnable onReturnToMenu, Consumer<Long> onGameOver) {
        this.game = game;
        this.view = view;
        this.timer = timer;
        this.onReturnToMenu = onReturnToMenu;
        this.onGameOver = onGameOver;
    }

    /**
     * 게임을 시작한다.
     * 키 입력 처리기를 등록하고, 첫 블록을 내보낸 뒤 화면을 띄우고, 자동 낙하 타이머를 시작한다.
     */
    public void start() {
        if (started) {
            return;
        }
        started = true;
        view.setCommandHandler(this::handleCommand);
        game.start();
        view.render(game.createSnapshot());    
        view.show();
        
        synchronizeState();
    }

    // 타이머 간격마다 호출 : 블록을 한 칸 내리고 화면 갱신, 게임 오버가 되면 타이머를 멈춘다.
    private void handleTick() {
        if (terminalHandled) {
            return;
        }

        game.tick();
        synchronizeState();
    }

    // 화면에서 들어온 명령을 게임에 적용하고 화면 갱신. QUIT이면 메뉴로 돌아가고, 게임 오버면 타이머를 멈춘다.
    private void handleCommand(GameCommand command) {
        if (terminalHandled) {
            return;
        }

        game.handleCommand(command);
        synchronizeState();
    }

    // 현재 게임 상태의 스냅샷을 만들어 화면에 그린다.
    private void synchronizeState() {
        GameSnapshot snapshot = game.createSnapshot();
        view.render(snapshot);

        if (game.isFinished() || game.isGameOver()) {
            stopTimer();
            terminalHandled = true;
            if(game.isGameOver()) {
                onGameOver.accept(snapshot.getScore());
            } else {
                onReturnToMenu.run();
            }
            return;
        } 
        
        if (game.isPaused()) {
            stopTimer();
            return;
        } 

        long dropInterval = snapshot.getDropInterval();

        if (dropInterval != appliedDropInterval) {
            timer.setInterval(dropInterval);
            appliedDropInterval = dropInterval;
        }

        if (!timerRunning) {
            timer.start(this::handleTick);
            timerRunning = true;
        }
    }

    private void stopTimer() {
        if (timerRunning) {
            timer.stop();
            timerRunning = false;
        }
    }
}