package tetris.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tetris.scoreboard.ScoreEntry;

class FileScoreRepositoryTest {

    // AI-assisted code start
    private static final String SCORE_FILE_NAME = "scores.json";
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

    private ScoreEntry createEntry(String name, long score) {
        return new ScoreEntry(UUID.randomUUID(), name, score);
    }

    /**
     * ScoreEntry에 equals가 없으므로 id, 이름, 점수를 각각 비교한다.
     */
    private void assertSameEntry(ScoreEntry expected, ScoreEntry actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getScore(), actual.getScore());
    }
    // AI-assisted code end
}
