package tetris.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

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
}
