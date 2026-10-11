package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import tetris.model.GameSnapshot;
import tetris.model.GameStatus;
import tetris.model.TetrominoType;
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

    @Test
    void updateScoreAndDropInterval() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                JavaFxGameView view =
                        new JavaFxGameView(stage, GameSettings.defaults());

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(),
                        null,
                        100L,
                        GameStatus.PLAYING,
                        1_000L));

                Label scoreLabel = (Label) stage.getScene()
                        .lookup("#score-label");
                Label intervalLabel = (Label) stage.getScene()
                        .lookup("#interval-label");

                assertEquals("점수: 100", scoreLabel.getText());
                assertEquals(
                        "낙하 간격: 1000ms",
                        intervalLabel.getText());

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(),
                        null,
                        250L,
                        GameStatus.PLAYING,
                        900L));

                assertEquals("점수: 250", scoreLabel.getText());
                assertEquals(
                        "낙하 간격: 900ms",
                        intervalLabel.getText());
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void updateAndClearNextPiecePreview() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                JavaFxGameView view =
                        new JavaFxGameView(stage, GameSettings.defaults());

                Canvas preview = (Canvas) stage.getScene()
                        .lookup("#next-preview");

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(TetrominoType.O),
                        null,
                        0L,
                        GameStatus.PLAYING,
                        1_000L));

                assertTrue(containsColor(
                        preview,
                        Color.web("#FACC15")));

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(TetrominoType.I),
                        null,
                        0L,
                        GameStatus.PLAYING,
                        1_000L));

                assertTrue(containsColor(
                        preview,
                        Color.web("#22D3EE")));
                assertFalse(containsColor(
                        preview,
                        Color.web("#FACC15")));

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(),
                        null,
                        0L,
                        GameStatus.PLAYING,
                        1_000L));

                assertFalse(containsColor(
                        preview,
                        Color.web("#22D3EE")));
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void showAndClearHeldPiece() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                JavaFxGameView view =
                        new JavaFxGameView(stage, GameSettings.defaults());

                Canvas preview = (Canvas) stage.getScene()
                        .lookup("#hold-preview");

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(),
                        TetrominoType.T,
                        0L,
                        GameStatus.PLAYING,
                        1_000L));

                assertTrue(containsColor(
                        preview,
                        Color.web("#A855F7")));

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(),
                        null,
                        0L,
                        GameStatus.PLAYING,
                        1_000L));

                assertFalse(containsColor(
                        preview,
                        Color.web("#A855F7")));
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void showPauseNoticeOnlyWhilePaused() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                JavaFxGameView view =
                        new JavaFxGameView(stage, GameSettings.defaults());

                Label pauseLabel = (Label) stage.getScene()
                        .lookup("#pause-label");

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(),
                        null,
                        0L,
                        GameStatus.PAUSED,
                        1_000L));

                assertTrue(pauseLabel.isVisible());

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(),
                        null,
                        0L,
                        GameStatus.PLAYING,
                        1_000L));

                assertFalse(pauseLabel.isVisible());
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void useColorBlindPalette() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                GameSettings settings = new GameSettings(
                        GameSettings.getDefaultWindowSize(),
                        GameSettings.getDefaultKeyBindings(),
                        true);

                JavaFxGameView view =
                        new JavaFxGameView(stage, settings);

                int[][] cells = new int[20][10];
                cells[0][0] = TetrominoType.I.getCellId();

                view.render(new GameSnapshot(
                        cells,
                        List.of(TetrominoType.I),
                        TetrominoType.I,
                        0L,
                        GameStatus.PLAYING,
                        1_000L));

                Canvas board = (Canvas) stage.getScene()
                        .lookup("#game-board");
                Canvas next = (Canvas) stage.getScene()
                        .lookup("#next-preview");
                Canvas hold = (Canvas) stage.getScene()
                        .lookup("#hold-preview");

                Color accessibleColor = Color.web("#56B4E9");

                assertTrue(containsColor(board, accessibleColor));
                assertTrue(containsColor(next, accessibleColor));
                assertTrue(containsColor(hold, accessibleColor));
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void ignoreRepeatedPauseUntilKeyReleased() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                Map<GameCommand, String> bindings =
                        new HashMap<>(GameSettings.getDefaultKeyBindings());
                bindings.put(GameCommand.PAUSE, "A");

                GameSettings settings = new GameSettings(
                        GameSettings.getDefaultWindowSize(), bindings, false);
                JavaFxGameView view = new JavaFxGameView(stage, settings);

                List<GameCommand> commands = new ArrayList<>();
                view.setCommandHandler(commands::add);

                pressKey(stage, KeyCode.A);
                pressKey(stage, KeyCode.A);
                assertEquals(List.of(GameCommand.PAUSE), commands);

                releaseKey(stage, KeyCode.A);
                pressKey(stage, KeyCode.A);

                assertEquals(
                        List.of(GameCommand.PAUSE, GameCommand.PAUSE), commands);
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void keepRepeatedMovementKeys() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                JavaFxGameView view =
                        new JavaFxGameView(stage, GameSettings.defaults());

                List<GameCommand> commands = new ArrayList<>();
                view.setCommandHandler(commands::add);

                pressKey(stage, KeyCode.LEFT);
                pressKey(stage, KeyCode.LEFT);

                assertEquals(
                        List.of(GameCommand.MOVE_LEFT, GameCommand.MOVE_LEFT),
                        commands);
            } finally {
                stage.close();
            }
        });
    }

    private static void releaseKey(Stage stage, KeyCode keyCode) {
        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_RELEASED, "", "", keyCode,
                false, false, false, false);

        Event.fireEvent(stage.getScene().getRoot(), event);
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

    private static boolean containsColor(Canvas canvas, Color expected) {
        WritableImage image = canvas.snapshot(null, null);

        for (int row = 0; row < (int) image.getHeight(); row++) {
            for (int column = 0;
                    column < (int) image.getWidth();
                    column++) {

                Color actual = image.getPixelReader()
                        .getColor(column, row);

                if (Math.abs(actual.getRed() - expected.getRed()) < 0.01
                        && Math.abs(actual.getGreen()
                                - expected.getGreen()) < 0.01
                        && Math.abs(actual.getBlue()
                                - expected.getBlue()) < 0.01) {
                    return true;
                }
            }
        }

        return false;
    }
    
    // AI-assisted code end
}