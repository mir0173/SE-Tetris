package tetris;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.Window;

import tetris.persistence.FileScoreRepository;
import tetris.persistence.FileSettingsRepository;
import tetris.settings.WindowSize;

// AI-assisted code start
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class TetrisApplicationIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    private String previousUserHome;
    private Stage stage;

    @BeforeAll
    static void initializeJavaFx() throws Exception {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // 다른 테스트에서 초기화한 JavaFX를 사용한다.
        }
        onFx(() -> { Platform.setImplicitExit(false); return null; });
    }

    @BeforeEach
    void startApplicationWithTemporaryData() throws Exception {
        previousUserHome = System.getProperty("user.home");
        System.setProperty("user.home", temporaryDirectory.toString());
        onFx(() -> {
            stage = new Stage();
            new TetrisApplication().start(stage);
            return null;
        });
    }

    @AfterEach
    void closeApplicationAndRestoreDataPath() throws Exception {
        try {
            onFx(() -> {
                if (stage != null) {
                    if (stage.getScene().lookup("#game-board") != null) {
                        pressKey(KeyCode.ESCAPE);
                    }
                    for (Window window : new ArrayList<>(Window.getWindows())) {
                        if (window instanceof Stage dialog && dialog.getOwner() == stage) {
                            dialog.close();
                        }
                    }
                    stage.close();
                }
                return null;
            });
        } finally {
            System.setProperty("user.home", previousUserHome);
        }
    }

    @Test
    void saveSettingsImmediatelyAndApplyThemAfterRestart() throws Exception {
        onFx(() -> {
            clickButton("설정");
            pressKey(KeyCode.RIGHT);
            pressKey(KeyCode.DOWN);
            pressKey(KeyCode.RIGHT);
            return null;
        });

        var settings = new FileSettingsRepository(dataFile("settings.json")).load();
        assertEquals(WindowSize.LARGE, settings.getWindowSize());
        assertTrue(settings.isColorBlindMode());

        onFx(() -> {
            pressKey(KeyCode.ESCAPE);
            stage.close();
            stage = new Stage();
            new TetrisApplication().start(stage);
            clickButton("게임 시작");
            assertEquals(600, stage.getScene().getWidth(), 1.0);
            assertEquals(720, stage.getScene().getHeight(), 1.0);
            pressKey(KeyCode.ESCAPE);
            clickButton("설정");
            assertTrue(hasLabel("화면 크기: 크게"));
            assertTrue(hasLabel("색맹 모드: 켜짐"));
            return null;
        });
    }

    @Test
    void runGravityPauseResumeAndReturnToMenu() throws Exception {
        onFx(() -> { clickButton("게임 시작"); return null; });
        awaitScoreGreaterThan(0);

        long pausedScore = onFx(() -> {
            pressKey(KeyCode.P);
            assertTrue(((Label) stage.getScene().lookup("#pause-label")).isVisible());
            return displayedScore();
        });

        Thread.sleep(1_100);
        assertEquals(pausedScore, onFx(this::displayedScore));

        onFx(() -> {
            pressKey(KeyCode.P);
            assertFalse(((Label) stage.getScene().lookup("#pause-label")).isVisible());
            return null;
        });
        awaitScoreGreaterThan(pausedScore);

        onFx(() -> {
            pressKey(KeyCode.ESCAPE);
            assertTrue(stage.getScene().lookup("#game-board") == null);
            assertTrue(hasLabel("Tetris"));
            return null;
        });
    }

    @Test
    void saveGameOverScoreAndHighlightItOnScoreboard() throws Exception {
        CompletableFuture<Void> nameEntered = new CompletableFuture<>();

        onFx(() -> {
            clickButton("게임 시작");
            for (int index = 0; index < 100; index++) {
                pressKey(KeyCode.SPACE);
            }
            Platform.runLater(() -> {
                try {
                    DialogPane dialog = Window.getWindows().stream()
                            .filter(window -> window != stage && window.isShowing())
                            .map(window -> window.getScene().getRoot())
                            .filter(DialogPane.class::isInstance)
                            .map(DialogPane.class::cast)
                            .findFirst().orElseThrow();
                    ((TextField) dialog.lookup(".text-field")).setText("T10 tester");
                    ((Button) dialog.lookupButton(ButtonType.OK)).fire();
                    nameEntered.complete(null);
                } catch (Throwable exception) {
                    nameEntered.completeExceptionally(exception);
                }
            });
            return null;
        });

        nameEntered.get(10, TimeUnit.SECONDS);
        onFx(() -> {
            assertTrue(hasLabel("T10 tester"));
            assertTrue(hasLabel("★"));
            return null;
        });

        var entries = new FileScoreRepository(dataFile("scores.json")).load();
        assertEquals(1, entries.size());
        assertEquals("T10 tester", entries.get(0).getName());
        assertTrue(entries.get(0).getScore() > 0);
    }

    private Path dataFile(String name) {
        return temporaryDirectory.resolve(".se-tetris").resolve(name);
    }

    private void clickButton(String text) {
        stage.getScene().getRoot().applyCss();
        Button button = stage.getScene().getRoot().lookupAll(".button").stream()
                .filter(Button.class::isInstance).map(Button.class::cast)
                .filter(candidate -> candidate.getText().equals(text))
                .findFirst().orElseThrow();
        button.fire();
    }

    private boolean hasLabel(String text) {
        stage.getScene().getRoot().applyCss();
        return stage.getScene().getRoot().lookupAll(".label").stream()
                .filter(Label.class::isInstance).map(Label.class::cast)
                .anyMatch(label -> label.getText().contains(text));
    }

    private void pressKey(KeyCode keyCode) {
        Event.fireEvent(stage.getScene().getRoot(), new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", keyCode, false, false, false, false));
        Event.fireEvent(stage.getScene().getRoot(), new KeyEvent(
                KeyEvent.KEY_RELEASED, "", "", keyCode, false, false, false, false));
    }

    private long displayedScore() {
        String text = ((Label) stage.getScene().lookup("#score-label")).getText();
        return Long.parseLong(text.substring(text.indexOf(':') + 1).strip());
    }

    private void awaitScoreGreaterThan(long previousScore) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (onFx(this::displayedScore) > previousScore) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("자동 낙하 후 점수가 증가하지 않았습니다.");
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        try {
            return task.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof Error error) { throw error; }
            if (exception.getCause() instanceof Exception cause) { throw cause; }
            throw new RuntimeException(exception.getCause());
        }
    }
}
// AI-assisted code end
