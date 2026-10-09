package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import tetris.model.GameCommand;
import tetris.settings.GameSettings;

class JavaFxGameViewTest {

    // AI-assisted code start

    @BeforeAll
    static void initializeJavaFx() throws Exception {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException alreadyStarted) {
            // 다른 테스트에서 JavaFX를 이미 초기화한 경우다.
        }

        runOnFxThread(() -> Platform.setImplicitExit(false));
    }

    @Test
    void dispatchDefaultKeyBindings() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                JavaFxGameView view =
                        new JavaFxGameView(stage, GameSettings.defaults());

                List<GameCommand> commands = new ArrayList<>();
                view.setCommandHandler(commands::add);

                for (Map.Entry<GameCommand, String> binding
                        : GameSettings.getDefaultKeyBindings().entrySet()) {
                    commands.clear();

                    pressKey(stage, KeyCode.valueOf(binding.getValue()));

                    assertEquals(List.of(binding.getKey()), commands);
                }
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void useCustomKeyAndIgnorePreviousKey() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                Map<GameCommand, String> bindings =
                        new HashMap<>(GameSettings.getDefaultKeyBindings());
                bindings.put(GameCommand.MOVE_LEFT, "A");

                GameSettings settings = new GameSettings(
                        GameSettings.getDefaultWindowSize(),
                        bindings,
                        false
                );

                JavaFxGameView view =
                        new JavaFxGameView(stage, settings);

                List<GameCommand> commands = new ArrayList<>();
                view.setCommandHandler(commands::add);

                pressKey(stage, KeyCode.LEFT);
                assertTrue(commands.isEmpty());

                pressKey(stage, KeyCode.A);
                assertEquals(List.of(GameCommand.MOVE_LEFT), commands);
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void useCustomQuitKey() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                Map<GameCommand, String> bindings =
                        new HashMap<>(GameSettings.getDefaultKeyBindings());
                bindings.put(GameCommand.QUIT_GAME, "Q");

                GameSettings settings = new GameSettings(
                        GameSettings.getDefaultWindowSize(),
                        bindings,
                        false
                );

                JavaFxGameView view =
                        new JavaFxGameView(stage, settings);

                List<GameCommand> commands = new ArrayList<>();
                view.setCommandHandler(commands::add);

                pressKey(stage, KeyCode.ESCAPE);
                assertTrue(commands.isEmpty());

                pressKey(stage, KeyCode.Q);
                assertEquals(List.of(GameCommand.QUIT_GAME), commands);
            } finally {
                stage.close();
            }
        });
    }

    private static void pressKey(Stage stage, KeyCode keyCode) {
        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "",
                "",
                keyCode,
                false,
                false,
                false,
                false
        );

        Event.fireEvent(stage.getScene().getRoot(), event);
    }

    private static void runOnFxThread(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);

        try {
            task.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof Error error) {
                throw error;
            }

            if (cause instanceof Exception error) {
                throw error;
            }

            throw new RuntimeException(cause);
        }
    }

    // AI-assisted code end
}