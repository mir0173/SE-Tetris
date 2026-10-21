package tetris.model;

/**
 * 아이템의 효과를 정의한다.
 * 게임은 블록 고정, 칸 파괴, 줄 삭제, 첫 착지가 일어날 때 해당 아이템의 효과를 알린다.
 * 효과는 필요한 메서드만 재정의하고, 게임 상태 변경은 ItemContext에 요청한다.
 */
public interface ItemEffect {
    /**
     * 아이템이 포함된 블록이 보드에 고정된 직후 호출된다.
     * 
     * @param context  게임에 동작을 요청할 때 쓰는 컨텍스트
     * @param position 아이템이 박힌 칸의 보드 좌표(단독 아이템은 블록의 기준 칸)
     */
    default void handleLock(ItemContext context, Position position) {

    }

    /**
     * 보드에 고정된 아이템 칸이 줄 삭제나 다른 아이템 효과로 부서졌을 때 호출된다.
     * 
     * @param context  게임에 동작을 요청할때 쓰는 컨텍스트
     */
    default void handleDestroyed(ItemContext context) {

    }

    /**
     * 아이템이 포함된 블록이 고정되면서 가득 찬 줄이 지워졌을 때 호출된다.
     * 
     * @param context          게임에 동작을 요청할 때 쓰는 컨텍스트
     * @param clearedLineCount 이번 고정으로 지워진 줄 수
     */
    default void handleLinesCleared(ItemContext context, int clearedLineCount) {

    }

    /**
     * 아이템이 포함된 블록이 처음으로 아래가 막힌 순간, 고정되기 전에 호출된다.
     * 
     * @param context  게임에 동작을 요청할 때 쓰는 컨텍스트
     */
    default void handleFirstLanding(ItemContext context) {

    }
}
