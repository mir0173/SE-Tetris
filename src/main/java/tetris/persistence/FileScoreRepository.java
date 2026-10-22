package tetris.persistence;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import tetris.model.Difficulty;
import tetris.model.GameMode;
import tetris.scoreboard.ScoreEntry;

// AI-assisted code start
public class FileScoreRepository implements ScoreRepository {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final GameMode DEFAULT_MODE = GameMode.NORMAL;
    private static final Difficulty DEFAULT_DIFFICULTY = Difficulty.NORMAL;
    private static final String WRITE_ERROR_MESSAGE = "점수 기록을 저장할 수 없습니다.";

    private final Path path;

    public FileScoreRepository(Path path) {
        this.path = path;
    }

    /**
     * JSON 파일에서 점수 기록을 불러온다.
     * 파일이 없거나 비어 있으면 빈 목록을 반환한다.
     *
     * @return 불러온 점수 기록 목록
     * @throws IOException 파일을 읽을 수 없거나 JSON 형식이 잘못된 경우
     */
    @Override
    public List<ScoreEntry> load() throws IOException {
        if (Files.notExists(path)) {
            return List.of();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            List<ScoreEntry> entries = GSON.fromJson(
                    reader,
                    new TypeToken<List<ScoreEntry>>() {
                    }.getType()
            );
            if (entries == null) {
                return List.of();
            }

            validateEntries(entries);
            return List.copyOf(fillMissingValues(entries));
        } catch (JsonParseException exception) {
            throw new IOException("점수 기록 JSON을 읽을 수 없습니다.", exception);
        }
    }

    /**
     * 점수 기록 목록을 JSON 파일에 저장한다.
     * 기존 파일 내용은 새 목록으로 덮어쓴다.
     *
     * @param entries 저장할 점수 기록 목록
     * @throws IOException 파일 또는 부모 디렉터리를 생성할 수 없는 경우
     */
    @Override
    public void save(List<ScoreEntry> entries) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(entries, writer);
        } catch (JsonIOException exception) {
            throw new IOException(WRITE_ERROR_MESSAGE, exception);
        }
    }

    /**
     * 누락된 게임 모드와 난이도에 기본값을 채운 새 목록을 만든다.
     *
     * @param entries 보정할 점수 기록 목록
     * @return 기본값이 채워진 점수 기록 목록
     */
    private List<ScoreEntry> fillMissingValues(List<ScoreEntry> entries) {
        List<ScoreEntry> filledEntries = new ArrayList<>(entries.size());
        for (ScoreEntry entry : entries) {
            GameMode mode = entry.getMode() == null ? DEFAULT_MODE : entry.getMode();
            Difficulty difficulty = entry.getDifficulty() == null
                    ? DEFAULT_DIFFICULTY
                    : entry.getDifficulty();
            filledEntries.add(new ScoreEntry(
                    entry.getId(),
                    entry.getName(),
                    entry.getScore(),
                    mode,
                    difficulty
            ));
        }
        return filledEntries;
    }

    /**
     * 불러온 점수 기록에 유효하지 않은 항목이 있는지 검사한다
     *
     */
    private void validateEntries(List<ScoreEntry> entries) throws IOException {
        for (ScoreEntry entry : entries) {
            if (entry == null
                    || entry.getId() == null
                    || entry.getName() == null
                    || entry.getName().isBlank()
                    || entry.getScore() < 0) {
                throw new IOException("점수 기록에 유효하지 않은 항목이 있습니다.");
            }
        }
    }
}
// AI-assisted code end