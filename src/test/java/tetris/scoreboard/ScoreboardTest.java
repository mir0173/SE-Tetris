package tetris.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ScoreboardTest {

    // AI-assisted code start
    private static final int MAX_ENTRIES = 10;
    private static final int RANK_NOT_FOUND = -1;
    private static final long LOWEST_SCORE = 10L;

    @Test
    void sortEntriesByScoreDescending() {
        // given: 점수가 섞인 순서로 들어온 기록
        List<ScoreEntry> entries = List.of(
                createEntry("low", 100L),
                createEntry("high", 300L),
                createEntry("middle", 200L));

        // when
        Scoreboard scoreboard = new Scoreboard(entries);

        // then
        assertEquals(List.of(300L, 200L, 100L), extractScores(scoreboard.getEntries()));
    }

    @Test
    void returnEmptyEntriesWhenNoEntriesGiven() {
        // given

        // when
        Scoreboard scoreboard = new Scoreboard(List.of());

        // then
        assertTrue(scoreboard.getEntries().isEmpty());
    }

    @Test
    void keepAllEntriesWhenFewerThanMaximum() {
        // given
        List<ScoreEntry> entries = List.of(
                createEntry("first", 100L),
                createEntry("second", 200L),
                createEntry("third", 300L));

        // when
        Scoreboard scoreboard = new Scoreboard(entries);

        // then
        assertEquals(3, scoreboard.getEntries().size());
    }

    @Test
    void keepOnlyTopEntriesWhenMoreThanMaximum() {
        // given: 1점부터 12점까지 오름차순으로 들어온 기록 12개 (정렬이 필요하다)
        List<ScoreEntry> entries = new ArrayList<>();
        for (long score = 1L; score <= MAX_ENTRIES + 2; score++) {
            entries.add(createEntry("player" + score, score));
        }

        // when
        Scoreboard scoreboard = new Scoreboard(entries);

        // then: 가장 낮은 두 기록(1점, 2점)이 잘려 나간다
        assertEquals(MAX_ENTRIES, scoreboard.getEntries().size());
        assertEquals(List.of(12L, 11L, 10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L),
                extractScores(scoreboard.getEntries()));
    }

    @Test
    void keepInputOrderWhenScoresAreTied() {
        // given: 같은 점수 두 개를 점수가 낮은 기록과 섞어서 둔다
        ScoreEntry lowEntry = createEntry("low", 50L);
        ScoreEntry firstTied = createEntry("firstTied", 100L);
        ScoreEntry secondTied = createEntry("secondTied", 100L);

        // when
        Scoreboard scoreboard = new Scoreboard(List.of(lowEntry, firstTied, secondTied));

        // then: 동점이면 먼저 기록한 쪽이 위에 온다
        List<ScoreEntry> result = scoreboard.getEntries();
        assertSame(firstTied, result.get(0));
        assertSame(secondTied, result.get(1));
        assertSame(lowEntry, result.get(2));
    }

    @Test
    void notModifyGivenListWhenCreating() {
        // given
        ScoreEntry first = createEntry("first", 100L);
        ScoreEntry second = createEntry("second", 300L);
        ScoreEntry third = createEntry("third", 200L);
        List<ScoreEntry> entries = new ArrayList<>(List.of(first, second, third));

        // when
        new Scoreboard(entries);

        // then: 정렬은 복사본에서만 일어나고 원본 목록의 순서는 그대로다
        assertEquals(List.of(first, second, third), entries);
    }

    @Test
    void notChangeWhenGivenListIsModifiedLater() {
        // given
        List<ScoreEntry> entries = new ArrayList<>();
        entries.add(createEntry("first", 100L));
        Scoreboard scoreboard = new Scoreboard(entries);

        // when: 스코어보드를 만든 뒤에 원본 목록을 바꾼다
        entries.add(createEntry("late", 999L));

        // then
        assertEquals(1, scoreboard.getEntries().size());
    }

    @Test
    void throwExceptionWhenModifyingReturnedEntries() {
        // given
        Scoreboard scoreboard = new Scoreboard(List.of(createEntry("first", 100L)));
        ScoreEntry extra = createEntry("extra", 200L);

        // when & then
        assertThrows(UnsupportedOperationException.class, () -> scoreboard.getEntries().add(extra));
    }

    @Test
    void rankInWhenBoardIsEmpty() {
        // given
        Scoreboard scoreboard = new Scoreboard(List.of());

        // when & then
        assertTrue(scoreboard.isRankIn(1L));
    }

    @Test
    void rankInWithZeroScoreWhenBoardIsNotFull() {
        // given
        Scoreboard scoreboard = new Scoreboard(List.of(createEntry("first", 100L)));

        // when & then: 0점도 기록할 수 있다
        assertTrue(scoreboard.isRankIn(0L));
    }

    @Test
    void rankInWhenScoreIsHigherThanLowestOfFullBoard() {
        // given: 10개가 가득 찬 스코어보드 (가장 낮은 점수는 10점)
        Scoreboard scoreboard = new Scoreboard(createFullEntries());

        // when & then
        assertTrue(scoreboard.isRankIn(LOWEST_SCORE + 1L));
    }

    @Test
    void notRankInWhenScoreEqualsLowestOfFullBoard() {
        // given
        Scoreboard scoreboard = new Scoreboard(createFullEntries());

        // when & then: 꼴찌와 같은 점수는 순위권이 아니다
        assertFalse(scoreboard.isRankIn(LOWEST_SCORE));
    }

    @Test
    void notRankInWhenScoreIsLowerThanLowestOfFullBoard() {
        // given
        Scoreboard scoreboard = new Scoreboard(createFullEntries());

        // when & then
        assertFalse(scoreboard.isRankIn(LOWEST_SCORE - 1L));
    }

    @Test
    void returnNewScoreboardWhenAddingEntry() {
        // given
        Scoreboard original = new Scoreboard(List.of(createEntry("first", 100L)));

        // when
        Scoreboard added = original.addEntry(createEntry("second", 200L));

        // then: 원본은 바뀌지 않는다
        assertNotSame(original, added);
        assertEquals(1, original.getEntries().size());
        assertEquals(2, added.getEntries().size());
    }

    @Test
    void placeHigherScoreAboveWhenAddingEntry() {
        // given
        ScoreEntry existing = createEntry("existing", 100L);
        ScoreEntry higher = createEntry("higher", 300L);
        Scoreboard scoreboard = new Scoreboard(List.of(existing));

        // when
        Scoreboard added = scoreboard.addEntry(higher);

        // then
        assertEquals(1, added.getRank(higher.getId()));
        assertEquals(2, added.getRank(existing.getId()));
    }

    @Test
    void placeNewEntryBelowExistingWhenScoresAreTied() {
        // given
        ScoreEntry existing = createEntry("existing", 100L);
        ScoreEntry newcomer = createEntry("newcomer", 100L);
        Scoreboard scoreboard = new Scoreboard(List.of(existing));

        // when
        Scoreboard added = scoreboard.addEntry(newcomer);

        // then: 동점이면 먼저 기록한 쪽이 위에 온다
        assertEquals(1, added.getRank(existing.getId()));
        assertEquals(2, added.getRank(newcomer.getId()));
    }

    @Test
    void dropLowestEntryWhenAddingToFullBoard() {
        // given: 10개가 가득 찬 스코어보드 (100, 90, ..., 10점)
        List<ScoreEntry> entries = createFullEntries();
        ScoreEntry lowest = entries.get(MAX_ENTRIES - 1);
        ScoreEntry newcomer = createEntry("newcomer", 55L);
        Scoreboard scoreboard = new Scoreboard(entries);

        // when
        Scoreboard added = scoreboard.addEntry(newcomer);

        // then: 새 기록이 6등으로 들어가고 꼴찌였던 기록은 밀려난다
        assertEquals(MAX_ENTRIES, added.getEntries().size());
        assertEquals(6, added.getRank(newcomer.getId()));
        assertEquals(RANK_NOT_FOUND, added.getRank(lowest.getId()));
    }

    @Test
    void excludeNewEntryWhenScoreIsLowerThanLowestOfFullBoard() {
        // given
        ScoreEntry newcomer = createEntry("newcomer", LOWEST_SCORE - 5L);
        Scoreboard scoreboard = new Scoreboard(createFullEntries());

        // when
        Scoreboard added = scoreboard.addEntry(newcomer);

        // then
        assertEquals(MAX_ENTRIES, added.getEntries().size());
        assertEquals(RANK_NOT_FOUND, added.getRank(newcomer.getId()));
    }

    @Test
    void excludeNewEntryWhenScoreEqualsLowestOfFullBoard() {
        // given: isRankIn 이 false 인 점수와 같은 결과가 나와야 한다
        ScoreEntry newcomer = createEntry("newcomer", LOWEST_SCORE);
        Scoreboard scoreboard = new Scoreboard(createFullEntries());

        // when
        Scoreboard added = scoreboard.addEntry(newcomer);

        // then
        assertEquals(MAX_ENTRIES, added.getEntries().size());
        assertEquals(RANK_NOT_FOUND, added.getRank(newcomer.getId()));
    }

    @Test
    void returnOneBasedRankOfEntry() {
        // given
        ScoreEntry first = createEntry("first", 300L);
        ScoreEntry second = createEntry("second", 200L);
        ScoreEntry third = createEntry("third", 100L);
        Scoreboard scoreboard = new Scoreboard(List.of(third, first, second));

        // when & then: 순위는 1등부터 센다
        assertEquals(1, scoreboard.getRank(first.getId()));
        assertEquals(2, scoreboard.getRank(second.getId()));
        assertEquals(3, scoreboard.getRank(third.getId()));
    }

    @Test
    void returnNotFoundWhenEntryIdDoesNotExist() {
        // given
        Scoreboard scoreboard = new Scoreboard(List.of(createEntry("first", 100L)));

        // when
        int rank = scoreboard.getRank(UUID.randomUUID());

        // then
        assertEquals(RANK_NOT_FOUND, rank);
    }

    @Test
    void returnNotFoundWhenEntryIdIsNull() {
        // given
        Scoreboard scoreboard = new Scoreboard(List.of(createEntry("first", 100L)));

        // when
        int rank = scoreboard.getRank(null);

        // then
        assertEquals(RANK_NOT_FOUND, rank);
    }

    @Test
    void distinguishEntriesWithSameNameAndScoreById() {
        // given: 이름과 점수가 같아도 id 가 다른 두 기록
        ScoreEntry older = createEntry("player", 100L);
        ScoreEntry newer = createEntry("player", 100L);
        Scoreboard scoreboard = new Scoreboard(List.of(older, newer));

        // when & then: id 로 방금 기록을 정확히 찾을 수 있다
        assertEquals(1, scoreboard.getRank(older.getId()));
        assertEquals(2, scoreboard.getRank(newer.getId()));
    }

    private ScoreEntry createEntry(String name, long score) {
        return new ScoreEntry(UUID.randomUUID(), name, score);
    }

    /**
     * 10개가 가득 찬 기록 목록을 만든다. 점수는 100, 90, ..., 10점(내림차순)이다.
     */
    private List<ScoreEntry> createFullEntries() {
        List<ScoreEntry> entries = new ArrayList<>();
        for (int i = 0; i < MAX_ENTRIES; i++) {
            long score = 100L - i * 10L;
            entries.add(createEntry("player" + i, score));
        }
        return entries;
    }

    private List<Long> extractScores(List<ScoreEntry> entries) {
        return entries.stream().map(ScoreEntry::getScore).toList();
    }
    // AI-assisted code end
}
