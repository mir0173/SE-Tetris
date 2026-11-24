package tetris.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import tetris.model.Difficulty;
import tetris.model.GameCommand;
import tetris.settings.BattleKeyBindings;
import tetris.settings.GameSettings;
import tetris.settings.WindowSize;

class FileSettingsRepositoryTest {

    // AI-assisted code start
    private static final String SETTINGS_FILE_NAME = "settings.json";
    private static final String NESTED_DIRECTORY_NAME = "nested";
    private static final String EMPTY_CONTENT = "";
    private static final String CORRUPTED_CONTENT = "{\"windowSize\": \"LARGE\"";
    private static final String CUSTOM_LEFT_KEY = "A";
    private static final String CUSTOM_RIGHT_KEY = "D";
    private static final String INVALID_KEY_NAME = "LFET";

    @TempDir
    Path tempDir;

    private Path settingsFile;
    private FileSettingsRepository repository;

    @BeforeEach
    void setUp() {
        settingsFile = tempDir.resolve(SETTINGS_FILE_NAME);
        repository = new FileSettingsRepository(settingsFile);
    }

    @Test
    void returnDefaultSettingsWhenFileDoesNotExist() throws IOException {
        // given: 저장 파일이 없는 상태 (첫 실행)

        // when
        GameSettings loaded = repository.load();

        // then
        assertSameAsDefaults(loaded);
    }

    @Test
    void returnDefaultSettingsWhenFileIsEmpty() throws IOException {
        // given
        Files.writeString(settingsFile, EMPTY_CONTENT);

        // when
        GameSettings loaded = repository.load();

        // then
        assertSameAsDefaults(loaded);
    }

    @Test
    void loadSavedSettings() throws IOException {
        // given: 기본값과 다른 값만 골라서 저장한다
        GameSettings customSettings = createCustomSettings();

        // when
        repository.save(customSettings);
        GameSettings loaded = repository.load();

        // then
        assertSameSettings(customSettings, loaded);
    }

    @Test
    void loadSettingsSavedByPreviousInstance() throws IOException {
        // given
        GameSettings customSettings = createCustomSettings();
        repository.save(customSettings);

        // when: 프로그램을 다시 실행한 것처럼 새 저장소 객체로 불러온다
        FileSettingsRepository restartedRepository = new FileSettingsRepository(settingsFile);
        GameSettings loaded = restartedRepository.load();

        // then
        assertSameSettings(customSettings, loaded);
    }

    @Test
    void replaceSettingsWhenSavingAgain() throws IOException {
        // given
        repository.save(createCustomSettings());

        // when: 기본값으로 되돌리는 상황
        repository.save(GameSettings.defaults());
        GameSettings loaded = repository.load();

        // then
        assertSameAsDefaults(loaded);
    }

    @Test
    void createDirectoryWhenMissing() throws IOException {
        // given: 아직 만들어지지 않은 폴더 안의 파일 경로
        Path nestedFile = tempDir.resolve(NESTED_DIRECTORY_NAME).resolve(SETTINGS_FILE_NAME);
        FileSettingsRepository nestedRepository = new FileSettingsRepository(nestedFile);
        GameSettings customSettings = createCustomSettings();

        // when
        nestedRepository.save(customSettings);
        GameSettings loaded = nestedRepository.load();

        // then
        assertTrue(Files.exists(nestedFile));
        assertSameSettings(customSettings, loaded);
    }

    @Test
    void throwIOExceptionWhenFileIsCorrupted() throws IOException {
        // given: 저장 도중 끊긴 것처럼 JSON이 완성되지 않은 파일
        Files.writeString(settingsFile, CORRUPTED_CONTENT);

        // when & then
        assertThrows(IOException.class, () -> repository.load());
    }

    @Test
    void useDefaultKeyBindingsWhenKeyBindingsAreMissing() throws IOException {
        // given: keyBindings 항목이 아예 없는 파일
        Files.writeString(settingsFile, """
                {
                  "windowSize": "LARGE",
                  "colorBlindMode": true
                }
                """);

        // when
        GameSettings loaded = repository.load();

        // then: 있는 값은 유지하고, 없는 키 설정만 기본값으로 채운다
        assertEquals(WindowSize.LARGE, loaded.getWindowSize());
        assertTrue(loaded.isColorBlindMode());
        assertEquals(GameSettings.getDefaultKeyBindings(), loaded.getKeyBindings());
    }

    @Test
    void useDefaultWindowSizeWhenWindowSizeIsMissing() throws IOException {
        // given
        Files.writeString(settingsFile, """
                {
                  "colorBlindMode": true
                }
                """);

        // when
        GameSettings loaded = repository.load();

        // then
        assertEquals(GameSettings.getDefaultWindowSize(), loaded.getWindowSize());
        assertTrue(loaded.isColorBlindMode());
    }

    @Test
    void useDefaultWindowSizeWhenWindowSizeIsUnknown() throws IOException {
        // given: 존재하지 않는 화면 크기 이름
        Files.writeString(settingsFile, """
                {
                  "windowSize": "HUGE"
                }
                """);

        // when
        GameSettings loaded = repository.load();

        // then
        assertEquals(GameSettings.getDefaultWindowSize(), loaded.getWindowSize());
    }

    @Test
    void fillMissingCommandsWithDefaultKeys() throws IOException {
        // given: 명령 하나만 저장된 옛날 설정 파일
        Files.writeString(settingsFile, """
                {
                  "keyBindings": {
                    "MOVE_LEFT": "A"
                  }
                }
                """);

        // when
        GameSettings loaded = repository.load();

        // then: 저장된 명령은 유지하고, 나머지 명령은 기본 키로 채운다
        Map<GameCommand, String> keyBindings = loaded.getKeyBindings();
        assertEquals(CUSTOM_LEFT_KEY, keyBindings.get(GameCommand.MOVE_LEFT));
        assertEveryDefaultKeyExceptFor(keyBindings, GameCommand.MOVE_LEFT);
    }

    @Test
    void replaceInvalidKeyNameWithDefaultKey() throws IOException {
        // given: 오타가 난 키 이름과 올바른 키 이름이 섞인 파일
        Files.writeString(settingsFile, """
                {
                  "keyBindings": {
                    "MOVE_LEFT": "LFET",
                    "MOVE_RIGHT": "D"
                  }
                }
                """);

        // when
        GameSettings loaded = repository.load();

        // then: 잘못된 명령만 기본 키로 대체하고, 올바른 명령은 유지한다
        Map<GameCommand, String> keyBindings = loaded.getKeyBindings();
        assertEquals(GameSettings.getDefaultKeyBindings().get(GameCommand.MOVE_LEFT),
                keyBindings.get(GameCommand.MOVE_LEFT));
        assertEquals(CUSTOM_RIGHT_KEY, keyBindings.get(GameCommand.MOVE_RIGHT));
    }

    @Test
    void replaceNullKeyNameWithDefaultKey() throws IOException {
        // given: 값이 null 인 키 설정
        Files.writeString(settingsFile, """
                {
                  "keyBindings": {
                    "MOVE_LEFT": null
                  }
                }
                """);

        // when
        GameSettings loaded = repository.load();

        // then
        assertEquals(GameSettings.getDefaultKeyBindings(), loaded.getKeyBindings());
    }

    @Test
    void ignoreUnknownCommandName() throws IOException {
        // given: GameCommand 에 없는 명령 이름 하나
        Files.writeString(settingsFile, """
                {
                  "keyBindings": {
                    "MOVE_LEFT": "A",
                    "REMOVED_COMMAND": "X"
                  }
                }
                """);

        // when
        GameSettings loaded = repository.load();

        // then
        Map<GameCommand, String> keyBindings = loaded.getKeyBindings();
        assertEquals(CUSTOM_LEFT_KEY, keyBindings.get(GameCommand.MOVE_LEFT));
        assertEveryDefaultKeyExceptFor(keyBindings, GameCommand.MOVE_LEFT);
    }

    @Test
    void ignoreMultipleUnknownCommandNames() throws IOException {
        // given: GameCommand 에 없는 명령 이름 여러 개 (명령이 삭제·변경된 경우)
        Files.writeString(settingsFile, """
                {
                  "keyBindings": {
                    "MOVE_LEFT": "A",
                    "REMOVED_COMMAND": "X",
                    "OLD_COMMAND": "Y"
                  }
                }
                """);

        // when
        GameSettings loaded = repository.load();

        // then
        Map<GameCommand, String> keyBindings = loaded.getKeyBindings();
        assertEquals(CUSTOM_LEFT_KEY, keyBindings.get(GameCommand.MOVE_LEFT));
        assertEveryDefaultKeyExceptFor(keyBindings, GameCommand.MOVE_LEFT);
    }

    @Test
    void ignoreKeyBindingOfNoneCommand() throws IOException {
        // given: 키가 필요 없는 NONE 명령에 키가 저장된 파일
        Files.writeString(settingsFile, """
                {
                  "keyBindings": {
                    "NONE": "A"
                  }
                }
                """);

        // when
        GameSettings loaded = repository.load();

        // then
        assertFalse(loaded.getKeyBindings().containsKey(GameCommand.NONE));
        assertEquals(GameSettings.getDefaultKeyBindings(), loaded.getKeyBindings());
    }

    @Test
    void useDefaultKeyWhenSavedKeyNameIsInvalid() throws IOException {
        // given: 저장소 밖에서 잘못된 키 이름을 가진 설정이 만들어진 경우
        Map<GameCommand, String> keyBindings = new EnumMap<>(GameCommand.class);
        keyBindings.putAll(GameSettings.getDefaultKeyBindings());
        keyBindings.put(GameCommand.HOLD, INVALID_KEY_NAME);
        repository.save(new GameSettings(WindowSize.SMALL, keyBindings, false, Difficulty.NORMAL));

        // when
        GameSettings loaded = repository.load();

        // then: 불러올 때 잘못된 키는 기본 키로 대체된다
        assertEquals(WindowSize.SMALL, loaded.getWindowSize());
        assertEquals(GameSettings.getDefaultKeyBindings(), loaded.getKeyBindings());
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void saveAndReloadEveryDifficulty(Difficulty difficulty)
            throws IOException {
        repository.save(new GameSettings(
                WindowSize.LARGE,
                GameSettings.getDefaultKeyBindings(),
                true,
                difficulty));

        GameSettings loaded =
                new FileSettingsRepository(settingsFile).load();

        assertEquals(difficulty, loaded.getDifficulty());
        assertEquals(WindowSize.LARGE, loaded.getWindowSize());
        assertTrue(loaded.isColorBlindMode());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"difficulty\":null}",
        "{\"difficulty\":\"UNKNOWN\"}"
    })
    void useNormalForMissingOrUnknownDifficulty(String json)
            throws IOException {
        Files.writeString(settingsFile, json);

        assertEquals(
                Difficulty.NORMAL,
                repository.load().getDifficulty());
    }

    @Test
    void preserveOldSettingsWhenAddingDefaultDifficulty()
            throws IOException {
        Files.writeString(settingsFile, """
                {
                "windowSize": "LARGE",
                "colorBlindMode": true,
                "keyBindings": {"MOVE_LEFT": "A"}
                }
                """);

        GameSettings loaded = repository.load();

        assertEquals(Difficulty.NORMAL, loaded.getDifficulty());
        assertEquals(WindowSize.LARGE, loaded.getWindowSize());
        assertTrue(loaded.isColorBlindMode());
        assertEquals(
                "A",
                loaded.getKeyBindings().get(GameCommand.MOVE_LEFT));
    }

    // ---------- 대전 키 ----------

    @Test
    void saveAndLoadBattleKeyBindings() throws IOException {
        // given: 1P 왼쪽 이동, 2P 홀드, 공용 일시정지를 바꾼 설정
        BattleKeyBindings battleKeys = new BattleKeyBindings(
                battleKeysWith(BattleKeyBindings.defaults().getFirstPlayerKeys(), GameCommand.MOVE_LEFT, "Q"),
                battleKeysWith(BattleKeyBindings.defaults().getSecondPlayerKeys(), GameCommand.HOLD, "SHIFT"),
                battleKeysWith(BattleKeyBindings.defaults().getSharedKeys(), GameCommand.PAUSE, "BACK_SPACE"));
        GameSettings settings = new GameSettings(
                WindowSize.LARGE, GameSettings.getDefaultKeyBindings(), true, Difficulty.HARD, battleKeys);

        // when: 저장하고 프로그램을 다시 실행한 것처럼 불러온다
        repository.save(settings);
        GameSettings loaded = new FileSettingsRepository(settingsFile).load();

        // then
        assertSameBattleKeys(battleKeys, loaded.getBattleKeyBindings());
        assertSameSettings(settings, loaded);
    }

    @Test
    void useDefaultBattleKeysWhenFileHasNoBattleKeys() throws IOException {
        // given: 대전 키가 생기기 전에 저장된 설정 파일
        Files.writeString(settingsFile, """
                {
                  "windowSize": "LARGE",
                  "keyBindings": {"MOVE_LEFT": "A"},
                  "colorBlindMode": true,
                  "difficulty": "HARD"
                }
                """);

        // when
        GameSettings loaded = repository.load();

        // then: 기존 값은 그대로, 대전 키만 기본값
        assertSameBattleKeys(BattleKeyBindings.defaults(), loaded.getBattleKeyBindings());
        assertEquals(WindowSize.LARGE, loaded.getWindowSize());
        assertEquals(CUSTOM_LEFT_KEY, loaded.getKeyBindings().get(GameCommand.MOVE_LEFT));
        assertTrue(loaded.isColorBlindMode());
        assertEquals(Difficulty.HARD, loaded.getDifficulty());
    }

    @Test
    void fillMissingBattleCommandsWithDefaultKeys() throws IOException {
        // given: 1P 왼쪽 이동만 저장되어 있고 2P·공용 항목은 없다
        Files.writeString(settingsFile, """
                {
                  "battleKeyBindings": {
                    "firstPlayer": {"MOVE_LEFT": "Q"}
                  }
                }
                """);

        // when
        BattleKeyBindings loaded = repository.load().getBattleKeyBindings();

        // then
        assertEquals(
                battleKeysWith(BattleKeyBindings.defaults().getFirstPlayerKeys(), GameCommand.MOVE_LEFT, "Q"),
                loaded.getFirstPlayerKeys());
        assertEquals(BattleKeyBindings.defaults().getSecondPlayerKeys(), loaded.getSecondPlayerKeys());
        assertEquals(BattleKeyBindings.defaults().getSharedKeys(), loaded.getSharedKeys());
    }

    @Test
    void replaceInvalidBattleKeyNameWithDefaultKey() throws IOException {
        // given: 오타 난 키 이름, null 키 이름
        Files.writeString(settingsFile, """
                {
                  "battleKeyBindings": {
                    "firstPlayer": {"MOVE_LEFT": "LFET", "HOLD": "Q"},
                    "secondPlayer": {"MOVE_LEFT": null}
                  }
                }
                """);

        // when
        BattleKeyBindings loaded = repository.load().getBattleKeyBindings();

        // then: 잘못된 것만 기본 키, 올바른 것은 유지
        assertEquals("F", loaded.getFirstPlayerKeys().get(GameCommand.MOVE_LEFT));
        assertEquals("Q", loaded.getFirstPlayerKeys().get(GameCommand.HOLD));
        assertEquals("LEFT", loaded.getSecondPlayerKeys().get(GameCommand.MOVE_LEFT));
    }

    @Test
    void ignoreBattleCommandsOutsideTheirGroup() throws IOException {
        // given: 1P 묶음에 공용 명령·없는 명령·NONE이, 공용 묶음에 플레이어 명령이 들어 있다
        Files.writeString(settingsFile, """
                {
                  "battleKeyBindings": {
                    "firstPlayer": {"PAUSE": "Q", "REMOVED_COMMAND": "W", "NONE": "E"},
                    "shared": {"HOLD": "R"}
                  }
                }
                """);

        // when
        BattleKeyBindings loaded = repository.load().getBattleKeyBindings();

        // then: 묶음에 맞지 않는 항목은 무시하고 기본값을 쓴다
        assertSameBattleKeys(BattleKeyBindings.defaults(), loaded);
    }

    @Test
    void keepBattleKeysWhenOnlySinglePlayerKeysChange() throws IOException {
        // given: 1인 게임 키만 바꾼 설정 (대전 키는 기본값)
        GameSettings settings = createCustomSettings();

        // when
        repository.save(settings);
        GameSettings loaded = repository.load();

        // then: 두 키 설정은 서로 영향이 없다
        assertEquals(settings.getKeyBindings(), loaded.getKeyBindings());
        assertSameBattleKeys(BattleKeyBindings.defaults(), loaded.getBattleKeyBindings());
    }

    // 기본 키 묶음에서 한 명령만 바꾼 Map
    private static Map<GameCommand, String> battleKeysWith(Map<GameCommand, String> defaults,
            GameCommand command, String keyName) {
        Map<GameCommand, String> keys = new EnumMap<>(GameCommand.class);
        keys.putAll(defaults);
        keys.put(command, keyName);
        return keys;
    }

    private static void assertSameBattleKeys(BattleKeyBindings expected, BattleKeyBindings actual) {
        assertEquals(expected.getFirstPlayerKeys(), actual.getFirstPlayerKeys());
        assertEquals(expected.getSecondPlayerKeys(), actual.getSecondPlayerKeys());
        assertEquals(expected.getSharedKeys(), actual.getSharedKeys());
    }

    private GameSettings createCustomSettings() {
        Map<GameCommand, String> keyBindings = new EnumMap<>(GameCommand.class);
        keyBindings.putAll(GameSettings.getDefaultKeyBindings());
        keyBindings.put(GameCommand.MOVE_LEFT, CUSTOM_LEFT_KEY);
        keyBindings.put(GameCommand.MOVE_RIGHT, CUSTOM_RIGHT_KEY);
        return new GameSettings(WindowSize.LARGE, keyBindings, true, Difficulty.NORMAL);
    }

    private void assertSameAsDefaults(GameSettings actual) {
        assertSameSettings(GameSettings.defaults(), actual);
    }

    /**
     * GameSettings 에 equals 가 없으므로 항목을 각각 비교한다.
     */
    private void assertSameSettings(GameSettings expected, GameSettings actual) {
        assertEquals(expected.getWindowSize(), actual.getWindowSize());
        assertEquals(expected.isColorBlindMode(), actual.isColorBlindMode());
        assertEquals(expected.getKeyBindings(), actual.getKeyBindings());
        assertEquals(expected.getDifficulty(), actual.getDifficulty());
    }

    /**
     * excluded 를 제외한 모든 명령이 기본 키를 가지는지 확인한다.
     */
    private void assertEveryDefaultKeyExceptFor(Map<GameCommand, String> actual, GameCommand excluded) {
        for (Map.Entry<GameCommand, String> entry : GameSettings.getDefaultKeyBindings().entrySet()) {
            if (entry.getKey() != excluded) {
                assertEquals(entry.getValue(), actual.get(entry.getKey()));
            }
        }
    }
    // AI-assisted code end
}
