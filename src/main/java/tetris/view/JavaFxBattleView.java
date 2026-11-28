package tetris.view;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontWeight;
import tetris.battle.MatchResult;
import tetris.model.GameCommand;
import tetris.model.GameSnapshot;
import tetris.settings.BattleKeyBindings;
import tetris.settings.GameSettings;

// AI-assisted code start
/**
 * 로컬 2인 대전 화면. P2P 대전 화면(JavaFxP2pGameView)과 같은 구성으로 보드 두 개를 나란히 보여 준다.
 * 키는 창 전체(Scene)에서 받아 BattleKeyRouter로 1P / 2P / 공용 명령으로 나눈다.
 */
public final class JavaFxBattleView {
    private static final double SPACING = 12;
    private static final double PADDING = 12;
    private static final double PLAYER_GAP = 40; // 1P 정보 칸과 2P 보드 사이 간격
    private static final double PLAYER_NAME_FONT_SIZE = 28;
    private static final double PLAYER_NAME_SPACING = 0;

    private final PlayerPanel firstPanel;
    private final PlayerPanel secondPanel;
    private final BattleKeyRouter keyRouter;
    private final Runnable onReturnToMenu;
    private final Label resultLabel = new Label();
    private final Label timeLeft = new Label();
    private final Button back = new Button("메뉴로");
    private final Button rematch = new Button("다시 하기");
    private final Button exit = new Button("게임 종료");
    private final KeyHintBar keyHints = new KeyHintBar(); // 화면 아래 키 안내
    private final Map<GameCommand, String> sharedKeys;
    private final Scene scene;

    private boolean finished; // 결과가 나왔는지
    private boolean pauseKeyHeld; // 일시정지 키를 누르고 있는지

    public JavaFxBattleView(GameSettings settings, Runnable onReturnToMenu, Runnable onRematch, Runnable onExit) {
        Objects.requireNonNull(settings, "settings");
        this.onReturnToMenu = Objects.requireNonNull(onReturnToMenu, "onReturnToMenu");
        Objects.requireNonNull(onRematch, "onRematch");
        Objects.requireNonNull(onExit, "onExit");

        BattleKeyBindings keys = settings.getBattleKeyBindings();
        keyRouter = new BattleKeyRouter(keys);
        sharedKeys = keys.getSharedKeys();
        firstPanel = new PlayerPanel(settings, keys.getFirstPlayerKeys(), keys.getSharedKeys(),
                "battle-first-panel");
        secondPanel = new PlayerPanel(settings, keys.getSecondPlayerKeys(), keys.getSharedKeys(),
                "battle-second-panel");

        resultLabel.setId("battle-result");
        resultLabel.setWrapText(true);
        timeLeft.setId("battle-time-left");
        // 숨겨도 자리는 남겨 둔다(managed 유지). 창 크기를 내용에 맞추므로, 결과가 나올 때 내용이 창 밖으로 밀리지 않게 한다
        for (Label label : new Label[]{resultLabel, timeLeft}) {
            label.visibleProperty().bind(label.textProperty().isNotEmpty());
        }

        back.setId("battle-back");
        rematch.setId("battle-rematch");
        exit.setId("battle-exit");
        back.setOnAction(event -> onReturnToMenu.run());
        rematch.setOnAction(event -> onRematch.run());
        exit.setOnAction(event -> onExit.run());
        // 경기 중 중단은 ESC로만 한다. 버튼 3개는 결과가 나온 뒤에만 보인다 (자리는 처음부터 남겨 둔다)
        for (Button button : new Button[]{rematch, back, exit}) {
            button.setVisible(false);
        }

        double scale = settings.getWindowSize().getScale();
        VBox firstColumn = createPlayerColumn("1P", "battle-first-name", firstPanel, scale);
        VBox secondColumn = createPlayerColumn("2P", "battle-second-name", secondPanel, scale);

        HBox status = new HBox(SPACING * scale, timeLeft, resultLabel); // 남은 시간과 결과를 한 줄에
        status.setAlignment(Pos.CENTER);
        HBox boards = new HBox(PLAYER_GAP * scale, firstColumn, secondColumn);
        boards.setAlignment(Pos.CENTER);
        HBox buttons = new HBox(SPACING * scale, rematch, back, exit);
        buttons.setAlignment(Pos.CENTER);
        VBox root = new VBox(SPACING * scale, status, boards, buttons);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(PADDING * scale));

        showMatchKeyHints();
        BorderPane frame = new BorderPane(root);
        frame.setBottom(keyHints);

        // 창 크기는 정하지 않는다. stage.sizeToScene()이 내용 크기에 맞춘다
        scene = new Scene(frame);
        Theme.apply(scene);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, this::handleKeyReleased);
    }

    public GameView getFirstPlayerView() {
        return firstPanel;
    }

    public GameView getSecondPlayerView() {
        return secondPanel;
    }

    public Scene getScene() {
        return scene;
    }

    /** 경기 결과를 보여 주고, 다시 하기·메뉴로·게임 종료 버튼을 보인다. 이후 게임 키는 무시한다. */
    public void renderResult(MatchResult result) {
        String outcome = switch (result.winner()) {
            case PLAYER_ONE -> "1P 승리";
            case PLAYER_TWO -> "2P 승리";
            case DRAW -> "무승부";
        };
        String reason = switch (result.reason()) {
            case GAME_OVER -> "게임오버";
            case TIME_UP -> "시간 종료";
            case STOPPED -> "기권";
        };
        resultLabel.setText(String.format("%s · %s (1P %d : 2P %d)",
                outcome, reason, result.firstScore(), result.secondScore()));

        finished = true;
        rematch.setVisible(true);
        back.setVisible(true);
        exit.setVisible(true);
        rematch.requestFocus();
        showResultKeyHints();
    }

    /** 시간제한 모드의 남은 시간을 "남은 시간 m:ss"로 보인다. */
    public void renderRemainingSeconds(int seconds) {
        timeLeft.setText(String.format(Locale.ROOT, "남은 시간 %d:%02d", seconds / 60, seconds % 60));
    }

    // 경기 중 안내: 두 사람이 같이 쓰는 키(일시정지, 경기 중단). 각자 조작 키는 모드 선택 화면의 표에 있다
    private void showMatchKeyHints() {
        keyHints.clear()
                .add("일시정지", sharedKeys.get(GameCommand.PAUSE))
                .add("경기 중단", sharedKeys.get(GameCommand.QUIT_GAME));
    }

    // 결과 뒤 안내: 버튼 고르기와 실행
    private void showResultKeyHints() {
        keyHints.clear()
                .add("버튼 이동", "LEFT", "RIGHT")
                .add("선택", "ENTER")
                .add("메뉴로", sharedKeys.get(GameCommand.QUIT_GAME));
    }

    private void handleKeyPressed(KeyEvent event) {
        Optional<BattleKeyRouter.Route> route = keyRouter.findRoute(event.getCode().name());

        if (finished) {
            // 결과 뒤: 게임 키는 무시하고 종료 키만 메뉴로. Enter는 키보드로 고른 버튼을 실행한다.
            // 나머지(방향키 등)는 버튼 사이 포커스 이동에 쓰이도록 그대로 둔다
            if (route.isPresent() && route.get().command() == GameCommand.QUIT_GAME) {
                event.consume();
                onReturnToMenu.run();
            } else if (event.getCode() == KeyCode.ENTER && scene.getFocusOwner() instanceof Button focused) {
                event.consume();
                focused.fire();
            }
            return;
        }

        // 경기 중: 키는 모두 게임 키로만 쓰고 다른 노드로 넘기지 않는다
        event.consume();
        if (route.isEmpty()) {
            return;
        }

        BattleKeyRouter.Route found = route.get();
        switch (found.target()) {
            case FIRST_PLAYER -> firstPanel.sendCommand(found.command());
            case SECOND_PLAYER -> secondPanel.sendCommand(found.command());
            case SHARED -> handleSharedCommand(found.command());
        }
    }

    private void handleSharedCommand(GameCommand command) {
        if (command == GameCommand.QUIT_GAME) {
            onReturnToMenu.run();
            return;
        }

        // 일시정지: 누르고 있는 동안 반복 입력은 무시하고 1P에만 보낸다. 2P는 MatchManager가 같이 멈춘다
        if (pauseKeyHeld) {
            return;
        }
        pauseKeyHeld = true;
        firstPanel.sendCommand(GameCommand.PAUSE);
    }

    private void handleKeyReleased(KeyEvent event) {
        keyRouter.findRoute(event.getCode().name())
                .filter(route -> route.command() == GameCommand.PAUSE)
                .ifPresent(route -> pauseKeyHeld = false);
        if (!finished) {
            event.consume();
        }
    }

    /**
     * 플레이어 이름("1P"/"2P")을 보드 바로 위 가운데에 둔 세로 묶음을 만든다.
     * 보드 캔버스의 너비는 첫 render 때 정해지므로, 이름 너비를 숫자가 아니라 캔버스 너비에 묶는다.
     */
    private VBox createPlayerColumn(String text, String id, PlayerPanel playerPanel, double scale) {
        Label name = new Label(text);
        name.setId(id);
        name.setFont(Theme.monoFont(FontWeight.BOLD, PLAYER_NAME_FONT_SIZE * scale));
        name.setAlignment(Pos.CENTER);

        Canvas board = (Canvas) playerPanel.getContent().lookup("#game-board");
        name.minWidthProperty().bind(board.widthProperty());
        name.prefWidthProperty().bind(board.widthProperty());
        name.maxWidthProperty().bind(board.widthProperty());

        // 패널 자체의 안쪽 여백(1인 화면용)을 없앤다. 바깥 여백과 플레이어 사이 간격은 대전 화면이 정한다
        Region content = (Region) playerPanel.getContent();
        content.setPadding(Insets.EMPTY);

        // 보드를 정보 칸과 위쪽 끝에 맞춘다 (가운데 정렬이면 정보 칸이 길 때 보드가 아래로 내려가 이름과 멀어진다)
        if (content instanceof HBox panelBox) {
            panelBox.setAlignment(Pos.TOP_LEFT);
            panelBox.setFillHeight(false); // 보드 영역이 정보 칸 높이만큼 늘어나지 않게 한다
        }
        // 정보 칸(보드가 아닌 쪽)의 고정 너비를 풀어 내용 너비만 쓰게 한다 (오른쪽 빈 공간 때문에 좌우 여백이 달라짐)
        for (Node child : content.getChildrenUnmodifiable()) {
            if (child != board.getParent() && child instanceof Region sidebar) {
                sidebar.setMinWidth(Region.USE_COMPUTED_SIZE);
                sidebar.setPrefWidth(Region.USE_COMPUTED_SIZE);
            }
        }

        VBox column = new VBox(PLAYER_NAME_SPACING, name, content);
        column.setAlignment(Pos.TOP_LEFT);
        return column;
    }

    /** 플레이어 한 명의 보드. 그리기는 createEmbedded 화면이 하고, 명령은 대전 화면이 나눠 준 것만 받는다. */
    private static final class PlayerPanel implements GameView {
        private final JavaFxGameView board;
        private Consumer<GameCommand> commandHandler;

        private PlayerPanel(GameSettings settings, Map<GameCommand, String> playerKeys,
                Map<GameCommand, String> sharedKeys, String id) {
            // 일시정지 안내에 이 플레이어가 쓰는 키가 보이도록, 키만 바꾼 설정으로 보드를 만든다
            Map<GameCommand, String> keys = new EnumMap<>(GameCommand.class);
            keys.putAll(playerKeys);
            keys.putAll(sharedKeys);
            GameSettings panelSettings = new GameSettings(settings.getWindowSize(), keys,
                    settings.isColorBlindMode(), settings.getDifficulty(), settings.getBattleKeyBindings());

            board = JavaFxGameView.createEmbedded(panelSettings);
            board.setKeyHelpVisible(false);
            board.getContent().setId(id);
            board.getContent().setFocusTraversable(false);
        }

        @Override
        public void setCommandHandler(Consumer<GameCommand> commandHandler) {
            this.commandHandler = commandHandler;
        }

        @Override
        public void render(GameSnapshot snapshot) {
            board.render(snapshot);
        }

        @Override
        public void show() {
            // 창은 대전 화면이 한 번에 보여 준다
        }

        @Override
        public void close() {
            // 창은 대전 화면이 닫는다
        }

        private javafx.scene.Parent getContent() {
            return board.getContent();
        }

        private void sendCommand(GameCommand command) {
            if (commandHandler != null) {
                commandHandler.accept(command);
            }
        }
    }
}
// AI-assisted code end