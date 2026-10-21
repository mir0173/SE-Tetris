package tetris.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import tetris.model.Difficulty;
import tetris.model.GameMode;

class ScoreEntryTest {

    // AI-assisted code start
    private static final String NAME = "alice";
    private static final long SCORE = 1200L;

    @Test
    void exposeGivenValues() {
        // given
        UUID id = UUID.randomUUID();

        // when
        ScoreEntry entry = new ScoreEntry(id, NAME, SCORE, GameMode.ITEM, Difficulty.HARD);

        // then
        assertEquals(id, entry.getId());
        assertEquals(NAME, entry.getName());
        assertEquals(SCORE, entry.getScore());
        assertEquals(GameMode.ITEM, entry.getMode());
        assertEquals(Difficulty.HARD, entry.getDifficulty());
    }

    @Test
    void keepModeAndDifficultyIndependently() {
        // given & when: 모드와 난이도를 서로 다른 값으로 준다
        ScoreEntry normalHard = new ScoreEntry(UUID.randomUUID(), NAME, SCORE, GameMode.NORMAL, Difficulty.HARD);
        ScoreEntry itemEasy = new ScoreEntry(UUID.randomUUID(), NAME, SCORE, GameMode.ITEM, Difficulty.EASY);

        // then: 값이 서로 바뀌어 저장되지 않는다
        assertEquals(GameMode.NORMAL, normalHard.getMode());
        assertEquals(Difficulty.HARD, normalHard.getDifficulty());
        assertEquals(GameMode.ITEM, itemEasy.getMode());
        assertEquals(Difficulty.EASY, itemEasy.getDifficulty());
    }

    @Test
    void provideCategoryOfModeAndDifficulty() {
        // given
        ScoreEntry entry = new ScoreEntry(UUID.randomUUID(), NAME, SCORE, GameMode.ITEM, Difficulty.HARD);

        // when
        ScoreCategory category = entry.getCategory();

        // then: 기록이 자기 조합을 스스로 알려 준다
        assertEquals(new ScoreCategory(GameMode.ITEM, Difficulty.HARD), category);
    }

    @Test
    void rejectNullMode() {
        // given & when & then
        assertThrows(NullPointerException.class,
                () -> new ScoreEntry(UUID.randomUUID(), NAME, SCORE, null, Difficulty.NORMAL));
    }

    @Test
    void rejectNullDifficulty() {
        // given & when & then
        assertThrows(NullPointerException.class,
                () -> new ScoreEntry(UUID.randomUUID(), NAME, SCORE, GameMode.NORMAL, null));
    }
    // AI-assisted code end
}
