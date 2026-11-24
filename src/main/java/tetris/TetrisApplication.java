package tetris;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import tetris.battle.BattleSettings;
import tetris.battle.BattleMode;
import tetris.controller.GameController;
import tetris.model.Board;
import tetris.model.Game;
import tetris.model.GameConfig;
import tetris.model.GameMode;
import tetris.model.Difficulty;
import tetris.model.TetrominoGenerator;
import tetris.controller.GameTimer;
import tetris.persistence.FileScoreRepository;
import tetris.persistence.FileSettingsRepository;
import tetris.persistence.RecentPeerAddressRepository;
import tetris.persistence.ScoreRepository;
import tetris.persistence.SettingsRepository;
import tetris.scoreboard.ScoreCategory;
import tetris.scoreboard.ScoreEntry;
import tetris.scoreboard.ScoreboardGroup;
import tetris.settings.GameSettings;
import tetris.settings.KeyBindingValidator;
import tetris.view.GameView;
import tetris.view.JavaFxGameTimer;
import tetris.view.JavaFxDelayScheduler;
import tetris.view.JavaFxGameView;
import tetris.view.JavaFxStartMenuView;
import tetris.view.JavaFxScoreboardView;
import tetris.view.JavaFxSettingsView;
import tetris.battle.LocalPlayer;
import tetris.battle.MatchManager;
import tetris.settings.BattleKeyBindings;
import tetris.view.JavaFxBattleKeySettingsView;
import tetris.view.JavaFxBattleSetupView;
import tetris.view.JavaFxBattleView;
import tetris.view.JavaFxP2pLobbyView;
import tetris.network.P2pLobbyController;
import tetris.network.P2pBattleController;
import tetris.network.MatchStart;
import tetris.network.LobbyState;
import tetris.view.JavaFxP2pGameView;

/**
 * JavaFX 애플리케이션 진입점
 */
public class TetrisApplication extends Application {

    private static final Logger LOGGER = Logger.getLogger(TetrisApplication.class.getName());
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private SettingsRepository settingsRepository;
    private ScoreRepository scoreRepository;
    private GameSettings currentSettings = GameSettings.defaults();
    private P2pLobbyController p2pLobbyController;
    private P2pBattleController p2pBattleController;
    private JavaFxP2pGameView p2pBattleView;
    private MatchManager localMatchManager;
    private RecentPeerAddressRepository recentPeerAddresses;

    @Override
    public void start(Stage stage) {
        stage.getIcons().add(new Image(TetrisApplication.class.getResourceAsStream("/icons/se-tetris.png")));
        Path dataDirectory = Path.of(System.getProperty("user.home"), ".se-tetris");

        settingsRepository = new FileSettingsRepository(dataDirectory.resolve("settings.json"));
        scoreRepository = new FileScoreRepository(dataDirectory.resolve("scores.json"));
        recentPeerAddresses = new RecentPeerAddressRepository(dataDirectory.resolve("last-peer-ip.txt"));
        currentSettings = loadSettings(stage);
        showStartMenu(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        closeP2pLobby();
        closeLocalBattle();
    }

    /**
     * 저장된 설정을 불러온다
     * 읽기 실패 시 기본 설정을 사용한다
     * 중복 키가 있으면 기본 키 설정을 사용한다
     *
     */
    private GameSettings loadSettings(Stage stage) {

        try {
            // AI-assisted code start
            GameSettings loadedSettings = settingsRepository.load();
            KeyBindingValidator validator = new KeyBindingValidator();

            if (validator.hasDuplicate(loadedSettings.getKeyBindings())) {
                showError(stage, "중복된 키 설정이 있습니다. 기본 키 설정을 사용합니다.");
                loadedSettings = new GameSettings(loadedSettings.getWindowSize(), GameSettings.getDefaultKeyBindings(),
                        loadedSettings.isColorBlindMode(), loadedSettings.getDifficulty(),
                        loadedSettings.getBattleKeyBindings());
            }
            if (loadedSettings.getBattleKeyBindings().hasDuplicate()) {
                showError(stage, "중복된 대전 키 설정이 있습니다. 기본 대전 키 설정을 사용합니다.");
                loadedSettings = new GameSettings(loadedSettings.getWindowSize(), loadedSettings.getKeyBindings(),
                        loadedSettings.isColorBlindMode(), loadedSettings.getDifficulty(),
                        BattleKeyBindings.defaults());
            }
            return loadedSettings;
            // AI-assisted code end
        } catch (IOException exception) {
            showError(stage, "설정을 불러오지 못했습니다. 기본 설정을 사용합니다.");
            return GameSettings.defaults();
        }
    }

    private void showP2pLobby(Stage stage) {
        closeP2pLobby();
        JavaFxP2pLobbyView view = new JavaFxP2pLobbyView();
        restorePeerAddress(view);
        P2pLobbyController controller = new P2pLobbyController(
                Platform::runLater, state -> {
                    view.render(state);
                    if (p2pBattleController != null && state.phase() == LobbyState.Phase.IDLE) {
                        if (p2pBattleController.peerDisconnected()) {
                            return;
                        }
                        if (p2pBattleController.hasResult()) {
                            p2pBattleView.setStatus(state.message());
                            p2pBattleView.renderConnectionHealth(null);
                        } else {
                            returnFromP2pBattle(stage, view);
                        }
                    } else if (p2pBattleController != null && state.phase() != LobbyState.Phase.STARTED
                            && state.phase() != LobbyState.Phase.STARTING) {
                        returnFromP2pBattle(stage, view);
                    } else if (p2pBattleView != null) {
                        p2pBattleView.renderConnectionHealth(state.health());
                    }
                }, start -> startP2pBattle(stage, view, start),
                new BattleSettings(BattleMode.NORMAL, currentSettings.getDifficulty(), 0), this::rememberPeerAddress);
        p2pLobbyController = controller;
        view.bind(controller, () -> {
            closeP2pLobby();
            showStartMenu(stage);
        });
        stage.setTitle("P2P 대전");
        stage.setScene(view.getScene());
        stage.sizeToScene();
        stage.show();
    }

    private void restorePeerAddress(JavaFxP2pLobbyView view) {
        try {
            recentPeerAddresses.load().ifPresent(view::setRecentAddress);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "최근 접속 IP를 불러오지 못했습니다.", exception);
        }
    }

    private void rememberPeerAddress(String address) {
        try {
            recentPeerAddresses.save(address);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "최근 접속 IP를 저장하지 못했습니다.", exception);
        }
    }

    private void closeP2pLobby() {
        closeP2pBattle();
        if (p2pLobbyController != null) {
            p2pLobbyController.close();
            p2pLobbyController = null;
        }
    }

    private void startP2pBattle(Stage stage, JavaFxP2pLobbyView lobbyView, MatchStart start) {
        closeP2pBattle();
        JavaFxP2pGameView view = new JavaFxP2pGameView(currentSettings,
                () -> p2pBattleController.requestReturnToLobby());
        p2pBattleView = view;
        view.renderConnectionHealth(p2pLobbyController.getState().health());
        GameMode mode = start.settings().mode() == BattleMode.ITEM ? GameMode.ITEM : GameMode.NORMAL;
        Game game = createGame(mode, start.settings().difficulty());
        p2pBattleController = new P2pBattleController(game, view.getLocalView(),
                new JavaFxGameTimer(Game.getInitialDropInterval()), new JavaFxDelayScheduler(),
                new JavaFxGameTimer(P2pBattleController.STATE_INTERVAL_MILLIS), new JavaFxGameTimer(1_000),
                p2pLobbyController.getBattleLink(start), start, view::renderRemote, view::setStatus,
                view::renderResult, view::renderRemainingSeconds, () -> {
                    if (p2pLobbyController.getState().phase() == LobbyState.Phase.STARTED) {
                        p2pLobbyController.requestReturnToLobby();
                    } else {
                        returnFromP2pBattle(stage, lobbyView);
                    }
                });
        stage.setTitle("P2P 대전");
        stage.setScene(view.getScene());
        stage.setOnCloseRequest(event -> {
            event.consume();
            p2pBattleController.requestWindowClose(() -> {
                stage.setOnCloseRequest(null);
                closeP2pLobby();
                stage.close();
            });
        });
        p2pBattleController.start();
    }

    private void returnFromP2pBattle(Stage stage, JavaFxP2pLobbyView view) {
        closeP2pBattle();
        stage.setOnCloseRequest(null);
        stage.setScene(view.getScene());
        stage.setTitle("P2P 대전");
        stage.sizeToScene();
    }

    private void closeP2pBattle() {
        p2pBattleView = null;
        if (p2pBattleController != null) {
            p2pBattleController.close();
            p2pBattleController = null;
        }
    }

    private void showStartMenu(Stage stage) {

        stage.setTitle("Tetris");

        JavaFxStartMenuView menu = new JavaFxStartMenuView(
                () -> startNewGame(stage, GameMode.NORMAL),
                () -> startNewGame(stage, GameMode.ITEM),
                () -> showBattleSetup(stage),
                () -> showP2pLobby(stage),
                () -> showSettings(stage),
                () -> showScoreboard(stage),
                stage::close);
        stage.setScene(menu.getScene());
        stage.sizeToScene();
        stage.show();

        menu.focusFirstButton();
    }

    /**
     * 새로운 보드, 게임, 화면, 컨트롤러를 생성하여 게임을 시작한다
     *
     * @param stage 게임 화면을 표시할 창
     */
    private void startNewGame(Stage stage, GameMode mode) {

        Difficulty difficulty = currentSettings.getDifficulty();
        Game game = createGame(mode, difficulty);
        GameView view = new JavaFxGameView(stage, currentSettings);
        GameTimer timer = new JavaFxGameTimer(Game.getInitialDropInterval());
        GameController controller = new GameController(game, view, timer, new JavaFxDelayScheduler(),
                () -> showStartMenu(stage),
                score -> Platform.runLater(() -> handleGameOver(stage, score, mode, difficulty)));
        controller.start();
    }

    // AI-assisted code start
    /** 로컬 대전 모드 선택 화면을 보인다 */
    private void showBattleSetup(Stage stage) {
        JavaFxBattleSetupView setupView = new JavaFxBattleSetupView(currentSettings,
                battleSettings -> startLocalBattle(stage, battleSettings),
                () -> showBattleKeySettings(stage, () -> showBattleSetup(stage)),
                () -> showStartMenu(stage));
        stage.setTitle("로컬 대전");
        stage.setScene(setupView.getScene());
        stage.sizeToScene();
        stage.show();
        setupView.focusStartButton();
    }

    /**
     * 한 창에서 두 사람이 키보드를 나눠 쓰는 대전을 시작한다.
     * 게임·컨트롤러를 두 벌 만들고, MatchManager가 공격 전달·일시정지 맞추기·승패 판정을 맡는다.
     * 대전 결과는 스코어보드에 기록하지 않는다.
     */
    private void startLocalBattle(Stage stage, BattleSettings battleSettings) {
        closeLocalBattle();
        JavaFxBattleView view = new JavaFxBattleView(currentSettings,
                () -> {
                    closeLocalBattle();
                    showStartMenu(stage);
                },
                () -> startLocalBattle(stage, battleSettings),
                () -> {
                    closeLocalBattle();
                    stage.close();
                });

        GameMode mode = battleSettings.mode() == BattleMode.ITEM ? GameMode.ITEM : GameMode.NORMAL;
        Game firstGame = createGame(mode, battleSettings.difficulty());
        Game secondGame = createGame(mode, battleSettings.difficulty());
        GameController firstController = createBattleController(firstGame, view.getFirstPlayerView());
        GameController secondController = createBattleController(secondGame, view.getSecondPlayerView());
        localMatchManager = new MatchManager(new LocalPlayer(firstGame, firstController),
                new LocalPlayer(secondGame, secondController), view::renderResult);

        stage.setTitle("로컬 대전");
        stage.setScene(view.getScene());

        localMatchManager.start();
        firstController.start();
        secondController.start();
        // 보드 크기는 첫 화면을 그릴 때 정해지므로, 게임을 시작한 뒤에 창 크기를 내용에 맞춘다
        stage.sizeToScene();
        stage.show();
        if (battleSettings.mode() == BattleMode.TIME_LIMIT) {
            localMatchManager.startTimeLimit(new JavaFxGameTimer(1_000), battleSettings.timeLimitSeconds(),
                    view::renderRemainingSeconds);
        }
    }

    // 대전용 컨트롤러. 메뉴 복귀와 게임오버는 MatchManager의 결과로만 처리하므로 아무것도 하지 않는다
    private GameController createBattleController(Game game, GameView view) {
        return new GameController(game, view, new JavaFxGameTimer(Game.getInitialDropInterval()),
                new JavaFxDelayScheduler(), () -> { }, score -> { });
    }

    // 진행 중인 로컬 대전을 결과 없이 멈춘다. 이미 끝난 경기면 아무 일도 하지 않는다
    private void closeLocalBattle() {
        if (localMatchManager != null) {
            localMatchManager.cancel();
            localMatchManager = null;
        }
    }
    // AI-assisted code end

    private void showSettings(Stage stage) {

        JavaFxSettingsView settingsView = new JavaFxSettingsView(
                currentSettings,
                newSettings -> saveSettings(stage, newSettings),
                () -> resetScoreboard(stage),
                () -> showBattleKeySettings(stage, () -> showSettings(stage)), // AI-assisted (T17)
                () -> showStartMenu(stage));
        stage.setTitle("설정");
        stage.setScene(settingsView.getScene());
        stage.sizeToScene();
        stage.show();
        settingsView.getScene().getRoot().requestFocus();
    }

    // AI-assisted code start
    /**
     * 대전 키 설정 화면을 보인다. 설정 화면과 로컬 대전 모드 선택 화면에서 들어온다.
     *
     * @param onBack 뒤로 갈 때 돌아갈 화면을 여는 동작 (들어온 화면)
     */
    private void showBattleKeySettings(Stage stage, Runnable onBack) {
        JavaFxBattleKeySettingsView keySettingsView = new JavaFxBattleKeySettingsView(
                currentSettings,
                newSettings -> saveSettings(stage, newSettings),
                onBack);
        stage.setTitle("대전 키 설정");
        stage.setScene(keySettingsView.getScene());
        stage.sizeToScene();
        stage.show();
        keySettingsView.getScene().getRoot().requestFocus();
    }
    // AI-assisted code end

    private void saveSettings(Stage stage, GameSettings newSettings) {

        try {
            settingsRepository.save(newSettings);
            currentSettings = newSettings;
        } catch (IOException exception) {
            showError(stage, "설정을 저장하지 못했습니다.");
            showSettings(stage);
        }
    }

    /**
     * 저장된 기록을 불러와 스코어보드에 표시한다
     * 실패 시 현재 화면을 유지한다
     *
     */
    private void showScoreboard(Stage stage) {

        try {
            ScoreboardGroup group = new ScoreboardGroup(scoreRepository.load());
            ScoreCategory category = new ScoreCategory(GameMode.NORMAL, currentSettings.getDifficulty());
            showScoreboard(stage, group, category, null);
        } catch (IOException exception) {
            showError(stage, "스코어보드를 불러오지 못했습니다.");
        }
    }

    private void showScoreboard(Stage stage, ScoreboardGroup group, ScoreCategory category, UUID highlightEntryId) {

        JavaFxScoreboardView scoreboardView = new JavaFxScoreboardView(
                group,
                category,
                highlightEntryId,
                () -> showStartMenu(stage),
                stage::close);

        stage.setTitle("스코어보드");
        stage.setScene(scoreboardView.getScene());
        stage.sizeToScene();
        stage.show();
        scoreboardView.getScene().getRoot().requestFocus();
    }

    private void resetScoreboard(Stage stage) {

        try {
            scoreRepository.save(List.of());
        } catch (IOException exception) {
            showError(stage, "스코어보드를 초기화하지 못했습니다.");
            showSettings(stage);
        }
    }

    /**
     * 게임오버 점수를 확인하고 순위에 들면 이름과 기록을 저장한다
     * 스코어보드 화면을 표시한다
     *
     */
    private void handleGameOver(Stage stage, long score, GameMode mode, Difficulty difficulty) {
        ScoreboardGroup group;

        try {
            group = new ScoreboardGroup(scoreRepository.load());
        } catch (IOException exception) {
            showError(stage, "점수 기록을 불러오지 못했습니다.");
            showStartMenu(stage);
            return;
        }

        ScoreCategory category = new ScoreCategory(mode, difficulty);
        UUID highlightEntryId = null;

        if (group.isRankIn(category, score)) {
            Optional<String> name = requestPlayerName(stage, score);

            if (name.isPresent()) {
                ScoreEntry entry = new ScoreEntry(UUID.randomUUID(), name.get(), score,
                        mode, difficulty);

                ScoreboardGroup updatedGroup = group.addEntry(entry);

                try {
                    scoreRepository.save(updatedGroup.getAllEntries());
                    group = updatedGroup;
                    highlightEntryId = entry.getId();
                } catch (IOException exception) {
                    showError(stage, "새 점수 기록을 저장하지 못했습니다.");
                }
            }
        }
        showScoreboard(stage, group, category, highlightEntryId);
    }

    /**
     * 순위에 오른 점수의 플레이어 이름을 입력받는다
     *
     */
    private Optional<String> requestPlayerName(Stage stage, long score) {
        TextInputDialog dialog = new TextInputDialog();

        dialog.initOwner(stage);
        dialog.setTitle("새 점수 기록");
        dialog.setHeaderText("최종 점수: " + score);
        dialog.setContentText("이름:");

        while (true) {
            Optional<String> result = dialog.showAndWait();

            if (result.isEmpty()) {
                return Optional.empty();
            }

            String name = result.get().strip();

            if (!name.isBlank()) {
                return Optional.of(name);
            }

            showError(stage, "이름을 입력해주세요.");
        }
    }

    /**
     * 파일 읽기 또는 저장 실패를 사용자에게 알린다
     *
     */
    private void showError(Stage stage, String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(stage);
        alert.setTitle("오류");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // 모드와 난이도에 맞는 새 게임을 만든다
    private Game createGame(GameMode mode, Difficulty difficulty) {
        GameConfig config = new GameConfig(BOARD_WIDTH, BOARD_HEIGHT);
        Board board = new Board(config.getBoardWidth(), config.getBoardHeight());
        TetrominoGenerator generator = new TetrominoGenerator(new Random(), difficulty.createWeights());
        Game game = new Game(board, generator, difficulty, mode);
        return game;
    }

}
