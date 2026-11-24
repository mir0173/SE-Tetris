package tetris.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tetris.model.Difficulty;
import tetris.model.GameCommand;

class GameSettingsTest {

    // AI-assisted code start
    @Test
    void createDefaultSettings() {
        // given

        // when
        GameSettings settings = GameSettings.defaults();

        // then
        assertEquals(WindowSize.MEDIUM, settings.getWindowSize());
        assertFalse(settings.isColorBlindMode());
        assertEquals(GameSettings.getDefaultKeyBindings(), settings.getKeyBindings());
    }

    @Test
    void provideDefaultKeyForEveryCommandExceptNone() {
        // given
        Map<GameCommand, String> defaultKeyBindings = GameSettings.getDefaultKeyBindings();

        // when & then: 명령이 새로 추가됐는데 기본 키를 빠뜨리면 이 테스트가 실패한다
        for (GameCommand command : GameCommand.values()) {
            if (command == GameCommand.NONE) {
                assertFalse(defaultKeyBindings.containsKey(command));
            } else {
                assertNotNull(defaultKeyBindings.get(command), command + " 의 기본 키가 없다");
            }
        }
    }

    @Test
    void provideDefaultWindowSize() {
        // given

        // when
        WindowSize defaultWindowSize = GameSettings.getDefaultWindowSize();

        // then
        assertEquals(WindowSize.MEDIUM, defaultWindowSize);
    }

    @Test
    void keepKeyBindingsUnmodifiable() {
        // given
        GameSettings settings = GameSettings.defaults();

        // when & then: 방어적 복사로 만든 불변 Map 이므로 수정할 수 없다
        assertThrows(UnsupportedOperationException.class,
                () -> settings.getKeyBindings().put(GameCommand.HOLD, "X"));
    }
    // AI-assisted code end

    // AI-assisted code start
    @Test
    void useDefaultBattleKeysInDefaultSettings() {
        // given

        // when
        GameSettings settings = GameSettings.defaults();

        // then
        assertSameBattleKeys(BattleKeyBindings.defaults(), settings.getBattleKeyBindings());
    }

    @Test
    void useDefaultBattleKeysWhenCreatedWithoutThem() {
        // given: 대전 키가 생기기 전부터 쓰던 생성자(인자 4개)

        // when
        GameSettings settings = new GameSettings(
                WindowSize.LARGE, GameSettings.getDefaultKeyBindings(), true, Difficulty.HARD);

        // then
        assertSameBattleKeys(BattleKeyBindings.defaults(), settings.getBattleKeyBindings());
    }

    @Test
    void keepGivenBattleKeys() {
        // given
        Map<GameCommand, String> first = new EnumMap<>(BattleKeyBindings.defaults().getFirstPlayerKeys());
        first.put(GameCommand.MOVE_LEFT, "Q");
        BattleKeyBindings battleKeys = new BattleKeyBindings(first,
                BattleKeyBindings.defaults().getSecondPlayerKeys(), BattleKeyBindings.defaults().getSharedKeys());

        // when
        GameSettings settings = new GameSettings(
                WindowSize.SMALL, GameSettings.getDefaultKeyBindings(), false, Difficulty.EASY, battleKeys);

        // then: 대전 키만 바뀌고 1인 키는 그대로다
        assertEquals("Q", settings.getBattleKeyBindings().getFirstPlayerKeys().get(GameCommand.MOVE_LEFT));
        assertEquals(GameSettings.getDefaultKeyBindings(), settings.getKeyBindings());
        assertEquals(WindowSize.SMALL, settings.getWindowSize());
        assertEquals(Difficulty.EASY, settings.getDifficulty());
    }

    @Test
    void rejectNullBattleKeys() {
        // given

        // when & then
        assertThrows(NullPointerException.class, () -> new GameSettings(
                WindowSize.MEDIUM, GameSettings.getDefaultKeyBindings(), false, Difficulty.NORMAL, null));
    }

    private static void assertSameBattleKeys(BattleKeyBindings expected, BattleKeyBindings actual) {
        assertEquals(expected.getFirstPlayerKeys(), actual.getFirstPlayerKeys());
        assertEquals(expected.getSecondPlayerKeys(), actual.getSecondPlayerKeys());
        assertEquals(expected.getSharedKeys(), actual.getSharedKeys());
    }
    // AI-assisted code end
}
