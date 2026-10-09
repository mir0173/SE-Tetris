package tetris.settings;

import java.util.Map;
import tetris.model.GameCommand;

public class GameSettings {

    private static final WindowSize DEFAULT_WINDOW_SIZE = WindowSize.MEDIUM;
    private static final boolean DEFAULT_COLOR_BLIND_MODE = false;
    private static final Map<GameCommand, String> DEFAULT_KEY_BINDINGS = Map.of(
        GameCommand.MOVE_LEFT, "LEFT",
        GameCommand.MOVE_RIGHT, "RIGHT",
        GameCommand.SOFT_DROP, "DOWN",
        GameCommand.HARD_DROP, "SPACE",
        GameCommand.ROTATE_CLOCKWISE, "UP",
        GameCommand.ROTATE_COUNTERCLOCKWISE, "Z",
        GameCommand.HOLD, "C",
        GameCommand.PAUSE, "P",
        GameCommand.QUIT_GAME, "ESCAPE"
    );

    private final WindowSize windowSize;
    private final Map<GameCommand, String> keyBindings;
    private final boolean colorBlindMode;

    public GameSettings(WindowSize windowSize, Map<GameCommand, String> keyBindings, boolean colorBlindMode) {
        this.windowSize = windowSize;
        this.keyBindings = Map.copyOf(keyBindings);
        this.colorBlindMode = colorBlindMode;
    }

    public static Map<GameCommand, String> getDefaultKeyBindings() {
        return DEFAULT_KEY_BINDINGS;
    }

    // AI-assisted code start
    public static WindowSize getDefaultWindowSize() {
        return DEFAULT_WINDOW_SIZE;
    }
    // AI-assisted code end

    public static GameSettings defaults() {
        return new GameSettings(DEFAULT_WINDOW_SIZE, DEFAULT_KEY_BINDINGS, DEFAULT_COLOR_BLIND_MODE);
    }

    public WindowSize getWindowSize() {
        return windowSize;
    }

    public Map<GameCommand, String> getKeyBindings() {
        return keyBindings;
    }

    public boolean isColorBlindMode() {
        return colorBlindMode;
    }
}