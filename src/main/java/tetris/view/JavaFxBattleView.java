package tetris.view;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    private static final double PADDING = 16;
    private static final double VIEW_WIDTH = 980;
    private static final double VIEW_HEIGHT = 680;

    private final PlayerPanel firstPanel;
    private final PlayerPanel secondPanel;
    private final BattleKeyRouter keyRouter;
    private final Runnable onReturnToMenu;
    private final Label resultLabel = new Label();
    private final Label timeLeft = new Label();
    private final Button back = new Button("경기 중단");
    private final Button rematch = new Button("다시 하기");
    private final Button exit = new Button("게임 종료");
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
        firstPanel = new PlayerPanel(settings, keys.getFirstPlayerKeys(), keys.getSharedKeys(),
                "battle-first-panel");
        secondPanel = new PlayerPanel(settings, keys.getSecondPlayerKeys(), keys.getSharedKeys(),
                "battle-second-panel");

        resultLabel.setId("battle-result");
        resultLabel.setWrapText(true);
        timeLeft.setId("battle-time-left");
        for (Label label : new Label[]{resultLabel, timeLeft}) {
            label.visibleProperty().bind(label.textProperty().isNotEmpty());
            label.managedProperty().bind(label.visibleProperty());
        }

        back.setId("battle-back");
        rematch.setId("battle-rematch");
        exit.setId("battle-exit");
        back.setOnAction(event -> onReturnToMenu.run());
        rematch.setOnAction(event -> onRematch.run());
        exit.setOnAction(event -> onExit.run());
        for (Button button : new Button[]{rematch, exit}) {
            button.setVisible(false);
            button.managedProperty().bind(button.visibleProperty());
        }

        HBox boards = new HBox(SPACING,
                new VBox(SPACING, new Label("1P"), firstPanel.getContent()),
                new VBox(SPACING, new Label("2P"), secondPanel.getContent()));
        ScrollPane scroll = new ScrollPane(boards);
        scroll.setPannable(false);
        scroll.setFocusTraversable(false);
        HBox buttons = new HBox(SPACING, rematch, back, exit);
        VBox root = new VBox(SPACING, resultLabel, timeLeft, scroll, buttons);
        root.setPadding(new Insets(PADDING));

        double scale = settings.getWindowSize().getScale();
        scene = new Scene(root, VIEW_WIDTH * scale, VIEW_HEIGHT * scale);
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
        back.setText("메뉴로");
        rematch.setVisible(true);
        exit.setVisible(true);
        rematch.requestFocus();
    }

    /** 시간제한 모드의 남은 시간을 "남은 시간 m:ss"로 보인다. */
    public void renderRemainingSeconds(int seconds) {
        timeLeft.setText(String.format(Locale.ROOT, "남은 시간 %d:%02d", seconds / 60, seconds % 60));
    }

    private void handleKeyPressed(KeyEvent event) {
        Optional<BattleKeyRouter.Route> route = keyRouter.findRoute(event.getCode().name());

        if (finished) {
            // 결과 뒤: 게임 키는 무시하고 종료 키만 메뉴로. 나머지(Enter 등)는 버튼이 받도록 그대로 둔다
            if (route.isPresent() && route.get().command() == GameCommand.QUIT_GAME) {
                event.consume();
                onReturnToMenu.run();
            }
            return;
        }

        // 경기 중: 버튼이 키를 받지 않도록 모두 막는다 (예: 포커스된 '경기 중단' 버튼이 SPACE로 눌리는 것 방지)
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
            board.setKeyHelpText(createKeyHelp(playerKeys, sharedKeys));
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

        private static String createKeyHelp(Map<GameCommand, String> playerKeys, Map<GameCommand, String> sharedKeys) {
            return playerKeys.get(GameCommand.MOVE_LEFT) + " / " + playerKeys.get(GameCommand.MOVE_RIGHT) + ": 좌우 이동\n"
                    + playerKeys.get(GameCommand.SOFT_DROP) + ": 아래로\n"
                    + playerKeys.get(GameCommand.ROTATE_CLOCKWISE) + " / "
                    + playerKeys.get(GameCommand.ROTATE_COUNTERCLOCKWISE) + ": 회전\n"
                    + playerKeys.get(GameCommand.HARD_DROP) + ": 하드 드롭\n"
                    + playerKeys.get(GameCommand.HOLD) + ": 홀드\n"
                    + sharedKeys.get(GameCommand.PAUSE) + ": 일시정지 · "
                    + sharedKeys.get(GameCommand.QUIT_GAME) + ": 중단";
        }
    }
}
// AI-assisted code end