package tetris.model.effect;

import tetris.model.ItemEffect;
import tetris.model.ItemContext;

/**
 * 무게추 아이템: 처음 닿으면 아래 셀을 부수며 바닥까지 내려간 뒤 고정된다.
 */
public class HeavyWeightEffect implements ItemEffect {

    @Override
    public void handleFirstLanding(ItemContext context) {
        context.startDrilling();
    }
}
