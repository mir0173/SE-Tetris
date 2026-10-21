package tetris.model.effect;

import tetris.model.ItemEffect;
import tetris.model.ItemContext;

/**
 * 속도 저하 아이템: 이 칸이 부서지면 일정 시간 동안 낙하 속도가 느려진다.
 */
public class SlowEffect implements ItemEffect {

    @Override
    public void handleDestroyed(ItemContext context) {
        context.startSlowDown();
    }
}
