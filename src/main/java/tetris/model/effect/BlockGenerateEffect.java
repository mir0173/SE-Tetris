package tetris.model.effect;

import tetris.model.ItemEffect;
import tetris.model.ItemContext;

/**
 * 블록 생성 아이템: 이 칸이 부서지면 특수 블록 생성을 예약한다.
 */
public class BlockGenerateEffect implements ItemEffect {

    @Override
    public void handleDestroyed(ItemContext context) {
        context.reserveSpecialPiece();
    }
}
