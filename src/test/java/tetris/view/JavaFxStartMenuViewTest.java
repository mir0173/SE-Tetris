package tetris.view;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

class JavaFxStartMenuViewTest {

     // AI-assisted code start
    private JavaFxStartMenuView menu;
    private int[] actionCalls;

    @BeforeAll
    static void initializeJavaFx() throws Exception {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException alreadyStarted) {
        
        }

        runOnFxThread(() -> Platform.setImplicitExit(false));
    }

    @BeforeEach
    void setUp() throws Exception {
        runOnFxThread(() -> {
            actionCalls = new int[4];

            menu = new JavaFxStartMenuView(
                    () -> actionCalls[0]++,
                    () -> actionCalls[1]++,
                    () -> actionCalls[2]++,
                    () -> actionCalls[3]++
            );
        });
    }

    @Test
    void focusFirstButtonSelectsStartGame() throws Exception {
        runOnFxThread(() -> {
            menu.focusFirstButton();

            assertSame(
                    findButton("게임 시작"),
                    menu.getScene().getFocusOwner()
            );
        });
    }

    @ParameterizedTest
    @CsvSource({
            "게임 시작, 0",
            "설정, 1",
            "스코어보드, 2",
            "종료, 3"
    })
    void buttonRunsOnlyItsOwnAction(
            String buttonText,
            int expectedAction
    ) throws Exception {
        runOnFxThread(() -> {
            findButton(buttonText).fire();

            int[] expectedCalls = new int[4];
            expectedCalls[expectedAction] = 1;

            assertArrayEquals(expectedCalls, actionCalls);
        });
    }

    @ParameterizedTest
    @CsvSource({
            "게임 시작, DOWN, 설정",
            "설정, UP, 게임 시작",
            "게임 시작, UP, 종료",
            "종료, DOWN, 게임 시작"
    })
    void arrowKeyMovesFocus(
            String currentButton,
            KeyCode key,
            String expectedButton
    ) throws Exception {
        runOnFxThread(() -> {
            findButton(currentButton).requestFocus();

            pressKey(key);

            assertSame(
                    findButton(expectedButton),
                    menu.getScene().getFocusOwner()
            );

            // 이동만 했으므로 메뉴 동작은 실행되지 않아야 한다.
            assertArrayEquals(new int[4], actionCalls);
        });
    }

    @ParameterizedTest
    @CsvSource({
            "게임 시작, 0",
            "설정, 1",
            "스코어보드, 2",
            "종료, 3"
    })
    void enterRunsOnlySelectedAction(
            String buttonText,
            int expectedAction
    ) throws Exception {
        runOnFxThread(() -> {
            findButton(buttonText).requestFocus();

            pressKey(KeyCode.ENTER);

            int[] expectedCalls = new int[4];
            expectedCalls[expectedAction] = 1;

            assertArrayEquals(expectedCalls, actionCalls);
        });
    }

    private Button findButton(String text) {
        return menu.getScene()
                .getRoot()
                .lookupAll(".button")
                .stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> text.equals(button.getText()))
                .findFirst()
                .orElseThrow(
                        () -> new AssertionError("버튼을 찾을 수 없음: " + text)
                );
    }

    private void pressKey(KeyCode key) {
        Node target = menu.getScene().getFocusOwner();

        if (target == null) {
            target = menu.getScene().getRoot();
        }

        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "",
                "",
                key,
                false,
                false,
                false,
                false
        );

        Event.fireEvent(target, event);
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
    // AI-assisted code end (테스트 코드)
}