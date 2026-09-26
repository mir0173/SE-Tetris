package tetris.settings;

import java.util.Map;
import tetris.model.GameCommand;

public class GameSettings {

    private final WindowSize windowSize;
    private final Map<GameCommand, String> keyBindings;
    private final boolean colorBlindMode;

    public GameSettings(WindowSize windowSize, Map<GameCommand, String> keyBindings, boolean colorBlindMode) {
        this.windowSize = windowSize;
        this.keyBindings = Map.copyOf(keyBindings);
        this.colorBlindMode = colorBlindMode;
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