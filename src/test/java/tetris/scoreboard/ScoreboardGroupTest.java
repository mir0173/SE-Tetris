package tetris.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tetris.model.Difficulty;
import tetris.model.GameMode;

class ScoreboardGroupTest {

    // AI-assisted code start
    private static final int MAX_ENTRIES = 10;
    private static final ScoreCategory NORMAL_EASY = new ScoreCategory(GameMode.NORMAL, Difficulty.EASY);
    private static final ScoreCategory NORMAL_HARD = new ScoreCategory(GameMode.NORMAL, Difficulty.HARD);
    private static final ScoreCategory ITEM_EASY = new ScoreCategory(GameMode.ITEM, Difficulty.EASY);
    private static final ScoreCategory ITEM_HARD = new ScoreCategory(GameMode.ITEM, Difficulty.HARD);

    @Test
    void provideEmptyScoreboardForEveryCategoryWhenNoEntries() {
        // given

        // when
        ScoreboardGroup group = new ScoreboardGroup(List.of());

        // then: 기록이 없어도 6조합 모두 빈 스코어보드가 있다
        for (ScoreCategory category : ScoreCategory.getAllCategories()) {
            assertTrue(group.getScoreboard(category).getEntries().isEmpty());
        }
        assertTrue(group.getAllEntries().isEmpty());
    }

    @Test
    void splitEntriesByCategory() {
        // given
        List<ScoreEntry> entries = List.of(
                createEntry("normalEasy", 100L, NORMAL_EASY),
                createEntry("itemEasy", 200L, ITEM_EASY),
                createEntry("itemHard", 300L, ITEM_HARD));

        // when
        ScoreboardGroup group = new ScoreboardGroup(entries);

        // then: 기록은 자기 조합의 스코어보드에만 들어간다
        assertEquals(List.of("normalEasy"), extractNames(group.getScoreboard(NORMAL_EASY).getEntries()));
        assertEquals(List.of("itemEasy"), extractNames(group.getScoreboard(ITEM_EASY).getEntries()));
        assertEquals(List.of("itemHard"), extractNames(group.getScoreboard(ITEM_HARD).getEntries()));
        assertTrue(group.getScoreboard(NORMAL_HARD).getEntries().isEmpty());
    }

    @Test
    void sortEachCategoryByScoreDescending() {
        // given: 같은 조합의 기록이 섞인 순서로 들어온다
        List<ScoreEntry> entries = List.of(
                createEntry("low", 100L, NORMAL_EASY),
                createEntry("high", 300L, NORMAL_EASY),
                createEntry("middle", 200L, NORMAL_EASY));

        // when
        ScoreboardGroup group = new ScoreboardGroup(entries);

        // then
        assertEquals(List.of("high", "middle", "low"),
                extractNames(group.getScoreboard(NORMAL_EASY).getEntries()));
    }

    @Test
    void keepOtherCategoriesWhenOneCategoryExceedsMaximum() {
        // given: 한 조합에 12개, 다른 조합에 1개
        List<ScoreEntry> entries = new ArrayList<>();
        for (long score = 1L; score <= MAX_ENTRIES + 2; score++) {
            entries.add(createEntry("player" + score, score, NORMAL_EASY));
        }
        entries.add(createEntry("other", 1L, ITEM_HARD));

        // when
        ScoreboardGroup group = new ScoreboardGroup(entries);

        // then: 넘친 조합만 10개로 잘리고 다른 조합의 낮은 점수 기록은 그대로 남는다
        assertEquals(MAX_ENTRIES, group.getScoreboard(NORMAL_EASY).getEntries().size());
        assertEquals(1, group.getScoreboard(ITEM_HARD).getEntries().size());
        assertEquals(MAX_ENTRIES + 1, group.getAllEntries().size());
    }

    @Test
    void keepTopEntriesOfEachCategoryIndependently() {
        // given: 두 조합 모두 12개씩
        List<ScoreEntry> entries = new ArrayList<>();
        for (long score = 1L; score <= MAX_ENTRIES + 2; score++) {
            entries.add(createEntry("a" + score, score, NORMAL_EASY));
            entries.add(createEntry("b" + score, score, ITEM_EASY));
        }

        // when
        ScoreboardGroup group = new ScoreboardGroup(entries);

        // then: 조합마다 10개씩, 모두 20개가 남는다
        assertEquals(MAX_ENTRIES, group.getScoreboard(NORMAL_EASY).getEntries().size());
        assertEquals(MAX_ENTRIES, group.getScoreboard(ITEM_EASY).getEntries().size());
        assertEquals(MAX_ENTRIES * 2, group.getAllEntries().size());
    }

    @Test
    void copyEntriesWhenCreated() {
        // given
        List<ScoreEntry> source = new ArrayList<>(List.of(createEntry("first", 100L, NORMAL_EASY)));
        ScoreboardGroup group = new ScoreboardGroup(source);

        // when: 만든 뒤에 원본 목록을 바꾼다
        source.add(createEntry("second", 200L, NORMAL_EASY));

        // then: 이미 만들어진 그룹은 영향을 받지 않는다
        assertEquals(1, group.getAllEntries().size());
    }

    @Test
    void acceptAnyScoreWhenCategoryHasRoom() {
        // given
        ScoreboardGroup group = new ScoreboardGroup(List.of(createEntry("player", 500L, NORMAL_EASY)));

        // when & then: 10개가 안 찼으면 낮은 점수도, 0점도 순위권이다
        assertTrue(group.isRankIn(NORMAL_EASY, 1L));
        assertTrue(group.isRankIn(NORMAL_EASY, 0L));
    }

    @Test
    void notAffectRankInOfOtherCategoryWhenOneCategoryIsFull() {
        // given: 한 조합만 10개가 가득 찼다 (100, 90, ..., 10점)
        ScoreboardGroup group = new ScoreboardGroup(createFullEntries(NORMAL_EASY));

        // when & then: 가득 찬 조합은 꼴찌 이하 점수가 탈락하고, 다른 조합은 영향이 없다
        assertFalse(group.isRankIn(NORMAL_EASY, 5L));
        assertTrue(group.isRankIn(ITEM_EASY, 5L));
        assertTrue(group.isRankIn(NORMAL_HARD, 0L));
    }

    @Test
    void rejectScoreEqualToLowestWhenCategoryIsFull() {
        // given
        ScoreboardGroup group = new ScoreboardGroup(createFullEntries(NORMAL_EASY));

        // when & then: 꼴찌(10점)와 같은 점수는 순위권이 아니고, 더 높아야 들어간다
        assertFalse(group.isRankIn(NORMAL_EASY, 10L));
        assertTrue(group.isRankIn(NORMAL_EASY, 11L));
    }

    @Test
    void addEntryToItsOwnCategory() {
        // given
        ScoreboardGroup group = new ScoreboardGroup(List.of());
        ScoreEntry entry = createEntry("player", 300L, ITEM_HARD);

        // when
        ScoreboardGroup updated = group.addEntry(entry);

        // then: 기록의 모드와 난이도에 맞는 조합에만 들어간다
        assertEquals(List.of("player"), extractNames(updated.getScoreboard(ITEM_HARD).getEntries()));
        assertEquals(1, updated.getScoreboard(ITEM_HARD).getRank(entry.getId()));
        assertTrue(updated.getScoreboard(NORMAL_EASY).getEntries().isEmpty());
        assertTrue(updated.getScoreboard(ITEM_EASY).getEntries().isEmpty());
        assertEquals(1, updated.getAllEntries().size());
    }

    @Test
    void notChangeOriginalGroupWhenAddingEntry() {
        // given
        ScoreboardGroup group = new ScoreboardGroup(List.of(createEntry("existing", 100L, NORMAL_EASY)));

        // when
        group.addEntry(createEntry("added", 200L, NORMAL_EASY));

        // then: 원본은 그대로다
        assertEquals(List.of("existing"), extractNames(group.getAllEntries()));
    }

    @Test
    void keepExistingEntryFirstWhenAddedScoreIsTied() {
        // given
        ScoreboardGroup group = new ScoreboardGroup(List.of(createEntry("old", 100L, NORMAL_EASY)));

        // when
        ScoreboardGroup updated = group.addEntry(createEntry("new", 100L, NORMAL_EASY));

        // then: 동점이면 먼저 기록한 쪽이 위
        assertEquals(List.of("old", "new"), extractNames(updated.getScoreboard(NORMAL_EASY).getEntries()));
    }

    @Test
    void dropLowestEntryOnlyInItsCategoryWhenAddingToFullCategory() {
        // given: 한 조합은 가득 찼고(100, 90, ..., 10점) 다른 조합에도 기록이 있다
        List<ScoreEntry> entries = createFullEntries(NORMAL_EASY);
        entries.add(createEntry("other", 5L, ITEM_EASY));
        ScoreboardGroup group = new ScoreboardGroup(entries);

        // when: 가득 찬 조합에 55점을 추가한다
        ScoreboardGroup updated = group.addEntry(createEntry("added", 55L, NORMAL_EASY));

        // then: 그 조합의 꼴찌(10점)만 밀려나고 다른 조합은 그대로다
        assertEquals(List.of(100L, 90L, 80L, 70L, 60L, 55L, 50L, 40L, 30L, 20L),
                extractScores(updated.getScoreboard(NORMAL_EASY).getEntries()));
        assertEquals(List.of("other"), extractNames(updated.getScoreboard(ITEM_EASY).getEntries()));
    }

    @Test
    void listAllEntriesInCategoryOrderThenRank() {
        // given: 조합 순서와 점수 순서가 모두 섞인 입력
        List<ScoreEntry> entries = List.of(
                createEntry("itemHard", 50L, ITEM_HARD),
                createEntry("normalEasyLow", 10L, NORMAL_EASY),
                createEntry("normalEasyHigh", 90L, NORMAL_EASY),
                createEntry("itemEasy", 70L, ITEM_EASY),
                createEntry("normalHard", 30L, NORMAL_HARD));

        // when
        ScoreboardGroup group = new ScoreboardGroup(entries);

        // then: 조합 순서(일반 EASY → … → 아이템 HARD)대로, 각 조합 안에서는 순위대로
        assertEquals(List.of("normalEasyHigh", "normalEasyLow", "normalHard", "itemEasy", "itemHard"),
                extractNames(group.getAllEntries()));
    }

    @Test
    void keepTieOrderWhenRebuiltFromAllEntries() {
        // given: 같은 점수의 기록이 여러 개 있다
        ScoreboardGroup group = new ScoreboardGroup(List.of(
                createEntry("first", 100L, NORMAL_EASY),
                createEntry("second", 100L, NORMAL_EASY),
                createEntry("third", 100L, NORMAL_EASY),
                createEntry("other", 100L, ITEM_EASY)));

        // when: 저장했다가 다시 읽은 것처럼 getAllEntries 결과로 새로 만든다
        ScoreboardGroup reloaded = new ScoreboardGroup(group.getAllEntries());

        // then: 동점 순서까지 그대로 복원된다
        assertEquals(List.of("first", "second", "third", "other"), extractNames(group.getAllEntries()));
        assertEquals(extractNames(group.getAllEntries()), extractNames(reloaded.getAllEntries()));
    }

    @Test
    void rejectModificationOfAllEntries() {
        // given
        ScoreboardGroup group = new ScoreboardGroup(List.of(createEntry("player", 100L, NORMAL_EASY)));
        List<ScoreEntry> allEntries = group.getAllEntries();

        // when & then: 꺼낸 목록을 바꿔서 그룹을 망가뜨릴 수 없다
        assertThrows(UnsupportedOperationException.class,
                () -> allEntries.add(createEntry("hacker", 999L, NORMAL_EASY)));
    }

    private ScoreEntry createEntry(String name, long score, ScoreCategory category) {
        return new ScoreEntry(UUID.randomUUID(), name, score, category.mode(), category.difficulty());
    }

    /**
     * 10개가 가득 찬 한 조합의 기록 목록을 만든다. 점수는 100, 90, ..., 10점(내림차순)이다.
     */
    private List<ScoreEntry> createFullEntries(ScoreCategory category) {
        List<ScoreEntry> entries = new ArrayList<>();
        for (int i = 0; i < MAX_ENTRIES; i++) {
            long score = 100L - i * 10L;
            entries.add(createEntry("player" + i, score, category));
        }
        return entries;
    }

    private List<String> extractNames(List<ScoreEntry> entries) {
        return entries.stream().map(ScoreEntry::getName).toList();
    }

    private List<Long> extractScores(List<ScoreEntry> entries) {
        return entries.stream().map(ScoreEntry::getScore).toList();
    }
    // AI-assisted code end
}
