package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 저장된 키 이름(JavaFX KeyCode 이름)을 화면용 짧은 글자로 바꾸는 규칙
 */
class KeyNamesTest {

    // AI-assisted code start
    @ParameterizedTest
    @CsvSource({
            "LEFT, ←",
            "RIGHT, →",
            "UP, ↑",
            "DOWN, ↓",
            "ESCAPE, ESC",
            "ENTER, Enter",
            "SPACE, Space",
            "TAB, Tab",
            "PERIOD, .",
            "SLASH, /",
            "DIGIT7, 7"
    })
    void showSpecialKeysAsShortSymbols(String keyName, String expected) {
        // given: 특수 키 이름

        // when
        String shown = KeyNames.display(keyName);

        // then
        assertEquals(expected, shown);
    }

    @Test
    void showCommaKeyAsComma() {
        // given: CSV에 쉼표를 넣을 수 없어 따로 확인한다
        String keyName = "COMMA";

        // when
        String shown = KeyNames.display(keyName);

        // then
        assertEquals(",", shown);
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "Z", "F", "F5", "NUMPAD3", "DIGIT10"})
    void keepOtherKeyNamesAsTheyAre(String keyName) {
        // given: 이미 짧거나 따로 정하지 않은 키 이름

        // when
        String shown = KeyNames.display(keyName);

        // then
        assertEquals(keyName, shown);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void showMissingKeyAsUnassigned(String keyName) {
        // given: 배정되지 않은 키

        // when
        String shown = KeyNames.display(keyName);

        // then
        assertEquals(KeyNames.UNASSIGNED, shown);
    }
    // AI-assisted code end
}
