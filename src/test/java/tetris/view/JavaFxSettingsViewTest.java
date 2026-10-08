package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import tetris.model.GameCommand;
import tetris.settings.GameSettings;
import tetris.settings.WindowSize;

class JavaFxSettingsViewTest {

    // AI-assisted code start
    private static final String TITLE_TEXT = "설정";
    private static final String SELECTED_MARKER = "▶ ";
    private static final String UNSELECTED_MARKER = "   ";
    private static final String WAITING_KEY_TEXT = "키를 누르세요...";
    private static final String DEFAULTS_RESET_MESSAGE = "기본값으로 되돌렸습니다";
    private static final String SCOREBOARD_RESET_MESSAGE = "스코어보드를 초기화했습니다";
    private static final String RESET_DEFAULTS_CONFIRMATION_TEXT =
            "기본값으로 되돌리기: 정말 되돌리려면 Y, 취소는 ESC";
    private static final String RESET_SCOREBOARD_CONFIRMATION_TEXT =
            "스코어보드 초기화: 정말 지우려면 Y, 취소는 ESC";

    private static final int WINDOW_SIZE_ROW = 0;
    private static final int COLOR_BLIND_ROW = 1;
    private static final int MOVE_LEFT_ROW = 2;
    private static final int MOVE_RIGHT_ROW = 3;
    private static final int RESET_DEFAULTS_ROW = 11;
    private static final int RESET_SCOREBOARD_ROW = 12;
    private static final int BACK_ROW = 13;
    private static final int ROW_COUNT = 14;

    private final List<GameSettings> changes = new ArrayList<>();
    private int resetScoreboardCalls;
    private int backCalls;

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
    void setUp() {
        changes.clear();
        resetScoreboardCalls = 0;
        backCalls = 0;
    }

    @Test
    void showTitleAndCurrentSettings() throws Exception {
        runOnFxThread(() -> {
            // given & when
            JavaFxSettingsView view = createView();

            // then
            assertEquals(TITLE_TEXT, getTitleLabel(view).getText());
            assertEquals("화면 크기: 보통", getRowText(view, WINDOW_SIZE_ROW));
            assertEquals("색맹 모드: 꺼짐", getRowText(view, COLOR_BLIND_ROW));
            assertEquals("왼쪽 이동: LEFT", getRowText(view, MOVE_LEFT_ROW));
            assertEquals("오른쪽 이동: RIGHT", getRowText(view, MOVE_RIGHT_ROW));
            assertEquals("메뉴로", getRowText(view, BACK_ROW));
        });
    }

    @Test
    void selectFirstRowAtStart() throws Exception {
        runOnFxThread(() -> {
            // given & when
            JavaFxSettingsView view = createView();

            // then
            assertTrue(isSelected(view, WINDOW_SIZE_ROW));
            assertFalse(isSelected(view, COLOR_BLIND_ROW));
        });
    }

    @Test
    void moveSelectionWithDownAndUp() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();

            // when
            pressKey(view, KeyCode.DOWN);

            // then
            assertTrue(isSelected(view, COLOR_BLIND_ROW));
            assertFalse(isSelected(view, WINDOW_SIZE_ROW));

            // when
            pressKey(view, KeyCode.UP);

            // then
            assertTrue(isSelected(view, WINDOW_SIZE_ROW));
        });
    }

    @Test
    void wrapSelectionAroundTopAndBottom() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();

            // when: 첫 행에서 위로 이동
            pressKey(view, KeyCode.UP);

            // then: 마지막 행으로 순환한다
            assertTrue(isSelected(view, BACK_ROW));

            // when: 마지막 행에서 아래로 이동
            pressKey(view, KeyCode.DOWN);

            // then: 첫 행으로 순환한다
            assertTrue(isSelected(view, WINDOW_SIZE_ROW));
        });
    }

    @Test
    void changeWindowSizeWithRightKey() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();

            // when
            pressKey(view, KeyCode.RIGHT);

            // then
            assertEquals("화면 크기: 크게", getRowText(view, WINDOW_SIZE_ROW));
            assertEquals(1, changes.size());
            assertEquals(WindowSize.LARGE, getLastChange().getWindowSize());
        });
    }

    @Test
    void wrapWindowSizeAroundBothEnds() throws Exception {
        runOnFxThread(() -> {
            // given: 기본 크기는 MEDIUM
            JavaFxSettingsView view = createView();

            // when: MEDIUM -> SMALL -> LARGE
            pressKey(view, KeyCode.LEFT);
            pressKey(view, KeyCode.LEFT);

            // then
            assertEquals(WindowSize.LARGE, getLastChange().getWindowSize());

            // when: LARGE -> SMALL
            pressKey(view, KeyCode.RIGHT);

            // then
            assertEquals(WindowSize.SMALL, getLastChange().getWindowSize());
        });
    }

    @Test
    void toggleColorBlindModeWithEnter() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();
            moveToRow(view, COLOR_BLIND_ROW);

            // when
            pressKey(view, KeyCode.ENTER);

            // then
            assertEquals("색맹 모드: 켜짐", getRowText(view, COLOR_BLIND_ROW));
            assertTrue(getLastChange().isColorBlindMode());

            // when
            pressKey(view, KeyCode.ENTER);

            // then
            assertEquals("색맹 모드: 꺼짐", getRowText(view, COLOR_BLIND_ROW));
            assertFalse(getLastChange().isColorBlindMode());
        });
    }

    @Test
    void keepOtherSettingsWhenChangingOneSetting() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();

            // when
            pressKey(view, KeyCode.RIGHT);

            // then
            assertEquals(GameSettings.getDefaultKeyBindings(), getLastChange().getKeyBindings());
            assertFalse(getLastChange().isColorBlindMode());
        });
    }

    @Test
    void showWaitingTextWhenStartingKeyCapture() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();
            moveToRow(view, MOVE_LEFT_ROW);

            // when
            pressKey(view, KeyCode.ENTER);

            // then
            assertEquals("왼쪽 이동: " + WAITING_KEY_TEXT, getRowText(view, MOVE_LEFT_ROW));
            assertTrue(changes.isEmpty());
        });
    }

    @Test
    void assignPressedKeyToSelectedCommand() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();
            moveToRow(view, MOVE_LEFT_ROW);
            pressKey(view, KeyCode.ENTER);

            // when
            pressKey(view, KeyCode.A);

            // then
            assertEquals("왼쪽 이동: A", getRowText(view, MOVE_LEFT_ROW));
            assertEquals(1, changes.size());
            assertEquals("A", getLastChange().getKeyBindings().get(GameCommand.MOVE_LEFT));
            assertEquals("RIGHT", getLastChange().getKeyBindings().get(GameCommand.MOVE_RIGHT));
        });
    }

    @Test
    void useArrowKeyAsAssignedKeyWhileWaitingForKey() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();
            moveToRow(view, MOVE_LEFT_ROW);
            pressKey(view, KeyCode.ENTER);

            // when: 입력 대기 중에는 이동 키도 배정할 키로 쓴다
            pressKey(view, KeyCode.Q);

            // then: 선택 행이 바뀌지 않는다
            assertTrue(isSelected(view, MOVE_LEFT_ROW));
            assertEquals("Q", getLastChange().getKeyBindings().get(GameCommand.MOVE_LEFT));
        });
    }

    @Test
    void ignoreRepeatedEnterWhileStartKeyIsHeldAndKeepWaiting() throws Exception {
        runOnFxThread(() -> {
            // given: Enter 로 입력 대기를 시작하고 Enter 를 떼지 않은 상태
            JavaFxSettingsView view = createView();
            moveToRow(view, MOVE_LEFT_ROW);
            pressKey(view, KeyCode.ENTER);

            // when: 키 반복으로 Enter 가 다시 들어온다
            pressKey(view, KeyCode.ENTER);
            pressKey(view, KeyCode.ENTER);

            // then: ENTER 는 배정되지 않고 입력 대기가 유지된다
            assertTrue(changes.isEmpty());
            assertEquals("왼쪽 이동: " + WAITING_KEY_TEXT, getRowText(view, MOVE_LEFT_ROW));
            assertEquals("", getMessageLabel(view).getText());

            // when: 다른 키는 Enter 를 떼지 않아도 바로 배정된다
            pressKey(view, KeyCode.A);

            // then
            assertEquals("A", getLastChange().getKeyBindings().get(GameCommand.MOVE_LEFT));
        });
    }

    @Test
    void assignEnterAfterStartKeyIsReleased() throws Exception {
        runOnFxThread(() -> {
            // given: Enter 로 입력 대기를 시작한 뒤 Enter 를 뗀 상태
            JavaFxSettingsView view = createView();
            moveToRow(view, MOVE_LEFT_ROW);
            pressKey(view, KeyCode.ENTER);
            releaseKey(view, KeyCode.ENTER);

            // when: 다시 Enter 를 누른다
            pressKey(view, KeyCode.ENTER);

            // then: 의도한 입력이므로 ENTER 가 배정된다
            assertEquals(1, changes.size());
            assertEquals("ENTER", getLastChange().getKeyBindings().get(GameCommand.MOVE_LEFT));
            assertEquals("왼쪽 이동: ENTER", getRowText(view, MOVE_LEFT_ROW));
        });
    }

    @Test
    void ignoreEnterRepeatEachTimeKeyCaptureStarts() throws Exception {
        runOnFxThread(() -> {
            // given: 한 번 배정을 마치고 Enter 를 뗀 뒤 다른 행에서 다시 입력 대기를 시작
            JavaFxSettingsView view = createView();
            moveToRow(view, MOVE_LEFT_ROW);
            pressKey(view, KeyCode.ENTER);
            pressKey(view, KeyCode.A);
            releaseKey(view, KeyCode.ENTER);
            pressKey(view, KeyCode.DOWN);
            pressKey(view, KeyCode.ENTER);
            changes.clear();

            // when: 두 번째 입력 대기에서도 Enter 반복이 들어온다
            pressKey(view, KeyCode.ENTER);

            // then
            assertTrue(changes.isEmpty());
            assertEquals("오른쪽 이동: " + WAITING_KEY_TEXT, getRowText(view, MOVE_RIGHT_ROW));
        });
    }

    @Test
    void cancelKeyCaptureWithEscape() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();
            moveToRow(view, MOVE_LEFT_ROW);
            pressKey(view, KeyCode.ENTER);

            // when
            pressKey(view, KeyCode.ESCAPE);

            // then: 값은 그대로이고 화면도 닫히지 않는다
            assertEquals("왼쪽 이동: LEFT", getRowText(view, MOVE_LEFT_ROW));
            assertTrue(changes.isEmpty());
            assertEquals(0, backCalls);
        });
    }

    @Test
    void rejectKeyUsedByOtherCommand() throws Exception {
        runOnFxThread(() -> {
            // given: 오른쪽 이동이 RIGHT 를 쓰고 있다
            JavaFxSettingsView view = createView();
            moveToRow(view, MOVE_LEFT_ROW);
            pressKey(view, KeyCode.ENTER);

            // when
            pressKey(view, KeyCode.RIGHT);

            // then: 배정되지 않고, 충돌한 명령 이름을 안내하며, 입력 대기는 유지된다
            assertTrue(changes.isEmpty());
            assertEquals("'오른쪽 이동'에 이미 배정된 키입니다", getMessageLabel(view).getText());
            assertEquals("왼쪽 이동: " + WAITING_KEY_TEXT, getRowText(view, MOVE_LEFT_ROW));
        });
    }

    @Test
    void markConflictedRowInRed() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();
            moveToRow(view, MOVE_LEFT_ROW);
            pressKey(view, KeyCode.ENTER);

            // when
            pressKey(view, KeyCode.RIGHT);

            // then
            assertTrue(getRowLabel(view, MOVE_LEFT_ROW).getStyle().contains("red"));
            assertTrue(getMessageLabel(view).getStyle().contains("red"));
        });
    }

    @Test
    void assignAnotherKeyAfterConflictAndClearMessage() throws Exception {
        runOnFxThread(() -> {
            // given: 충돌로 거부된 직후
            JavaFxSettingsView view = createView();
            moveToRow(view, MOVE_LEFT_ROW);
            pressKey(view, KeyCode.ENTER);
            pressKey(view, KeyCode.RIGHT);

            // when
            pressKey(view, KeyCode.A);

            // then: 배정되고 빨간 표시와 메시지가 사라진다
            assertEquals("왼쪽 이동: A", getRowText(view, MOVE_LEFT_ROW));
            assertEquals("", getMessageLabel(view).getText());
            assertFalse(getRowLabel(view, MOVE_LEFT_ROW).getStyle().contains("red"));
        });
    }

    @Test
    void clearConflictMessageWhenCancelingKeyCapture() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();
            moveToRow(view, MOVE_LEFT_ROW);
            pressKey(view, KeyCode.ENTER);
            pressKey(view, KeyCode.RIGHT);

            // when
            pressKey(view, KeyCode.ESCAPE);

            // then
            assertEquals("", getMessageLabel(view).getText());
            assertEquals("왼쪽 이동: LEFT", getRowText(view, MOVE_LEFT_ROW));
        });
    }

    @Test
    void notNotifyWhenAssigningCommandsOwnCurrentKey() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();
            moveToRow(view, MOVE_LEFT_ROW);
            pressKey(view, KeyCode.ENTER);

            // when: 현재 키 LEFT 를 다시 누른다
            pressKey(view, KeyCode.LEFT);

            // then: 충돌도 변경도 아니다
            assertTrue(changes.isEmpty());
            assertEquals("", getMessageLabel(view).getText());
            assertEquals("왼쪽 이동: LEFT", getRowText(view, MOVE_LEFT_ROW));
        });
    }

    @Test
    void askConfirmationBeforeResettingDefaults() throws Exception {
        runOnFxThread(() -> {
            // given
            GameSettings changed = new GameSettings(WindowSize.LARGE, GameSettings.getDefaultKeyBindings(), true);
            JavaFxSettingsView view = createView(changed);
            moveToRow(view, RESET_DEFAULTS_ROW);

            // when
            pressKey(view, KeyCode.ENTER);

            // then: 아직 되돌리지 않고 확인 문구만 보여준다
            assertEquals(RESET_DEFAULTS_CONFIRMATION_TEXT, getRowText(view, RESET_DEFAULTS_ROW));
            assertTrue(changes.isEmpty());
        });
    }

    @Test
    void resetDefaultsWhenConfirmed() throws Exception {
        runOnFxThread(() -> {
            // given: 기본값이 아닌 설정으로 시작한 상태
            GameSettings changed = new GameSettings(WindowSize.LARGE, GameSettings.getDefaultKeyBindings(), true);
            JavaFxSettingsView view = createView(changed);
            moveToRow(view, RESET_DEFAULTS_ROW);
            pressKey(view, KeyCode.ENTER);

            // when
            pressKey(view, KeyCode.Y);

            // then
            assertEquals(1, changes.size());
            assertEquals(WindowSize.MEDIUM, getLastChange().getWindowSize());
            assertFalse(getLastChange().isColorBlindMode());
            assertEquals(GameSettings.getDefaultKeyBindings(), getLastChange().getKeyBindings());
            assertEquals("화면 크기: 보통", getRowText(view, WINDOW_SIZE_ROW));
            assertEquals("기본값으로 되돌리기", getRowText(view, RESET_DEFAULTS_ROW));
            assertEquals(DEFAULTS_RESET_MESSAGE, getMessageLabel(view).getText());
        });
    }

    @Test
    void cancelResetDefaultsWithEscape() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();
            moveToRow(view, RESET_DEFAULTS_ROW);
            pressKey(view, KeyCode.ENTER);

            // when
            pressKey(view, KeyCode.ESCAPE);

            // then: 되돌리지 않고 화면도 닫히지 않는다
            assertTrue(changes.isEmpty());
            assertEquals("기본값으로 되돌리기", getRowText(view, RESET_DEFAULTS_ROW));
            assertEquals(0, backCalls);
        });
    }

    @Test
    void cancelResetDefaultsWithEnterToProtectAgainstKeyRepeat() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();
            moveToRow(view, RESET_DEFAULTS_ROW);
            pressKey(view, KeyCode.ENTER);

            // when: Enter 를 길게 눌러 반복 입력이 들어온 경우
            pressKey(view, KeyCode.ENTER);

            // then: 확인 키(Y)가 아니므로 취소된다
            assertTrue(changes.isEmpty());
            assertEquals("기본값으로 되돌리기", getRowText(view, RESET_DEFAULTS_ROW));
        });
    }

    @Test
    void askConfirmationBeforeResettingScoreboard() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();
            moveToRow(view, RESET_SCOREBOARD_ROW);

            // when
            pressKey(view, KeyCode.ENTER);

            // then
            assertEquals(RESET_SCOREBOARD_CONFIRMATION_TEXT, getRowText(view, RESET_SCOREBOARD_ROW));
            assertEquals(0, resetScoreboardCalls);
        });
    }

    @Test
    void resetScoreboardWhenConfirmed() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();
            moveToRow(view, RESET_SCOREBOARD_ROW);
            pressKey(view, KeyCode.ENTER);

            // when
            pressKey(view, KeyCode.Y);

            // then
            assertEquals(1, resetScoreboardCalls);
            assertEquals(SCOREBOARD_RESET_MESSAGE, getMessageLabel(view).getText());
            assertEquals("스코어보드 초기화", getRowText(view, RESET_SCOREBOARD_ROW));
            assertTrue(changes.isEmpty());
        });
    }

    @Test
    void cancelResetScoreboardWithEscape() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();
            moveToRow(view, RESET_SCOREBOARD_ROW);
            pressKey(view, KeyCode.ENTER);

            // when
            pressKey(view, KeyCode.ESCAPE);

            // then
            assertEquals(0, resetScoreboardCalls);
            assertEquals(0, backCalls);
            assertEquals("스코어보드 초기화", getRowText(view, RESET_SCOREBOARD_ROW));
        });
    }

    @Test
    void cancelResetScoreboardWithEnterToProtectAgainstKeyRepeat() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();
            moveToRow(view, RESET_SCOREBOARD_ROW);
            pressKey(view, KeyCode.ENTER);

            // when
            pressKey(view, KeyCode.ENTER);

            // then
            assertEquals(0, resetScoreboardCalls);
        });
    }

    @Test
    void requireConfirmationAgainAfterCancelOrReset() throws Exception {
        runOnFxThread(() -> {
            // given: 한 번 초기화를 마친 상태
            JavaFxSettingsView view = createView();
            moveToRow(view, RESET_SCOREBOARD_ROW);
            pressKey(view, KeyCode.ENTER);
            pressKey(view, KeyCode.Y);

            // when: Y 만 다시 눌러 본다
            pressKey(view, KeyCode.Y);

            // then: 확인 없이 다시 초기화되지 않는다
            assertEquals(1, resetScoreboardCalls);
        });
    }

    @Test
    void clearMessageWhenMovingSelection() throws Exception {
        runOnFxThread(() -> {
            // given: 안내 메시지가 떠 있는 상태
            JavaFxSettingsView view = createView();
            moveToRow(view, RESET_SCOREBOARD_ROW);
            pressKey(view, KeyCode.ENTER);
            pressKey(view, KeyCode.Y);

            // when
            pressKey(view, KeyCode.DOWN);

            // then
            assertEquals("", getMessageLabel(view).getText());
        });
    }

    @Test
    void goBackWithEnterOnBackRow() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();
            moveToRow(view, BACK_ROW);

            // when
            pressKey(view, KeyCode.ENTER);

            // then
            assertEquals(1, backCalls);
        });
    }

    @Test
    void goBackWithEscape() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();

            // when
            pressKey(view, KeyCode.ESCAPE);

            // then
            assertEquals(1, backCalls);
        });
    }

    @Test
    void ignoreUnrelatedKeys() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxSettingsView view = createView();

            // when
            pressKey(view, KeyCode.A);

            // then
            assertTrue(changes.isEmpty());
            assertEquals(0, backCalls);
            assertTrue(isSelected(view, WINDOW_SIZE_ROW));
        });
    }

    @Test
    void showSettingsPassedToConstructor() throws Exception {
        runOnFxThread(() -> {
            // given: 기본값이 아닌 저장된 설정
            GameSettings saved = new GameSettings(WindowSize.LARGE, GameSettings.getDefaultKeyBindings(), true);

            // when
            JavaFxSettingsView view = createView(saved);

            // then
            assertEquals("화면 크기: 크게", getRowText(view, WINDOW_SIZE_ROW));
            assertEquals("색맹 모드: 켜짐", getRowText(view, COLOR_BLIND_ROW));
        });
    }

    private JavaFxSettingsView createView() {
        return createView(GameSettings.defaults());
    }

    private JavaFxSettingsView createView(GameSettings settings) {
        return new JavaFxSettingsView(settings, changes::add,
                () -> resetScoreboardCalls++, () -> backCalls++);
    }

    private GameSettings getLastChange() {
        return changes.get(changes.size() - 1);
    }

    private void moveToRow(JavaFxSettingsView view, int row) {
        for (int i = 0; i < row; i++) {
            pressKey(view, KeyCode.DOWN);
        }
    }

    private Label getTitleLabel(JavaFxSettingsView view) {
        return (Label) getRoot(view).getChildren().get(0);
    }

    private Label getRowLabel(JavaFxSettingsView view, int row) {
        return (Label) getRoot(view).getChildren().get(row + 1);
    }

    private Label getMessageLabel(JavaFxSettingsView view) {
        List<?> children = getRoot(view).getChildren();
        assertEquals(ROW_COUNT + 2, children.size());
        return (Label) children.get(children.size() - 1);
    }

    private String getRowText(JavaFxSettingsView view, int row) {
        String text = getRowLabel(view, row).getText();
        String marker = text.startsWith(SELECTED_MARKER) ? SELECTED_MARKER : UNSELECTED_MARKER;
        return text.substring(marker.length());
    }

    private boolean isSelected(JavaFxSettingsView view, int row) {
        return getRowLabel(view, row).getText().startsWith(SELECTED_MARKER);
    }

    private VBox getRoot(JavaFxSettingsView view) {
        return (VBox) view.getScene().getRoot();
    }

    private void pressKey(JavaFxSettingsView view, KeyCode key) {
        fireKeyEvent(view, KeyEvent.KEY_PRESSED, key);
    }

    private void releaseKey(JavaFxSettingsView view, KeyCode key) {
        fireKeyEvent(view, KeyEvent.KEY_RELEASED, key);
    }

    private void fireKeyEvent(JavaFxSettingsView view, EventType<KeyEvent> type, KeyCode key) {
        KeyEvent event = new KeyEvent(
                type,
                "",
                "",
                key,
                false,
                false,
                false,
                false
        );

        Event.fireEvent(view.getScene().getRoot(), event);
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
