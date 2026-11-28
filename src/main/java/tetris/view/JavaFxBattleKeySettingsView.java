package tetris.view;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import tetris.model.GameCommand;
import tetris.settings.BattleKeyBindings;
import tetris.settings.GameSettings;

// AI-assisted code start
/**
 * 대전 키 설정 화면. 설정 화면과 같은 조작으로 로컬 대전 키(1P 7개, 2P 7개, 공용 2개)를 바꾸고
 * 대전 키만 기본값으로 되돌릴 수 있다. 1인 게임 키와는 따로 관리하며, 키가 바뀔 때마다 새 설정을 onChange로 알린다.
 */
public class JavaFxBattleKeySettingsView {

    private static final double VIEW_WIDTH = 500;
    private static final double VIEW_HEIGHT = 660; // AI-assisted (redesign) 키 안내 줄 자리만큼 키움
    private static final double ROOT_SPACING = 4;
    private static final double ROOT_PADDING = 20;
    private static final double TITLE_FONT_SIZE = 20;
    private static final double ROW_FONT_SIZE = 16;
    private static final long CLICK_COOLDOWN_NANOS = 150_000_000L;
    private static final double SHAKE_DISTANCE = 8;
    private static final double SHAKE_STEP_MILLIS = 50;
    private static final int SHAKE_CYCLE_COUNT = 6;
    private static final KeyCode CONFIRM_KEY = KeyCode.Y;
    private static final KeyCode CAPTURE_START_KEY = KeyCode.ENTER;

    // 모드 선택 화면의 키 안내 표와 같은 순서 (좌우 이동만 두 줄로 나눔)
    private static final List<GameCommand> PLAYER_COMMANDS = List.of(
        GameCommand.MOVE_LEFT,
        GameCommand.MOVE_RIGHT,
        GameCommand.ROTATE_CLOCKWISE,
        GameCommand.SOFT_DROP,
        GameCommand.HARD_DROP,
        GameCommand.HOLD,
        GameCommand.ROTATE_COUNTERCLOCKWISE
    );
    private static final List<GameCommand> SHARED_COMMANDS = List.of(GameCommand.PAUSE, GameCommand.QUIT_GAME);
    private static final Map<GameCommand, String> COMMAND_TEXTS = Map.of(
        GameCommand.MOVE_LEFT, "왼쪽 이동",
        GameCommand.MOVE_RIGHT, "오른쪽 이동",
        GameCommand.ROTATE_CLOCKWISE, "시계 방향 회전",
        GameCommand.SOFT_DROP, "소프트 드롭",
        GameCommand.HARD_DROP, "하드 드롭",
        GameCommand.HOLD, "홀드",
        GameCommand.ROTATE_COUNTERCLOCKWISE, "반시계 방향 회전",
        GameCommand.PAUSE, "일시정지",
        GameCommand.QUIT_GAME, "경기 중단"
    );

    private static final String TITLE_TEXT = "대전 키 설정";
    private static final String ROW_ID_PREFIX = "battle-key-";
    private static final String RESET_ROW_ID = "battle-key-reset";
    private static final String BACK_ROW_ID = "battle-key-back";
    private static final String MESSAGE_ID = "battle-key-message";
    private static final String RESET_ROW_TEXT = "대전 키 기본값으로 되돌리기";
    private static final String RESET_CONFIRMATION_TEXT = "대전 키 기본값으로 되돌리기: 정말 되돌리려면 Y, 취소는 ESC";
    private static final String RESET_MESSAGE = "대전 키를 기본값으로 되돌렸습니다";
    private static final String BACK_ROW_TEXT = "뒤로";
    private static final String SELECTED_MARKER = "▶ ";
    private static final String UNSELECTED_MARKER = "   ";
    private static final String KEY_SEPARATOR = ": ";
    private static final String WAITING_KEY_TEXT = "키를 누르세요...";
    private static final String CONFLICT_MESSAGE_FORMAT = "'%s'에 이미 배정된 키입니다";
    private static final String CONFLICT_STYLE = "-fx-text-fill: red;";
    private static final String NORMAL_STYLE = "";
    private static final String EMPTY_TEXT = "";

    private final Scene scene;
    private final List<KeySlot> keySlots = new ArrayList<>(); // 키 줄 16개 (줄 번호 = 목록 번호)
    private final List<Label> rowLabels = new ArrayList<>(); // 키 줄 16개 + 기본값 줄 + 뒤로 줄
    private final Label messageLabel = new Label();
    private final Consumer<GameSettings> onChange;
    private final Runnable onBack;
    private final int resetRow;
    private final int backRow;

    private GameSettings settings;
    private int selectedIndex = 0;
    private boolean waitingForKey = false;
    private boolean captureStartKeyHeld = false;
    private boolean confirmingReset = false;
    private long lastMouseClickNanos = System.nanoTime() - CLICK_COOLDOWN_NANOS;
    private TranslateTransition shakeAnimation;

    /**
     * 대전 키 설정 화면을 구성한다.
     *
     * @param settings 화면에 표시할 현재 설정
     * @param onChange 대전 키가 바뀔 때마다 실행할 동작 (새 설정을 받는다)
     * @param onBack   뒤로 갈 때 실행할 동작
     */
    public JavaFxBattleKeySettingsView(GameSettings settings, Consumer<GameSettings> onChange, Runnable onBack) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
        this.onBack = Objects.requireNonNull(onBack, "onBack");

        for (GameCommand command : PLAYER_COMMANDS) {
            keySlots.add(new KeySlot(Target.FIRST_PLAYER, command));
        }
        for (GameCommand command : PLAYER_COMMANDS) {
            keySlots.add(new KeySlot(Target.SECOND_PLAYER, command));
        }
        for (GameCommand command : SHARED_COMMANDS) {
            keySlots.add(new KeySlot(Target.SHARED, command));
        }
        resetRow = keySlots.size();
        backRow = resetRow + 1;

        Label title = new Label(TITLE_TEXT);
        title.setFont(Theme.uiFont(FontWeight.BOLD, TITLE_FONT_SIZE)); // AI-assisted (redesign)

        VBox root = new VBox(ROOT_SPACING);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(ROOT_PADDING));
        root.setFocusTraversable(true);
        root.getChildren().add(title);

        for (int row = 0; row <= backRow; row++) {
            Label rowLabel = createRowLabel(row);
            rowLabels.add(rowLabel);
            root.getChildren().add(rowLabel);
        }

        messageLabel.setId(MESSAGE_ID);
        root.getChildren().add(messageLabel);

        refreshRows();

        // AI-assisted code start (redesign)
        KeyHintBar keyHints = new KeyHintBar()
                .add("이동", "UP", "DOWN")
                .add("키 바꾸기", "ENTER")
                .add("취소·뒤로", "ESCAPE");
        BorderPane frame = new BorderPane(root);
        frame.setBottom(keyHints);

        scene = new Scene(frame, VIEW_WIDTH, VIEW_HEIGHT);
        Theme.apply(scene);
        // AI-assisted code end
        scene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKey);
        scene.addEventHandler(KeyEvent.KEY_RELEASED, this::handleKeyReleased);
    }

    public Scene getScene() {
        return scene;
    }

    private Label createRowLabel(int row) {
        Label rowLabel = new Label();
        rowLabel.setId(createRowId(row));
        rowLabel.setFont(Theme.uiFont(ROW_FONT_SIZE)); // AI-assisted (redesign)
        rowLabel.setMaxWidth(Double.MAX_VALUE);
        rowLabel.setAlignment(Pos.CENTER);
        rowLabel.setCursor(Cursor.HAND);
        rowLabel.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            long now = System.nanoTime();
            if (now - lastMouseClickNanos >= CLICK_COOLDOWN_NANOS) {
                lastMouseClickNanos = now;
                handleRowClick(row);
            }
            event.consume();
        });
        return rowLabel;
    }

    private String createRowId(int row) {
        if (row == resetRow) {
            return RESET_ROW_ID;
        }
        if (row == backRow) {
            return BACK_ROW_ID;
        }
        KeySlot slot = keySlots.get(row);
        return ROW_ID_PREFIX + slot.target().getIdText() + "-" + slot.command().name();
    }

    /**
     * 키 입력을 처리한다. 재확인 중이거나 키 입력 대기 중이면 그 처리가 먼저다.
     */
    private void handleKey(KeyEvent event) {
        if (confirmingReset) {
            finishReset(event.getCode() == CONFIRM_KEY);
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
            case ENTER -> activateSelectedRow(true);
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
        selectedIndex = Math.floorMod(selectedIndex + direction, rowLabels.size());
        refreshRows();
    }

    // 클릭한 줄을 선택하고 실행한다. 재확인 중에는 다른 줄을 실행하지 않는다
    private void handleRowClick(int row) {
        if (confirmingReset) {
            return;
        }

        waitingForKey = false;
        captureStartKeyHeld = false;
        clearConflict();

        selectedIndex = row;
        activateSelectedRow(false);
        refreshRows();

        scene.getRoot().requestFocus();
    }

    private void activateSelectedRow(boolean startedByKeyboard) {
        if (isKeyRow(selectedIndex)) {
            waitingForKey = true;
            captureStartKeyHeld = startedByKeyboard;
        } else if (selectedIndex == resetRow) {
            confirmingReset = true;
        } else if (selectedIndex == backRow) {
            onBack.run();
            return;
        }
        refreshRows();
    }

    /**
     * 입력 대기 중 눌린 키를 선택한 칸에 배정한다.
     * ESC는 취소이고, 대전 키 16개 중 다른 칸이 쓰는 키는 거부하며 입력 대기를 유지한다.
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

        KeySlot slot = keySlots.get(selectedIndex);
        String keyName = event.getCode().name();
        Optional<KeySlot> conflict = findConflict(slot, keyName);

        if (conflict.isPresent()) {
            showConflict(conflict.get());
            return;
        }

        waitingForKey = false;
        clearConflict();

        if (keyName.equals(findKey(slot))) {
            refreshRows();
            return;
        }
        changeKey(slot, keyName);
    }

    // 대전 키 16개 중 같은 키를 쓰는 다른 칸을 찾는다 (1인 게임 키와는 비교하지 않는다)
    private Optional<KeySlot> findConflict(KeySlot slot, String keyName) {
        for (KeySlot other : keySlots) {
            if (!other.equals(slot) && keyName.equals(findKey(other))) {
                return Optional.of(other);
            }
        }
        return Optional.empty();
    }

    private void changeKey(KeySlot slot, String keyName) {
        BattleKeyBindings current = settings.getBattleKeyBindings();
        Map<GameCommand, String> firstKeys = new EnumMap<>(current.getFirstPlayerKeys());
        Map<GameCommand, String> secondKeys = new EnumMap<>(current.getSecondPlayerKeys());
        Map<GameCommand, String> sharedKeys = new EnumMap<>(current.getSharedKeys());

        switch (slot.target()) {
            case FIRST_PLAYER -> firstKeys.put(slot.command(), keyName);
            case SECOND_PLAYER -> secondKeys.put(slot.command(), keyName);
            case SHARED -> sharedKeys.put(slot.command(), keyName);
        }

        updateBattleKeys(new BattleKeyBindings(firstKeys, secondKeys, sharedKeys));
    }

    // 대전 키만 바꾼 새 설정을 만들어 알린다. 다른 설정 값은 그대로 둔다
    private void updateBattleKeys(BattleKeyBindings battleKeys) {
        settings = new GameSettings(settings.getWindowSize(), settings.getKeyBindings(),
                settings.isColorBlindMode(), settings.getDifficulty(), battleKeys);
        refreshRows();
        onChange.accept(settings);
    }

    private void finishReset(boolean confirmed) {
        confirmingReset = false;

        if (confirmed) {
            updateBattleKeys(BattleKeyBindings.defaults());
            showMessage(RESET_MESSAGE, NORMAL_STYLE);
        }

        refreshRows();
    }

    private void refreshRows() {
        for (int row = 0; row < rowLabels.size(); row++) {
            String marker = row == selectedIndex ? SELECTED_MARKER : UNSELECTED_MARKER;
            rowLabels.get(row).setText(marker + createRowText(row));
        }
    }

    private String createRowText(int row) {
        if (row == resetRow) {
            return confirmingReset ? RESET_CONFIRMATION_TEXT : RESET_ROW_TEXT;
        }
        if (row == backRow) {
            return BACK_ROW_TEXT;
        }

        KeySlot slot = keySlots.get(row);
        String keyText = row == selectedIndex && waitingForKey ? WAITING_KEY_TEXT : findKey(slot);
        return createSlotName(slot) + KEY_SEPARATOR + keyText;
    }

    private String findKey(KeySlot slot) {
        BattleKeyBindings battleKeys = settings.getBattleKeyBindings();
        return switch (slot.target()) {
            case FIRST_PLAYER -> battleKeys.getFirstPlayerKeys().get(slot.command());
            case SECOND_PLAYER -> battleKeys.getSecondPlayerKeys().get(slot.command());
            case SHARED -> battleKeys.getSharedKeys().get(slot.command());
        };
    }

    // 예: "1P 왼쪽 이동", "공용 일시정지"
    private String createSlotName(KeySlot slot) {
        return slot.target().getDisplayText() + " " + COMMAND_TEXTS.get(slot.command());
    }

    private boolean isKeyRow(int row) {
        return row >= 0 && row < keySlots.size();
    }

    private void showConflict(KeySlot conflictSlot) {
        showMessage(String.format(CONFLICT_MESSAGE_FORMAT, createSlotName(conflictSlot)), CONFLICT_STYLE);

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

    /** 키를 쓰는 대상. 화면에 보일 이름과 노드 id에 쓸 이름을 가진다 */
    private enum Target {
        FIRST_PLAYER("1P", "first"),
        SECOND_PLAYER("2P", "second"),
        SHARED("공용", "shared");

        private final String displayText;
        private final String idText;

        Target(String displayText, String idText) {
            this.displayText = displayText;
            this.idText = idText;
        }

        private String getDisplayText() {
            return displayText;
        }

        private String getIdText() {
            return idText;
        }
    }

    /** 키 칸 하나: 누구의(대상) 어떤 명령인지 */
    private record KeySlot(Target target, GameCommand command) {
    }
}
// AI-assisted code end
