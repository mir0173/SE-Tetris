package tetris.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tetris.model.Difficulty;
import tetris.model.GameMode;
import tetris.scoreboard.ScoreEntry;

class FileScoreRepositoryTest {

    // AI-assisted code start
    private static final String SCORE_FILE_NAME = "scores.json";
    private static final String ENTRY_ID = "00000000-0000-0000-0000-000000000001";
    private static final String ENTRY_NAME = "player";
    private static final long ENTRY_SCORE = 100L;
    private static final String NESTED_DIRECTORY_NAME = "nested";
    private static final String KOREAN_NAME = "홍길동";
    private static final String EMPTY_CONTENT = "";
    private static final String CORRUPTED_CONTENT = "[{\"name\": \"alice\"";

    @TempDir
    Path tempDir;

    private Path scoreFile;
    private FileScoreRepository repository;

    @BeforeEach
    void setUp() {
        scoreFile = tempDir.resolve(SCORE_FILE_NAME);
        repository = new FileScoreRepository(scoreFile);
    }

    @Test
    void returnEmptyListWhenFileDoesNotExist() throws IOException {
        // given: 저장 파일이 없는 상태

        // when
        List<ScoreEntry> loaded = repository.load();

        // then
        assertTrue(loaded.isEmpty());
    }

    @Test
    void returnEmptyListWhenFileIsEmpty() throws IOException {
        // given
        Files.writeString(scoreFile, EMPTY_CONTENT);

        // when
        List<ScoreEntry> loaded = repository.load();

        // then
        assertTrue(loaded.isEmpty());
    }

    @Test
    void loadSavedEntriesInSameOrder() throws IOException {
        // given: 점수가 낮은 기록을 뒤에 둬서 저장소가 정렬하지 않는지도 확인한다
        ScoreEntry first = createEntry("alice", 300L);
        ScoreEntry second = createEntry("bob", 100L);

        // when
        repository.save(List.of(first, second));
        List<ScoreEntry> loaded = repository.load();

        // then
        assertEquals(2, loaded.size());
        assertSameEntry(first, loaded.get(0));
        assertSameEntry(second, loaded.get(1));
    }

    @Test
    void loadEntriesSavedByPreviousInstance() throws IOException {
        // given
        ScoreEntry entry = createEntry("alice", 300L);
        repository.save(List.of(entry));

        // when: 프로그램을 다시 실행한 것처럼 새 저장소 객체로 불러온다
        FileScoreRepository restartedRepository = new FileScoreRepository(scoreFile);
        List<ScoreEntry> loaded = restartedRepository.load();

        // then
        assertEquals(1, loaded.size());
        assertSameEntry(entry, loaded.get(0));
    }

    @Test
    void preserveKoreanName() throws IOException {
        // given
        ScoreEntry entry = createEntry(KOREAN_NAME, 500L);

        // when
        repository.save(List.of(entry));
        List<ScoreEntry> loaded = repository.load();

        // then
        assertEquals(1, loaded.size());
        assertEquals(KOREAN_NAME, loaded.get(0).getName());
    }

    @Test
    void replaceEntriesWhenSavingAgain() throws IOException {
        // given
        ScoreEntry oldEntry = createEntry("alice", 300L);
        ScoreEntry newEntry = createEntry("bob", 100L);
        repository.save(List.of(oldEntry));

        // when
        repository.save(List.of(newEntry));
        List<ScoreEntry> loaded = repository.load();

        // then
        assertEquals(1, loaded.size());
        assertSameEntry(newEntry, loaded.get(0));
    }

    @Test
    void clearEntriesWhenSavingEmptyList() throws IOException {
        // given
        repository.save(List.of(createEntry("alice", 300L)));

        // when
        repository.save(List.of());
        List<ScoreEntry> loaded = repository.load();

        // then
        assertTrue(loaded.isEmpty());
    }

    @Test
    void createDirectoryWhenMissing() throws IOException {
        // given: 아직 만들어지지 않은 폴더 안의 파일 경로
        Path nestedFile = tempDir.resolve(NESTED_DIRECTORY_NAME).resolve(SCORE_FILE_NAME);
        FileScoreRepository nestedRepository = new FileScoreRepository(nestedFile);
        ScoreEntry entry = createEntry("alice", 300L);

        // when
        nestedRepository.save(List.of(entry));
        List<ScoreEntry> loaded = nestedRepository.load();

        // then
        assertTrue(Files.exists(nestedFile));
        assertEquals(1, loaded.size());
        assertSameEntry(entry, loaded.get(0));
    }

    @Test
    void throwIOExceptionWhenFileIsCorrupted() throws IOException {
        // given: 저장 도중 끊긴 것처럼 JSON이 완성되지 않은 파일
        Files.writeString(scoreFile, CORRUPTED_CONTENT);

        // when & then
        assertThrows(IOException.class, () -> repository.load());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[null]",
            "[{\"name\":\"player\",\"score\":100}]",
            "[{\"id\":\"00000000-0000-0000-0000-000000000001\",\"score\":100}]",
            "[{\"id\":\"00000000-0000-0000-0000-000000000001\",\"name\":\" \",\"score\":100}]",
            "[{\"id\":\"00000000-0000-0000-0000-000000000001\",\"name\":\"player\",\"score\":-1}]"
    })
    void rejectInvalidScoreEntries(String json) throws IOException {
        Files.writeString(scoreFile, json);

        assertThrows(IOException.class, repository::load);
    }

    @Test
    void preserveModeAndDifficultyForEveryCombination() throws IOException {
        // given: 6가지 조합마다 기록 하나씩
        List<ScoreEntry> entries = new ArrayList<>();
        for (GameMode mode : GameMode.values()) {
            for (Difficulty difficulty : Difficulty.values()) {
                entries.add(createEntry("player", 100L, mode, difficulty));
            }
        }

        // when
        repository.save(entries);
        List<ScoreEntry> loaded = repository.load();

        // then
        assertEquals(entries.size(), loaded.size());
        for (int i = 0; i < entries.size(); i++) {
            assertSameEntry(entries.get(i), loaded.get(i));
        }
    }

    @Test
    void writeModeAndDifficultyAsEnumNames() throws IOException {
        // given
        ScoreEntry entry = createEntry("alice", 300L, GameMode.ITEM, Difficulty.HARD);

        // when
        repository.save(List.of(entry));
        String content = Files.readString(scoreFile);

        // then: 파일에는 enum 이름이 그대로 적힌다
        assertTrue(content.contains("\"mode\": \"ITEM\""));
        assertTrue(content.contains("\"difficulty\": \"HARD\""));
    }

    @Test
    void loadLegacyEntryAsNormalModeAndNormalDifficulty() throws IOException {
        // given: 모드와 난이도 필드가 없는 이전 버전의 기록
        Files.writeString(scoreFile, createJson(""));

        // when
        List<ScoreEntry> loaded = repository.load();

        // then: 일반 + NORMAL로 보정되고 나머지 값은 그대로다
        assertEquals(1, loaded.size());
        assertEquals(UUID.fromString(ENTRY_ID), loaded.get(0).getId());
        assertEquals(ENTRY_NAME, loaded.get(0).getName());
        assertEquals(ENTRY_SCORE, loaded.get(0).getScore());
        assertEquals(GameMode.NORMAL, loaded.get(0).getMode());
        assertEquals(Difficulty.NORMAL, loaded.get(0).getDifficulty());
    }

    @Test
    void fillOnlyMissingModeWhenDifficultyExists() throws IOException {
        // given: 난이도만 있는 기록
        Files.writeString(scoreFile, createJson(",\"difficulty\":\"HARD\""));

        // when
        List<ScoreEntry> loaded = repository.load();

        // then: 없는 모드만 채우고 있는 난이도는 지킨다
        assertEquals(GameMode.NORMAL, loaded.get(0).getMode());
        assertEquals(Difficulty.HARD, loaded.get(0).getDifficulty());
    }

    @Test
    void fillOnlyMissingDifficultyWhenModeExists() throws IOException {
        // given: 모드만 있는 기록
        Files.writeString(scoreFile, createJson(",\"mode\":\"ITEM\""));

        // when
        List<ScoreEntry> loaded = repository.load();

        // then: 없는 난이도만 채우고 있는 모드는 지킨다
        assertEquals(GameMode.ITEM, loaded.get(0).getMode());
        assertEquals(Difficulty.NORMAL, loaded.get(0).getDifficulty());
    }

    @Test
    void replaceUnknownModeWithNormal() throws IOException {
        // given: 알 수 없는 모드 이름 (직접 고친 파일, 소문자 등)
        Files.writeString(scoreFile, createJson(",\"mode\":\"EXTREME\",\"difficulty\":\"EASY\""));

        // when
        List<ScoreEntry> loaded = repository.load();

        // then: 기록을 버리지 않고 일반 모드로 보정하며, 유효한 난이도는 지킨다
        assertEquals(1, loaded.size());
        assertEquals(GameMode.NORMAL, loaded.get(0).getMode());
        assertEquals(Difficulty.EASY, loaded.get(0).getDifficulty());
    }

    @Test
    void replaceUnknownDifficultyWithNormal() throws IOException {
        // given
        Files.writeString(scoreFile, createJson(",\"mode\":\"ITEM\",\"difficulty\":\"extreme\""));

        // when
        List<ScoreEntry> loaded = repository.load();

        // then
        assertEquals(1, loaded.size());
        assertEquals(GameMode.ITEM, loaded.get(0).getMode());
        assertEquals(Difficulty.NORMAL, loaded.get(0).getDifficulty());
    }

    @Test
    void replaceNullModeAndDifficultyWithNormal() throws IOException {
        // given: 값이 명시적으로 null인 기록
        Files.writeString(scoreFile, createJson(",\"mode\":null,\"difficulty\":null"));

        // when
        List<ScoreEntry> loaded = repository.load();

        // then
        assertEquals(GameMode.NORMAL, loaded.get(0).getMode());
        assertEquals(Difficulty.NORMAL, loaded.get(0).getDifficulty());
    }

    @Test
    void keepNewEntryValuesWhenLoadedWithLegacyEntry() throws IOException {
        // given: 이전 버전 기록과 새 기록이 한 파일에 섞여 있다
        String json = "[{\"id\":\"" + ENTRY_ID + "\",\"name\":\"old\",\"score\":50},"
                + "{\"id\":\"00000000-0000-0000-0000-000000000002\",\"name\":\"new\",\"score\":200,"
                + "\"mode\":\"ITEM\",\"difficulty\":\"HARD\"}]";
        Files.writeString(scoreFile, json);

        // when
        List<ScoreEntry> loaded = repository.load();

        // then: 순서가 유지되고 새 기록의 값은 바뀌지 않는다
        assertEquals(2, loaded.size());
        assertEquals("old", loaded.get(0).getName());
        assertEquals(GameMode.NORMAL, loaded.get(0).getMode());
        assertEquals(Difficulty.NORMAL, loaded.get(0).getDifficulty());
        assertEquals("new", loaded.get(1).getName());
        assertEquals(GameMode.ITEM, loaded.get(1).getMode());
        assertEquals(Difficulty.HARD, loaded.get(1).getDifficulty());
    }

    @Test
    void writeDefaultsWhenSavingLoadedLegacyEntry() throws IOException {
        // given: 이전 버전 파일을 불러왔다
        Files.writeString(scoreFile, createJson(""));
        List<ScoreEntry> loaded = repository.load();

        // when: 그대로 다시 저장한다
        repository.save(loaded);
        String content = Files.readString(scoreFile);

        // then: 보정된 값이 파일에 적혀서 다음부터는 그대로 읽힌다
        assertTrue(content.contains("\"mode\": \"NORMAL\""));
        assertTrue(content.contains("\"difficulty\": \"NORMAL\""));
    }

    @Test
    void rejectLegacyEntryWithoutNameEvenIfModeIsMissing() throws IOException {
        // given: 모드가 없을 뿐 아니라 이름도 없는 잘못된 기록
        Files.writeString(scoreFile, "[{\"id\":\"" + ENTRY_ID + "\",\"score\":100}]");

        // when & then: 보정과 상관없이 기존 검증은 그대로 거부한다
        assertThrows(IOException.class, repository::load);
    }

    private ScoreEntry createEntry(String name, long score) {
        return createEntry(name, score, GameMode.NORMAL, Difficulty.NORMAL);
    }

    private ScoreEntry createEntry(String name, long score, GameMode mode, Difficulty difficulty) {
        return new ScoreEntry(UUID.randomUUID(), name, score, mode, difficulty);
    }

    /**
     * 기록 하나가 든 JSON을 만든다. extraFields에는 "," 로 시작하는 추가 필드를 넣는다.
     */
    private String createJson(String extraFields) {
        return "[{\"id\":\"" + ENTRY_ID + "\",\"name\":\"" + ENTRY_NAME
                + "\",\"score\":" + ENTRY_SCORE + extraFields + "}]";
    }

    /**
     * ScoreEntry에 equals가 없으므로 id, 이름, 점수, 모드, 난이도를 각각 비교한다.
     */
    private void assertSameEntry(ScoreEntry expected, ScoreEntry actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getScore(), actual.getScore());
        assertEquals(expected.getMode(), actual.getMode());
        assertEquals(expected.getDifficulty(), actual.getDifficulty());
    }
    // AI-assisted code end
}
