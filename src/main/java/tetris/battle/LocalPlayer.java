package tetris.battle;

import tetris.model.Game;
import tetris.model.GameSnapshot;
import tetris.model.GameStatus;
import tetris.controller.GameController;

import java.util.List;
import static java.util.Objects.requireNonNull;

// 내 PC의 Game을 대전 플레이어로 감싼다.
public class LocalPlayer implements Player {

    private final Game game;
    private final GameController controller;

    // 생성할 때 game과 controller는 서로 연결된 상태로 매개변수로 들어온다고 가정한다.
    public LocalPlayer(Game game, GameController controller) {
        requireNonNull(game);
        requireNonNull(controller);

        this.game = game;
        this.controller = controller;
    }

    /** 지금 화면에 그릴 snapshot을 생성한다 */
    @Override
    public GameSnapshot createSnapshot() {
        return game.createSnapshot();
    }

    /**
     * 상대에게 보낼 공격 줄을 꺼낸다
     * 꺼낸 뒤에는 비워진다.
     */
    @Override
    public List<int[]> takeAttackRows() {
        return game.takeAttackRows();
    }

    /**
     * 상대에게서 온 공격 줄을 받는다
     * 
     * @param garbageRows 상대가 보낸 공격 줄
     */
    @Override
    public void receiveGarbageRows(List<int[]> garbageRows) {
        game.receiveGarbageRows(garbageRows);
    }

    /** 이 플레이어가 졌는지 확인한다 */
    @Override
    public boolean isGameOver() {
        return game.isGameOver();
    }

    
    /** 현재 점수를 반환한다. (제한시간 모드에서 승패 정할 때 사용) */
    @Override
    public long getScore() {
        return game.getScore();
    }

    /** 현재 상태를 반환한다. */
    @Override
    public GameStatus getStatus() {
        return game.getStatus();
    }

    /** 게임 일시정지 */
    @Override
    public void pause() {
        game.pause();
        controller.refresh();
    }

    /** 게임 재개 */
    @Override
    public void resume() {
        game.resume();
        controller.refresh();
    }

    /** 외부에서 경기종료 */
    @Override
    public void stop() {
        game.stop();
        controller.refresh();
    }

    /** 상태 바뀔 시 부를 동작 핸들러 등록 */
    @Override
    public void setOnUpdate(Runnable onUpdate) {
        controller.setOnUpdate(onUpdate);
    }

}
