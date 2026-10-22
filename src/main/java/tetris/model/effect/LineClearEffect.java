package tetris.model.effect;

import tetris.model.ItemEffect;
import tetris.model.ItemContext;
import tetris.model.Position;

/**
 * L 아이템: 고정되면 박힌 칸의 행을 지운다.
 */
public class LineClearEffect implements ItemEffect {

    @Override
    public void handleLock(ItemContext context, Position position) {
        context.clearLine(position.row());
    }
}
