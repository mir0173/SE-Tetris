package tetris.model;

/** 
 * 아이템 효과가 게임에 요청할 수 있는 동작 모음
*/
public interface ItemContext {
    /**
     * rowIndex 행을 지워달라고 요청하는 메서드
     * 
     * @param rowIndex 보드 기준행 번호, 맨 위가 0
     */
    void clearLine(int rowIndex);

    /**
     * 현재 블록이 고정되지 않고, 아래 칸을 부수며 내려가는 드릴 상태로 바꿔달라고 요청한다.
     */
    void startDrilling();
}
