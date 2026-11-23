package tetris.battle;

import tetris.model.GameSnapshot;
import tetris.model.GameStatus;
import java.util.List;

/**
 * 대전에서 심판(MatchManager)이 다루는 플레이어 한명. 로컬/원격 둘 다 같은 방식으로 다룬다.
 * 모든 메서드는 JavaFX 스레드에서만 불린다. 원격 쪽은 받은 데이터를 Platform.runLater로 넘긴 뒤 처리한다.
 * 공격 줄 형식 : 위에서 아래 순서이고, 각 줄의 길이는 보드폭, 채워진 칸은 Board.GARBAGE_CELL
 */
public interface Player {
    /** 화면에 그릴 상태 사진을 만든다. 
     * 화면 표시용이며 심판은 판정에 사용하지 않는다. 
     * 원격 플레이어는 받아 둔 최신 화면 정보를 돌려준다. 
     */
    GameSnapshot createSnapshot();

    /**
     * 상대에게 보낼 공격 줄을 꺼낸다
     * 꺼낸 뒤에는 비워진다.
     */
    List<int[]> takeAttackRows();

    /**
     * 상대에게서 온 공격 줄을 받는다
     * 
     * @param garbageRows 상대가 보낸 공격 줄
     */
    void receiveGarbageRows(List<int[]> garbageRows);

    /** 이 플레이어가 졌는지 확인한다 */
    boolean isGameOver();

    /** 현재 점수를 반환한다. (제한시간 모드에서 승패 정할 때 사용) */
    long getScore();

    /**
     * 현재 게임 상태를 반환한다.
     * 심판이 일시정지와 재개를 맞출 때 사용한다.
     * 원격 플레이어는 상대가 보내준 최신 상태를 돌려준다.
     */
    GameStatus getStatus();

    /** 게임 일시정지 */
    void pause();

    /** 게임 재개 */
    void resume();

    /** 외부에서 경기종료 */
    void stop();

    /** 상태 바뀔 시 부를 동작 핸들러 등록 */
    void setOnUpdate(Runnable onUpdate);
}
