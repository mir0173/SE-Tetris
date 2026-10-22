package tetris.model.effect;

import tetris.model.ItemEffect;
import tetris.model.ItemContext;

/**
 * SPECIAL_I 아이템 : 이 아이템으로 테트리스 시 점수를 두배로 획득한다.
 */
public class SpecialIEffect implements ItemEffect {

    private static final int TETRIS_LINE_COUNT = 4;

    @Override
    public void handleLinesCleared(ItemContext context, int clearedLineCount) {
        if (clearedLineCount == TETRIS_LINE_COUNT) {
            context.doubleScore();
        }
    }
}
