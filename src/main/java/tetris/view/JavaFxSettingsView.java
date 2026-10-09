package tetris.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Duration;

import tetris.model.GameCommand;
import tetris.settings.GameSettings;
import tetris.settings.KeyBindingValidator;
import tetris.settings.WindowSize;

// AI-assisted code start
/**
 * 설정 화면. 키보드만으로 화면 크기, 색맹 모드, 키 설정을 바꾸고
 * 기본값 복원과 스코어보드 초기화를 할 수 있다.
 * 설정이 바뀔 때마다 새 설정을 onChange 로 알린다.
 */
public class JavaFxSettingsView {

    private static final double SETTINGS_WIDTH = 500;
    private static final double SETTINGS_HEIGHT = 600;
    private static final double ROOT_SPACING = 10;
    private static final double ROOT_PADDING = 25;
    private static final double ROW_FONT_SIZE = 18;
    private static final double SHAKE_DISTANCE = 8;
    private static final double SHAKE_STEP_MILLIS = 50;
    private static final int SHAKE_CYCLE_COUNT = 6;
    private static final int NO_CONFIRMING_ROW = -1;
    private static final KeyCode CONFIRM_KEY = KeyCode.Y;
    private static final KeyCode CAPTURE_START_KEY = KeyCode.ENTER;

    private static final List<GameCommand> KEY_COMMANDS = List.of(
        GameCommand.MOVE_LEFT,
        GameCommand.MOVE_RIGHT,
        GameCommand.SOFT_DROP,
        GameCommand.HARD_DROP,
        GameCommand.ROTATE_CLOCKWISE,
        GameCommand.ROTATE_COUNTERCLOCKWISE,
        GameCommand.HOLD,
        GameCommand.PAUSE,
        GameCommand.QUIT_GAME
    );
    private static final Map<GameCommand, String> COMMAND_TEXTS = Map.of(
        GameCommand.MOVE_LEFT, "왼쪽 이동",
        GameCommand.MOVE_RIGHT, "오른쪽 이동",
        GameCommand.SOFT_DROP, "아래로 이동",
        GameCommand.HARD_DROP, "하드 드롭",
        GameCommand.ROTATE_CLOCKWISE, "시계 방향 회전",
        GameCommand.ROTATE_COUNTERCLOCKWISE, "반시계 방향 회전",
        GameCommand.HOLD, "홀드",
        GameCommand.PAUSE, "일시정지",
        GameCommand.QUIT_GAME, "게임 종료"
    );

    private static final int WINDOW_SIZE_ROW = 0;
    private static final int COLOR_BLIND_ROW = 1;
    private static final int FIRST_KEY_ROW = 2;
    private static final int RESET_DEFAULTS_ROW = FIRST_KEY_ROW + KEY_COMMANDS.size();
    private static final int RESET_SCOREBOARD_ROW = RESET_DEFAULTS_ROW + 1;
    private static final int BACK_ROW = RESET_SCOREBOARD_ROW + 1;
    private static final int ROW_COUNT = BACK_ROW + 1;

    private static final String TITLE_TEXT = "설정";
    private static final String WINDOW_SIZE_TEXT = "화면 크기: ";
    private static final String COLOR_BLIND_TEXT = "색맹 모드: ";
    private static final String RESET_DEFAULTS_ROW_TEXT = "기본값으로 되돌리기";
    private static final String RESET_DEFAULTS_CONFIRMATION_TEXT =
        "기본값으로 되돌리기: 정말 되돌리려면 Y, 취소는 ESC";
    private static final String RESET_SCOREBOARD_ROW_TEXT = "스코어보드 초기화";
    private static final String RESET_SCOREBOARD_CONFIRMATION_TEXT =
        "스코어보드 초기화: 정말 지우려면 Y, 취소는 ESC";
    private static final String BACK_ROW_TEXT = "메뉴로";
    private static final String SMALL_SIZE_TEXT = "작게";
    private static final String MEDIUM_SIZE_TEXT = "보통";
    private static final String LARGE_SIZE_TEXT = "크게";
    private static final String ON_TEXT = "켜짐";
    private static final String OFF_TEXT = "꺼짐";
    private static final String SELECTED_MARKER = "▶ ";
    private static final String UNSELECTED_MARKER = "   ";
    private static final String KEY_SEPARATOR = ": ";
    private static final String WAITING_KEY_TEXT = "키를 누르세요...";
    private static final String UNASSIGNED_KEY_TEXT = "없음";
    private static final String EMPTY_TEXT = "";
    private static final String CONFLICT_MESSAGE_FORMAT = "'%s'에 이미 배정된 키입니다";
    private static final String DEFAULTS_RESET_MESSAGE = "기본값으로 되돌렸습니다";
    private static final String SCOREBOARD_RESET_MESSAGE = "스코어보드를 초기화했습니다";
    private static final String CONFLICT_STYLE = "-fx-text-fill: red;";
    private static final String NORMAL_STYLE = "";

    private final Scene scene;
    private final List<Label> rowLabels = new ArrayList<>();
    private final Label messageLabel = new Label();
    private final KeyBindingValidator keyBindingValidator = new KeyBindingValidator();
    private final Consumer<GameSettings> onChange;
    private final Runnable onResetScoreboard;
    private final Runnable onBack;

    private GameSettings settings;
    private int selectedIndex = 0;
    private int confirmingRow = NO_CONFIRMING_ROW;
    private boolean waitingForKey = false;
    private boolean captureStartKeyHeld = false;
    private TranslateTransition shakeAnimation;

    /**
     * 설정 화면을 구성한다.
     *
     * @param settings 화면에 표시할 현재 설정
     * @param onChange 설정이 바뀔 때마다 실행할 동작 (새 설정을 받는다)
     * @param onResetScoreboard 스코어보드를 초기화할 때 실행할 동작
     * @param onBack 메뉴로 돌아갈 때 실행할 동작
     */
    public JavaFxSettingsView(
        GameSettings settings,
        Consumer<GameSettings> onChange,
        Runnable onResetScoreboard,
        Runnable onBack
    ) {
        this.settings = settings;
        this.onChange = onChange;
        this.onResetScoreboard = onResetScoreboard;
        this.onBack = onBack;

        Label title = new Label(TITLE_TEXT);

        VBox root = new VBox(ROOT_SPACING);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(ROOT_PADDING));
        root.getChildren().add(title);

        for (int i = 0; i < ROW_COUNT; i++) {
            Label rowLabel = new Label();
            rowLabel.setFont(Font.font(ROW_FONT_SIZE));
            rowLabels.add(rowLabel);
            root.getChildren().add(rowLabel);
        }

        root.getChildren().add(messageLabel);

        refreshRows();

        scene = new Scene(root, SETTINGS_WIDTH, SETTINGS_HEIGHT);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKey);
        scene.addEventHandler(KeyEvent.KEY_RELEASED, this::handleKeyReleased);
    }

    public Scene getScene() {
        return scene;
    }

    /**
     * 키 입력을 처리한다. 재확인 중이거나 키 입력 대기 중이면 그 처리가 먼저다.
     */
    private void handleKey(KeyEvent event) {
        if (confirmingRow != NO_CONFIRMING_ROW) {
            handleConfirmation(event);
            event.consume();
            return;
        }

        if (waitingForKey) {
            captureKey(event);
            event.consume();
            return;
        }

        switch (event.getCode()) {
            case UP -> moveSelection(-1);
            case DOWN -> moveSelection(1);
            case LEFT -> changeSelectedValue(-1);
            case RIGHT -> changeSelectedValue(1);
            case ENTER -> activateSelectedRow();
            case ESCAPE -> onBack.run();
            default -> {
                return;
            }
        }

        event.consume();
    }

    private void handleKeyReleased(KeyEvent event) {
        if (event.getCode() == CAPTURE_START_KEY) {
            captureStartKeyHeld = false;
        }
    }

    private void moveSelection(int direction) {
        clearConflict();
        selectedIndex = Math.floorMod(selectedIndex + direction, ROW_COUNT);
        refreshRows();
    }

    private void changeSelectedValue(int direction) {
        if (selectedIndex == WINDOW_SIZE_ROW) {
            changeWindowSize(direction);
        } else if (selectedIndex == COLOR_BLIND_ROW) {
            toggleColorBlindMode();
        }
    }

    private void activateSelectedRow() {
        if (selectedIndex == WINDOW_SIZE_ROW) {
            changeWindowSize(1);
        } else if (selectedIndex == COLOR_BLIND_ROW) {
            toggleColorBlindMode();
        } else if (isKeyRow(selectedIndex)) {
            startKeyCapture();
        } else if (selectedIndex == RESET_DEFAULTS_ROW || selectedIndex == RESET_SCOREBOARD_ROW) {
            startConfirmation();
        } else if (selectedIndex == BACK_ROW) {
            onBack.run();
        }
    }

    private void changeWindowSize(int direction) {
        WindowSize[] sizes = WindowSize.values();
        int nextIndex = Math.floorMod(settings.getWindowSize().ordinal() + direction, sizes.length);
        updateSettings(new GameSettings(sizes[nextIndex], settings.getKeyBindings(), settings.isColorBlindMode()));
    }

    private void toggleColorBlindMode() {
        updateSettings(new GameSettings(settings.getWindowSize(), settings.getKeyBindings(), !settings.isColorBlindMode()));
    }

    private void changeKeyBinding(GameCommand command, String keyName) {
        if (keyName.equals(settings.getKeyBindings().get(command))) {
            refreshRows();
            return;
        }

        Map<GameCommand, String> newKeyBindings = new HashMap<>(settings.getKeyBindings());
        newKeyBindings.put(command, keyName);
        updateSettings(new GameSettings(settings.getWindowSize(), newKeyBindings, settings.isColorBlindMode()));
    }

    private void updateSettings(GameSettings newSettings) {
        settings = newSettings;
        refreshRows();
        onChange.accept(settings);
    }

    private void startKeyCapture() {
        waitingForKey = true;
        captureStartKeyHeld = true;
        refreshRows();
    }

    /**
     * 입력 대기 중 눌린 키를 선택한 명령에 배정한다.
     * ESC 는 취소이고, 다른 명령이 쓰는 키는 거부하며 입력 대기를 유지한다.
     */
    private void captureKey(KeyEvent event) {
        if (captureStartKeyHeld && event.getCode() == CAPTURE_START_KEY) {
            return;
        }

        if (event.getCode() == KeyCode.ESCAPE) {
            waitingForKey = false;
            clearConflict();
            refreshRows();
            return;
        }

        GameCommand command = KEY_COMMANDS.get(selectedIndex - FIRST_KEY_ROW);
        String keyName = event.getCode().name();
        Optional<GameCommand> conflict = keyBindingValidator.findConflict(settings.getKeyBindings(), command, keyName);

        if (conflict.isPresent()) {
            showConflict(conflict.get());
            return;
        }

        waitingForKey = false;
        clearConflict();
        changeKeyBinding(command, keyName);
    }

    private void startConfirmation() {
        confirmingRow = selectedIndex;
        refreshRows();
    }

    /**
     * 재확인 중 눌린 키를 처리한다.
     * 확인 키면 해당 행의 동작을 실행하고, 그 외의 키는 모두 취소다.
     */
    private void handleConfirmation(KeyEvent event) {
        int confirmedRow = confirmingRow;
        confirmingRow = NO_CONFIRMING_ROW;

        if (event.getCode() == CONFIRM_KEY) {
            if (confirmedRow == RESET_DEFAULTS_ROW) {
                updateSettings(GameSettings.defaults());
                showMessage(DEFAULTS_RESET_MESSAGE, NORMAL_STYLE);
            } else if (confirmedRow == RESET_SCOREBOARD_ROW) {
                onResetScoreboard.run();
                showMessage(SCOREBOARD_RESET_MESSAGE, NORMAL_STYLE);
            }
        }

        refreshRows();
    }

    private void refreshRows() {
        for (int i = 0; i < rowLabels.size(); i++) {
            String marker = i == selectedIndex ? SELECTED_MARKER : UNSELECTED_MARKER;
            rowLabels.get(i).setText(marker + createRowText(i));
        }
    }

    private String createRowText(int rowIndex) {
        if (rowIndex == WINDOW_SIZE_ROW) {
            return WINDOW_SIZE_TEXT + getWindowSizeText(settings.getWindowSize());
        }
        if (rowIndex == COLOR_BLIND_ROW) {
            return COLOR_BLIND_TEXT + (settings.isColorBlindMode() ? ON_TEXT : OFF_TEXT);
        }
        if (isKeyRow(rowIndex)) {
            GameCommand command = KEY_COMMANDS.get(rowIndex - FIRST_KEY_ROW);
            return createKeyRowText(command, rowIndex == selectedIndex && waitingForKey);
        }
        if (rowIndex == RESET_DEFAULTS_ROW) {
            return confirmingRow == rowIndex
                ? RESET_DEFAULTS_CONFIRMATION_TEXT
                : RESET_DEFAULTS_ROW_TEXT;
        }
        if (rowIndex == RESET_SCOREBOARD_ROW) {
            return confirmingRow == rowIndex
                ? RESET_SCOREBOARD_CONFIRMATION_TEXT
                : RESET_SCOREBOARD_ROW_TEXT;
        }

        return BACK_ROW_TEXT;
    }

    private String createKeyRowText(GameCommand command, boolean waiting) {
        String keyText = waiting
            ? WAITING_KEY_TEXT
            : settings.getKeyBindings().getOrDefault(command, UNASSIGNED_KEY_TEXT);

        return getCommandText(command) + KEY_SEPARATOR + keyText;
    }

    private String getWindowSizeText(WindowSize windowSize) {
        return switch (windowSize) {
            case SMALL -> SMALL_SIZE_TEXT;
            case MEDIUM -> MEDIUM_SIZE_TEXT;
            case LARGE -> LARGE_SIZE_TEXT;
        };
    }

    private String getCommandText(GameCommand command) {
        return COMMAND_TEXTS.getOrDefault(command, command.name());
    }

    private boolean isKeyRow(int rowIndex) {
        return rowIndex >= FIRST_KEY_ROW && rowIndex < RESET_DEFAULTS_ROW;
    }

    private void showConflict(GameCommand conflictCommand) {
        showMessage(
            String.format(CONFLICT_MESSAGE_FORMAT, getCommandText(conflictCommand)),
            CONFLICT_STYLE
        );

        Label rowLabel = rowLabels.get(selectedIndex);
        rowLabel.setStyle(CONFLICT_STYLE);
        shakeRow(rowLabel);
    }

    private void clearConflict() {
        showMessage(EMPTY_TEXT, NORMAL_STYLE);

        for (Label rowLabel : rowLabels) {
            rowLabel.setStyle(NORMAL_STYLE);
        }
    }

    private void showMessage(String text, String style) {
        messageLabel.setText(text);
        messageLabel.setStyle(style);
    }

    private void shakeRow(Label rowLabel) {
        if (shakeAnimation != null) {
            shakeAnimation.stop();
        }

        shakeAnimation = new TranslateTransition(Duration.millis(SHAKE_STEP_MILLIS), rowLabel);
        shakeAnimation.setFromX(-SHAKE_DISTANCE);
        shakeAnimation.setToX(SHAKE_DISTANCE);
        shakeAnimation.setCycleCount(SHAKE_CYCLE_COUNT);
        shakeAnimation.setAutoReverse(true);
        shakeAnimation.setOnFinished(event -> rowLabel.setTranslateX(0));
        shakeAnimation.play();
    }
}
// AI-assisted code end
