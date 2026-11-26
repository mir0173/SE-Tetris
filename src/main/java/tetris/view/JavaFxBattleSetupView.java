package tetris.view;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tetris.battle.BattleMode;
import tetris.battle.BattleSettings;
import tetris.model.GameCommand;
import tetris.model.GameKind;
import tetris.model.Difficulty;
import tetris.settings.BattleKeyBindings;
import tetris.settings.GameSettings;

// AI-assisted code start
/** 로컬 대전 모드 선택 화면. P2P 대기실의 대전 설정 칸(모드, 난이도, 제한시간)과 같은 구성이다. */
public final class JavaFxBattleSetupView {
    private static final double WIDTH = 620;
    private static final double HEIGHT = 650;
    private static final double SPACING = 12;
    private static final double PADDING = 24;
    private static final int DURATION_STEP_SECONDS = 10;
    private static final String KEY_GUIDE_TITLE = "대전 키";
    private static final String MOVE_LEFT_RIGHT_LABEL = "좌우 이동";
    private static final String ROTATE_CLOCKWISE_LABEL = "시계 방향 회전";
    private static final String SOFT_DROP_LABEL = "소프트 드롭";
    private static final String HARD_DROP_LABEL = "하드 드롭";
    private static final String HOLD_LABEL = "홀드";
    private static final String ROTATE_COUNTERCLOCKWISE_LABEL = "반시계 방향 회전";

    private final ComboBox<GameKind> gameBox = new ComboBox<>();
    private final ComboBox<BattleMode> modeBox = new ComboBox<>();
    private final ComboBox<Difficulty> difficultyBox = new ComboBox<>();
    private final Spinner<Integer> durationSpinner = new Spinner<>(
            BattleSettings.MIN_TIME_LIMIT_SECONDS,
            BattleSettings.MAX_TIME_LIMIT_SECONDS,
            BattleSettings.DEFAULT_TIME_LIMIT_SECONDS,
            DURATION_STEP_SECONDS);
    private final Button startButton = new Button("게임 시작");
    private final Button keySettingsButton = new Button("대전 키 설정");
    private final Button backButton = new Button("뒤로");
    private final Consumer<BattleSettings> onStart;
    private final Runnable onBack;
    private final Scene scene;

    /**
     * 모드 선택 화면을 만든다.
     *
     * @param settings          현재 설정 (난이도 처음 값과 대전 키 안내에 사용)
     * @param onStart           게임 시작 시 고른 대전 설정을 받을 동작
     * @param onOpenKeySettings '대전 키 설정' 버튼을 눌렀을 때 실행할 동작
     * @param onBack            뒤로 갈 때 실행할 동작
     */
    public JavaFxBattleSetupView(GameSettings settings, Consumer<BattleSettings> onStart, Runnable onOpenKeySettings,
            Runnable onBack) {
        Objects.requireNonNull(settings, "settings");
        this.onStart = Objects.requireNonNull(onStart, "onStart");
        Objects.requireNonNull(onOpenKeySettings, "onOpenKeySettings");
        this.onBack = Objects.requireNonNull(onBack, "onBack");

        modeBox.setId("battle-mode");
        gameBox.setId("battle-game");
        gameBox.getItems().setAll(GameKind.values());
        gameBox.setValue(GameKind.TETRIS);
        difficultyBox.setId("battle-difficulty");
        durationSpinner.setId("battle-duration");
        startButton.setId("battle-start");
        keySettingsButton.setId("battle-key-settings");
        backButton.setId("battle-setup-back");

        modeBox.getItems().setAll(BattleMode.values());
        difficultyBox.getItems().setAll(Difficulty.values());
        modeBox.setValue(BattleMode.NORMAL);
        difficultyBox.setValue(settings.getDifficulty());
        gameBox.valueProperty().addListener((ignored, previous, kind) -> {
            BattleMode selected = modeBox.getValue();
            var available = BattleSettings.modesFor(kind);
            modeBox.getItems().setAll(available);
            modeBox.setValue(selected != null && available.contains(selected) ? selected : BattleMode.NORMAL);
        });
        difficultyBox.disableProperty().bind(gameBox.valueProperty().isEqualTo(GameKind.PUYO));

        durationSpinner.disableProperty().bind(
                modeBox.valueProperty().isNotEqualTo(BattleMode.TIME_LIMIT));

        GridPane gridPane = new GridPane();
        gridPane.setHgap(SPACING);
        gridPane.setVgap(SPACING);
        gridPane.addRow(0, new Label("대전 모드"), modeBox);
        gridPane.addRow(1, new Label("난이도"), difficultyBox);
        gridPane.addRow(2, new Label("제한시간(초)"), durationSpinner);
        gridPane.addRow(3, new Label("게임 종류"), gameBox);

        Label keyGuideTitle = new Label(KEY_GUIDE_TITLE);
        GridPane keyGuide = createKeyGuide(settings.getBattleKeyBindings());
        keyGuide.setId("battle-key-guide");

        startButton.setOnAction(event -> startBattle());
        keySettingsButton.setOnAction(event -> onOpenKeySettings.run());
        backButton.setOnAction(event -> onBack.run());

        HBox buttonBox = new HBox(SPACING, startButton, keySettingsButton, backButton);
        VBox root = new VBox(SPACING, new Label("로컬 대전"), gridPane,
                keyGuideTitle, keyGuide, buttonBox);
        root.setPadding(new Insets(PADDING));
        scene = new Scene(root, WIDTH, HEIGHT);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    }

    public Scene getScene() {
        return scene;
    }

    public void focusStartButton() {
        startButton.requestFocus();
    }

    private void startBattle() {
        BattleMode mode = modeBox.getValue();
        Difficulty difficulty = difficultyBox.getValue();
        int timeLimitSeconds = mode == BattleMode.TIME_LIMIT
                ? durationSpinner.getValue()
                : 0;
        onStart.accept(new BattleSettings(mode, difficulty, timeLimitSeconds, gameBox.getValue()));
    }

    private void handleKeyPressed(KeyEvent event) {
        if (modeBox.isShowing() || difficultyBox.isShowing() || gameBox.isShowing()) {
            return;
        }
        if (event.getCode() == KeyCode.ENTER) {
            event.consume();
            // 다른 버튼에 키보드 포커스가 있으면 그 버튼을 실행하고, 아니면 게임을 시작한다
            Node focusOwner = scene.getFocusOwner();
            if (focusOwner == keySettingsButton || focusOwner == backButton) {
                ((Button) focusOwner).fire();
            } else {
                startBattle();
            }
        } else if (event.getCode() == KeyCode.ESCAPE) {
            event.consume();
            onBack.run();
        }
    }

    private GridPane createKeyGuide(BattleKeyBindings battleKeyBindings) {
        Map<GameCommand, String> firstKeys = battleKeyBindings.getFirstPlayerKeys();
        Map<GameCommand, String> secondKeys = battleKeyBindings.getSecondPlayerKeys();

        GridPane keyGuide = new GridPane();
        keyGuide.setHgap(SPACING);
        keyGuide.setVgap(SPACING);
        keyGuide.addRow(0, new Label(""), new Label("1P"), new Label("2P"));
        keyGuide.addRow(1, new Label(MOVE_LEFT_RIGHT_LABEL),
                new Label(firstKeys.get(GameCommand.MOVE_LEFT) + " / " + firstKeys.get(GameCommand.MOVE_RIGHT)),
                new Label(secondKeys.get(GameCommand.MOVE_LEFT) + " / " + secondKeys.get(GameCommand.MOVE_RIGHT)));
        keyGuide.addRow(2, new Label(ROTATE_CLOCKWISE_LABEL),
                new Label(firstKeys.get(GameCommand.ROTATE_CLOCKWISE)),
                new Label(secondKeys.get(GameCommand.ROTATE_CLOCKWISE)));
        keyGuide.addRow(3, new Label(SOFT_DROP_LABEL),
                new Label(firstKeys.get(GameCommand.SOFT_DROP)),
                new Label(secondKeys.get(GameCommand.SOFT_DROP)));
        keyGuide.addRow(4, new Label(HARD_DROP_LABEL),
                new Label(firstKeys.get(GameCommand.HARD_DROP)),
                new Label(secondKeys.get(GameCommand.HARD_DROP)));
        keyGuide.addRow(5, new Label(HOLD_LABEL),
                new Label(firstKeys.get(GameCommand.HOLD)),
                new Label(secondKeys.get(GameCommand.HOLD)));
        keyGuide.addRow(6, new Label(ROTATE_COUNTERCLOCKWISE_LABEL),
                new Label(firstKeys.get(GameCommand.ROTATE_COUNTERCLOCKWISE)),
                new Label(secondKeys.get(GameCommand.ROTATE_COUNTERCLOCKWISE)));
        return keyGuide;
    }
}
// AI-assisted code end
