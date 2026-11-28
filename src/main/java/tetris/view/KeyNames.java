package tetris.view;

import java.util.Map;

// AI-assisted code start
/**
 * 저장된 키 이름(JavaFX KeyCode 이름, 예: "LEFT", "ESCAPE")을 화면에 보여 줄 짧은 글자로 바꾼다.
 * 예: LEFT → ←, ESCAPE → ESC, COMMA → ",", DIGIT1 → 1
 */
public final class KeyNames {

    public static final String UNASSIGNED = "-";

    private static final String DIGIT_PREFIX = "DIGIT";
    private static final Map<String, String> DISPLAY_NAMES = Map.ofEntries(
        Map.entry("LEFT", "←"),
        Map.entry("RIGHT", "→"),
        Map.entry("UP", "↑"),
        Map.entry("DOWN", "↓"),
        Map.entry("ESCAPE", "ESC"),
        Map.entry("ENTER", "Enter"),
        Map.entry("SPACE", "Space"),
        Map.entry("TAB", "Tab"),
        Map.entry("SHIFT", "Shift"),
        Map.entry("CONTROL", "Ctrl"),
        Map.entry("ALT", "Alt"),
        Map.entry("BACK_SPACE", "Backspace"),
        Map.entry("COMMA", ","),
        Map.entry("PERIOD", "."),
        Map.entry("SLASH", "/"),
        Map.entry("BACK_SLASH", "\\"),
        Map.entry("SEMICOLON", ";"),
        Map.entry("QUOTE", "'"),
        Map.entry("MINUS", "-"),
        Map.entry("EQUALS", "="),
        Map.entry("OPEN_BRACKET", "["),
        Map.entry("CLOSE_BRACKET", "]")
    );

    private KeyNames() {
    }

    /**
     * 키 이름을 화면 표시용 글자로 바꾼다. 목록에 없는 이름은 그대로 돌려준다.
     *
     * @param keyName JavaFX KeyCode 이름. 비어 있으면 배정되지 않은 키로 본다
     * @return 화면에 보여 줄 글자
     */
    public static String display(String keyName) {
        if (keyName == null || keyName.isBlank()) {
            return UNASSIGNED;
        }
        String displayName = DISPLAY_NAMES.get(keyName);
        if (displayName != null) {
            return displayName;
        }
        if (keyName.startsWith(DIGIT_PREFIX) && keyName.length() == DIGIT_PREFIX.length() + 1) {
            return keyName.substring(DIGIT_PREFIX.length());
        }
        return keyName;
    }
}
// AI-assisted code end
