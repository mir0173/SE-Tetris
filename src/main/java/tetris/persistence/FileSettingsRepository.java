package tetris.persistence;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;

import javafx.scene.input.KeyCode;

import tetris.settings.GameSettings;
import tetris.settings.WindowSize;
import tetris.model.GameCommand;

public class FileSettingsRepository implements SettingsRepository {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    // AI-assisted code start
    private static final String WRITE_ERROR_MESSAGE = "설정을 저장할 수 없습니다.";
    // AI-assisted code end

    // AI-assisted code start
    private static final String READ_ERROR_MESSAGE = "설정 JSON을 읽을 수 없습니다.";
    // AI-assisted code end

    private final Path path;

    public FileSettingsRepository(Path path) {
        this.path = path;
    }

    // AI-assisted code start
    /**
     * JSON 파일에서 설정을 불러온다.
     * 파일이 없거나 비어 있으면 기본 설정을 반환한다.
     *
     * @return 불러온 설정
     * @throws IOException 파일을 읽을 수 없거나 JSON 형식이 잘못된 경우
     */
    @Override
    public GameSettings load() throws IOException {
        if (Files.notExists(path)) {
            return GameSettings.defaults();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            SettingsData data = GSON.fromJson(reader, SettingsData.class);
            return data == null ? GameSettings.defaults() : createSettings(data);
        } catch (JsonParseException exception) {
            throw new IOException(READ_ERROR_MESSAGE, exception);
        }
    }
    // AI-assisted code end

    // AI-assisted code start
    @Override
    public void save(GameSettings settings) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(createData(settings), writer);
        } catch (JsonIOException exception) {
            throw new IOException(WRITE_ERROR_MESSAGE, exception);
        }
    }
    // AI-assisted code end

    // AI-assisted code start
    private SettingsData createData(GameSettings settings) {
        Map<String, String> keyBindings = new HashMap<>();
        for (Map.Entry<GameCommand, String> entry : settings.getKeyBindings().entrySet()) {
            keyBindings.put(entry.getKey().name(), entry.getValue());
        }

        return new SettingsData(
                settings.getWindowSize(),
                keyBindings,
                settings.isColorBlindMode()
        );
    }
    // AI-assisted code end

    // AI-assisted code start
    private GameSettings createSettings(SettingsData data) {
        return new GameSettings(
                normalizeWindowSize(data.windowSize),
                normalizeKeyBindings(data.keyBindings),
                data.colorBlindMode
        );
    }
    // AI-assisted code end

    // AI-assisted code start
    private WindowSize normalizeWindowSize(WindowSize windowSize) {
        return windowSize == null ? GameSettings.getDefaultWindowSize() : windowSize;
    }
    // AI-assisted code end

    // AI-assisted code start
    /**
     * 저장된 키 설정을 기본 키 설정과 병합하고 유효하지 않은 값을 보정한다.
     *
     * @param keyBindings 저장된 키 설정
     * @return 기본값으로 보정된 키 설정
     */
    private Map<GameCommand, String> normalizeKeyBindings(Map<String, String> keyBindings) {
        Map<GameCommand, String> normalizedKeyBindings = new EnumMap<>(GameCommand.class);
        normalizedKeyBindings.putAll(GameSettings.getDefaultKeyBindings());

        if (keyBindings == null) {
            return normalizedKeyBindings;
        }

        for (Map.Entry<String, String> entry : keyBindings.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }

            GameCommand command;
            try {
                command = GameCommand.valueOf(entry.getKey());
            } catch (IllegalArgumentException exception) {
                continue;
            }

            String keyName = entry.getValue();
            if (command != null
                    && command != GameCommand.NONE
                    && isValidKeyName(keyName)) {
                normalizedKeyBindings.put(command, keyName);
            }
        }

        return normalizedKeyBindings;
    }
    // AI-assisted code end

    // AI-assisted code start
    private boolean isValidKeyName(String keyName) {
        if (keyName == null) {
            return false;
        }

        try {
            KeyCode.valueOf(keyName);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
    // AI-assisted code end

    private static class SettingsData {
        private final WindowSize windowSize;
        private final Map<String, String> keyBindings;
        private final boolean colorBlindMode;

        private SettingsData(WindowSize windowSize, Map<String, String> keyBindings, boolean colorBlindMode) {
            this.windowSize = windowSize;
            this.keyBindings = keyBindings;
            this.colorBlindMode = colorBlindMode;
        }
    }

}
