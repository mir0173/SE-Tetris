package tetris.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import tetris.model.Difficulty;
import tetris.model.GameMode;

class ScoreCategoryTest {

    // AI-assisted code start
    private static final int CATEGORY_COUNT = 6;

    @Test
    void provideSixCategories() {
        // given

        // when
        List<ScoreCategory> categories = ScoreCategory.getAllCategories();

        // then
        assertEquals(CATEGORY_COUNT, categories.size());
    }

    @Test
    void provideEveryModeAndDifficultyCombinationOnce() {
        // given
        Set<ScoreCategory> expected = new HashSet<>();
        for (GameMode mode : GameMode.values()) {
            for (Difficulty difficulty : Difficulty.values()) {
                expected.add(new ScoreCategory(mode, difficulty));
            }
        }

        // when
        List<ScoreCategory> categories = ScoreCategory.getAllCategories();

        // then: 6개가 서로 겹치지 않고 모든 조합을 담는다
        assertEquals(expected, new HashSet<>(categories));
        assertEquals(CATEGORY_COUNT, new HashSet<>(categories).size());
    }

    @Test
    void keepNormalModeFirstThenItemModeInDifficultyOrder() {
        // given: 일반 EASY → NORMAL → HARD, 아이템 EASY → NORMAL → HARD 순서가 화면 순서다
        List<ScoreCategory> expected = List.of(
                new ScoreCategory(GameMode.NORMAL, Difficulty.EASY),
                new ScoreCategory(GameMode.NORMAL, Difficulty.NORMAL),
                new ScoreCategory(GameMode.NORMAL, Difficulty.HARD),
                new ScoreCategory(GameMode.ITEM, Difficulty.EASY),
                new ScoreCategory(GameMode.ITEM, Difficulty.NORMAL),
                new ScoreCategory(GameMode.ITEM, Difficulty.HARD));

        // when
        List<ScoreCategory> categories = ScoreCategory.getAllCategories();

        // then
        assertEquals(expected, categories);
    }

    @Test
    void rejectModification() {
        // given
        List<ScoreCategory> categories = ScoreCategory.getAllCategories();

        // when & then: 꺼낸 목록을 바꿔서 순서를 망가뜨릴 수 없다
        assertThrows(UnsupportedOperationException.class,
                () -> categories.add(new ScoreCategory(GameMode.NORMAL, Difficulty.EASY)));
    }

    @Test
    void moveToNextCategory() {
        // given
        ScoreCategory first = firstCategory();

        // when
        ScoreCategory moved = first.move(1);

        // then
        assertEquals(new ScoreCategory(GameMode.NORMAL, Difficulty.NORMAL), moved);
    }

    @Test
    void moveToPreviousCategory() {
        // given
        ScoreCategory second = ScoreCategory.getAllCategories().get(1);

        // when
        ScoreCategory moved = second.move(-1);

        // then
        assertEquals(firstCategory(), moved);
    }

    @Test
    void wrapToLastCategoryWhenMovingBackFromFirst() {
        // given
        ScoreCategory first = firstCategory();

        // when
        ScoreCategory moved = first.move(-1);

        // then: 1번에서 왼쪽으로 가면 6번
        assertEquals(lastCategory(), moved);
    }

    @Test
    void wrapToFirstCategoryWhenMovingForwardFromLast() {
        // given
        ScoreCategory last = lastCategory();

        // when
        ScoreCategory moved = last.move(1);

        // then: 6번에서 오른쪽으로 가면 1번
        assertEquals(firstCategory(), moved);
    }

    @Test
    void stayWhenOffsetIsZero() {
        // given
        ScoreCategory category = ScoreCategory.getAllCategories().get(2);

        // when
        ScoreCategory moved = category.move(0);

        // then
        assertEquals(category, moved);
    }

    @Test
    void returnToSameCategoryAfterFullLap() {
        // given
        ScoreCategory category = ScoreCategory.getAllCategories().get(2);

        // when & then: 한 바퀴(6칸)를 돌면 제자리, 반대 방향도 같다
        assertEquals(category, category.move(CATEGORY_COUNT));
        assertEquals(category, category.move(-CATEGORY_COUNT));
    }

    @Test
    void wrapWhenOffsetIsLargerThanCategoryCount() {
        // given
        ScoreCategory first = firstCategory();

        // when & then: 한 바퀴보다 더 큰 이동도 같은 결과로 접힌다
        assertEquals(first.move(1), first.move(CATEGORY_COUNT + 1));
        assertEquals(first.move(-1), first.move(-(CATEGORY_COUNT + 1)));
    }

    @Test
    void visitEveryCategoryWhenMovingForwardRepeatedly() {
        // given
        ScoreCategory current = firstCategory();
        Set<ScoreCategory> visited = new HashSet<>();

        // when
        for (int i = 0; i < CATEGORY_COUNT; i++) {
            visited.add(current);
            current = current.move(1);
        }

        // then: 6번 이동하면 모든 카테고리를 한 번씩 지나 처음으로 돌아온다
        assertEquals(CATEGORY_COUNT, visited.size());
        assertEquals(firstCategory(), current);
    }

    @Test
    void moveBackAndForthReturnsToOriginalCategory() {
        // given
        for (ScoreCategory category : ScoreCategory.getAllCategories()) {

            // when
            ScoreCategory moved = category.move(1).move(-1);

            // then
            assertEquals(category, moved);
        }
    }

    @Test
    void exposeModeAndDifficulty() {
        // given
        ScoreCategory category = new ScoreCategory(GameMode.ITEM, Difficulty.HARD);

        // when & then
        assertEquals(GameMode.ITEM, category.mode());
        assertEquals(Difficulty.HARD, category.difficulty());
    }

    @Test
    void treatSameModeAndDifficultyAsEqual() {
        // given
        ScoreCategory first = new ScoreCategory(GameMode.ITEM, Difficulty.EASY);
        ScoreCategory second = new ScoreCategory(GameMode.ITEM, Difficulty.EASY);

        // when & then: 값이 같으면 같은 카테고리로 취급해서 목록 검색과 Map 키에 쓸 수 있다
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void treatDifferentModeOrDifficultyAsDifferent() {
        // given
        ScoreCategory base = new ScoreCategory(GameMode.NORMAL, Difficulty.EASY);

        // when & then
        assertNotEquals(base, new ScoreCategory(GameMode.ITEM, Difficulty.EASY));
        assertNotEquals(base, new ScoreCategory(GameMode.NORMAL, Difficulty.HARD));
    }

    @Test
    void rejectNullMode() {
        // given & when & then
        assertThrows(NullPointerException.class, () -> new ScoreCategory(null, Difficulty.EASY));
    }

    @Test
    void rejectNullDifficulty() {
        // given & when & then
        assertThrows(NullPointerException.class, () -> new ScoreCategory(GameMode.NORMAL, null));
    }

    private ScoreCategory firstCategory() {
        return ScoreCategory.getAllCategories().get(0);
    }

    private ScoreCategory lastCategory() {
        List<ScoreCategory> categories = ScoreCategory.getAllCategories();
        return categories.get(categories.size() - 1);
    }
    // AI-assisted code end
}
