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
        // given: 일반 EASY → NORMAL → HARD, 아이템 EASY → NORMAL → HARD 순서가 조합 순서(저장 순서)다
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
    void moveModeToNextModeAndKeepDifficulty() {
        // given
        ScoreCategory category = new ScoreCategory(GameMode.NORMAL, Difficulty.HARD);

        // when
        ScoreCategory moved = category.moveMode(1);

        // then: 모드만 바뀌고 난이도는 그대로
        assertEquals(new ScoreCategory(GameMode.ITEM, Difficulty.HARD), moved);
    }

    @Test
    void moveModeToPreviousModeAndKeepDifficulty() {
        // given
        ScoreCategory category = new ScoreCategory(GameMode.ITEM, Difficulty.EASY);

        // when
        ScoreCategory moved = category.moveMode(-1);

        // then
        assertEquals(new ScoreCategory(GameMode.NORMAL, Difficulty.EASY), moved);
    }

    @Test
    void wrapToFirstModeWhenMovingModeForwardFromLast() {
        // given: 마지막 모드(아이템)
        ScoreCategory category = new ScoreCategory(GameMode.ITEM, Difficulty.NORMAL);

        // when
        ScoreCategory moved = category.moveMode(1);

        // then: 아이템에서 오른쪽으로 가면 일반
        assertEquals(new ScoreCategory(GameMode.NORMAL, Difficulty.NORMAL), moved);
    }

    @Test
    void wrapToLastModeWhenMovingModeBackFromFirst() {
        // given: 첫 번째 모드(일반)
        ScoreCategory category = new ScoreCategory(GameMode.NORMAL, Difficulty.NORMAL);

        // when
        ScoreCategory moved = category.moveMode(-1);

        // then: 일반에서 왼쪽으로 가면 아이템
        assertEquals(new ScoreCategory(GameMode.ITEM, Difficulty.NORMAL), moved);
    }

    @Test
    void moveDifficultyToNextAndPreviousAndKeepMode() {
        // given
        ScoreCategory easy = new ScoreCategory(GameMode.ITEM, Difficulty.EASY);
        ScoreCategory normal = new ScoreCategory(GameMode.ITEM, Difficulty.NORMAL);
        ScoreCategory hard = new ScoreCategory(GameMode.ITEM, Difficulty.HARD);

        // when & then: EASY → NORMAL → HARD, 거꾸로 HARD → NORMAL, 모드는 항상 그대로
        assertEquals(normal, easy.moveDifficulty(1));
        assertEquals(hard, normal.moveDifficulty(1));
        assertEquals(normal, hard.moveDifficulty(-1));
        assertEquals(easy, normal.moveDifficulty(-1));
    }

    @Test
    void wrapToFirstDifficultyWhenMovingDifficultyForwardFromLast() {
        // given: 마지막 난이도(HARD)
        ScoreCategory category = new ScoreCategory(GameMode.NORMAL, Difficulty.HARD);

        // when
        ScoreCategory moved = category.moveDifficulty(1);

        // then: HARD에서 오른쪽으로 가면 EASY, 모드는 그대로
        assertEquals(new ScoreCategory(GameMode.NORMAL, Difficulty.EASY), moved);
    }

    @Test
    void wrapToLastDifficultyWhenMovingDifficultyBackFromFirst() {
        // given: 첫 번째 난이도(EASY)
        ScoreCategory category = new ScoreCategory(GameMode.ITEM, Difficulty.EASY);

        // when
        ScoreCategory moved = category.moveDifficulty(-1);

        // then: EASY에서 왼쪽으로 가면 HARD, 모드는 그대로
        assertEquals(new ScoreCategory(GameMode.ITEM, Difficulty.HARD), moved);
    }

    @Test
    void keepOtherAxisForEveryCategory() {
        for (ScoreCategory category : ScoreCategory.getAllCategories()) {
            // given & when
            ScoreCategory modeMoved = category.moveMode(1);
            ScoreCategory difficultyMoved = category.moveDifficulty(1);

            // then: 모드를 바꾸면 난이도가, 난이도를 바꾸면 모드가 그대로다
            assertNotEquals(category.mode(), modeMoved.mode());
            assertEquals(category.difficulty(), modeMoved.difficulty());
            assertNotEquals(category.difficulty(), difficultyMoved.difficulty());
            assertEquals(category.mode(), difficultyMoved.mode());
        }
    }

    @Test
    void stayWhenOffsetIsZero() {
        // given
        ScoreCategory category = new ScoreCategory(GameMode.ITEM, Difficulty.NORMAL);

        // when & then
        assertEquals(category, category.moveMode(0));
        assertEquals(category, category.moveDifficulty(0));
    }

    @Test
    void returnToSameCategoryAfterFullLapOfEachAxis() {
        // given
        ScoreCategory category = new ScoreCategory(GameMode.ITEM, Difficulty.NORMAL);
        int modeCount = GameMode.values().length;
        int difficultyCount = Difficulty.values().length;

        // when & then: 한 바퀴를 돌면 제자리, 반대 방향도 같다
        assertEquals(category, category.moveMode(modeCount));
        assertEquals(category, category.moveMode(-modeCount));
        assertEquals(category, category.moveDifficulty(difficultyCount));
        assertEquals(category, category.moveDifficulty(-difficultyCount));
    }

    @Test
    void wrapWhenOffsetIsLargerThanAxisSize() {
        // given
        ScoreCategory category = new ScoreCategory(GameMode.NORMAL, Difficulty.EASY);
        int modeCount = GameMode.values().length;
        int difficultyCount = Difficulty.values().length;

        // when & then: 한 바퀴보다 큰 이동도 같은 결과로 접힌다
        assertEquals(category.moveMode(1), category.moveMode(modeCount + 1));
        assertEquals(category.moveMode(-1), category.moveMode(-(modeCount + 1)));
        assertEquals(category.moveDifficulty(1), category.moveDifficulty(difficultyCount + 1));
        assertEquals(category.moveDifficulty(-1), category.moveDifficulty(-(difficultyCount + 1)));
    }

    @Test
    void visitEveryDifficultyWhenMovingDifficultyForwardRepeatedly() {
        // given
        ScoreCategory current = new ScoreCategory(GameMode.NORMAL, Difficulty.EASY);
        Set<Difficulty> visited = new HashSet<>();

        // when
        for (int i = 0; i < Difficulty.values().length; i++) {
            visited.add(current.difficulty());
            current = current.moveDifficulty(1);
        }

        // then: 난이도를 모두 한 번씩 지나 처음으로 돌아온다
        assertEquals(Difficulty.values().length, visited.size());
        assertEquals(Difficulty.EASY, current.difficulty());
    }

    @Test
    void moveBackAndForthReturnsToOriginalCategoryOnEachAxis() {
        for (ScoreCategory category : ScoreCategory.getAllCategories()) {
            // given & when
            ScoreCategory modeRoundTrip = category.moveMode(1).moveMode(-1);
            ScoreCategory difficultyRoundTrip = category.moveDifficulty(1).moveDifficulty(-1);

            // then
            assertEquals(category, modeRoundTrip);
            assertEquals(category, difficultyRoundTrip);
        }
    }

    @Test
    void moveModeAndDifficultyIndependently() {
        // given
        ScoreCategory category = new ScoreCategory(GameMode.NORMAL, Difficulty.EASY);

        // when: 이동 순서를 바꿔도
        ScoreCategory modeFirst = category.moveMode(1).moveDifficulty(1);
        ScoreCategory difficultyFirst = category.moveDifficulty(1).moveMode(1);

        // then: 결과가 같다
        assertEquals(new ScoreCategory(GameMode.ITEM, Difficulty.NORMAL), modeFirst);
        assertEquals(modeFirst, difficultyFirst);
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

    // AI-assisted code end
}
