package tetris.model.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import tetris.model.ItemContext;
import tetris.model.ItemEffect;
import tetris.model.Position;

class ItemEffectsTest {

    // AI-assisted code start
    private static final int CLEAR_ROW = 7;
    private static final int ONE_LINE = 1;
    private static final int TWO_LINES = 2;
    private static final int THREE_LINES = 3;
    private static final int TETRIS_LINES = 4;

    @Test
    void lineClearEffectRequestsOnlyLineClearOnLock() {
        RecordingContext context = new RecordingContext();

        new LineClearEffect().handleLock(context, new Position(CLEAR_ROW, 2));

        assertEquals(List.of("clearLine:" + CLEAR_ROW), context.requests);
    }

    @Test
    void heavyWeightEffectRequestsOnlyDrillingOnFirstLanding() {
        RecordingContext context = new RecordingContext();

        new HeavyWeightEffect().handleFirstLanding(context);

        assertEquals(List.of("startDrilling"), context.requests);
    }

    @Test
    void specialIEffectDoublesScoreOnlyForFourClearedLines() {
        RecordingContext context = new RecordingContext();
        ItemEffect effect = new SpecialIEffect();

        effect.handleLinesCleared(context, ONE_LINE);
        effect.handleLinesCleared(context, TWO_LINES);
        effect.handleLinesCleared(context, THREE_LINES);
        assertTrue(context.requests.isEmpty());

        effect.handleLinesCleared(context, TETRIS_LINES);

        assertEquals(List.of("doubleScore"), context.requests);
    }

    @Test
    void slowEffectRequestsOnlySlowDownWhenDestroyed() {
        RecordingContext context = new RecordingContext();

        new SlowEffect().handleDestroyed(context);

        assertEquals(List.of("startSlowDown"), context.requests);
    }

    @Test
    void blockGenerateEffectRequestsOnlySpecialPieceWhenDestroyed() {
        RecordingContext context = new RecordingContext();

        new BlockGenerateEffect().handleDestroyed(context);

        assertEquals(List.of("reserveSpecialPiece"), context.requests);
    }

    @Test
    void effectsDoNothingForHooksTheyDoNotHandle() {
        List<ItemEffect> effects = List.of(
                new LineClearEffect(),
                new HeavyWeightEffect(),
                new SpecialIEffect(),
                new SlowEffect(),
                new BlockGenerateEffect()
        );

        for (ItemEffect effect : effects) {
            RecordingContext context = new RecordingContext();
            effect.handleLock(context, new Position(CLEAR_ROW, 0));
            effect.handleDestroyed(context);
            effect.handleLinesCleared(context, TETRIS_LINES);
            effect.handleFirstLanding(context);

            assertEquals(ONE_LINE, context.requests.size(), effect.getClass().getSimpleName());
        }
    }

    private static class RecordingContext implements ItemContext {
        private final List<String> requests = new ArrayList<>();

        @Override
        public void clearLine(int rowIndex) {
            requests.add("clearLine:" + rowIndex);
        }

        @Override
        public void startDrilling() {
            requests.add("startDrilling");
        }

        @Override
        public void doubleScore() {
            requests.add("doubleScore");
        }

        @Override
        public void startSlowDown() {
            requests.add("startSlowDown");
        }

        @Override
        public void reserveSpecialPiece() {
            requests.add("reserveSpecialPiece");
        }
    }
    // AI-assisted code end
}
