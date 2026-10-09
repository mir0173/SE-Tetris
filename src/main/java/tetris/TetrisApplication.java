package tetris;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import tetris.controller.GameController;
import tetris.model.Board;
import tetris.model.Game;
import tetris.model.GameConfig;
import tetris.model.TetrominoGenerator;
import tetris.controller.GameTimer;
import tetris.persistence.FileScoreRepository;
import tetris.persistence.FileSettingsRepository;
import tetris.persistence.ScoreRepository;
import tetris.persistence.SettingsRepository;
import tetris.scoreboard.Scoreboard;
import tetris.settings.GameSettings;
import tetris.settings.KeyBindingValidator;
import tetris.view.GameView;
import tetris.view.JavaFxGameTimer;
import tetris.view.JavaFxGameView;
import tetris.view.JavaFxStartMenuView;
import tetris.view.JavaFxScoreboardView;
import tetris.view.JavaFxSettingsView;


/**
 * JavaFX 애플리케이션 진입점
 */
public class TetrisApplication extends Application {

    private static final int TEMP_BOARD_WIDTH = 10;
    private static final int TEMP_BOARD_HEIGHT = 20;
    private SettingsRepository settingsRepository;
    private ScoreRepository scoreRepository;
    private GameSettings currentSettings = GameSettings.defaults();

    @Override
    public void start(Stage stage) {

        Path dataDirectory = Path.of(System.getProperty("user.home"), ".se-tetris");

        settingsRepository = new FileSettingsRepository(dataDirectory.resolve("settings.json"));
        scoreRepository = new FileScoreRepository(dataDirectory.resolve("scores.json"));
        currentSettings = loadSettings(stage);
        showStartMenu(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * 저장된 설정을 불러온다
     * 읽기 실패 시 기본 설정을 사용한다
     * 중복 키가 있으면 기본 키 설정을 사용한다
     *
     */
    private GameSettings loadSettings(Stage stage) {

        try {
            GameSettings loadedSettings = settingsRepository.load();
            KeyBindingValidator validator = new KeyBindingValidator();

            if (validator.hasDuplicate(loadedSettings.getKeyBindings())) {
                showError(stage, "중복된 키 설정이 있습니다. 기본 키 설정을 사용합니다." );
                return new GameSettings(loadedSettings.getWindowSize(), GameSettings.getDefaultKeyBindings(), loadedSettings.isColorBlindMode());
            }
            return loadedSettings;
        } catch (IOException exception) {
            showError(stage, "설정을 불러오지 못했습니다. 기본 설정을 사용합니다.");
            return GameSettings.defaults();
        }
    }

    private void showStartMenu(Stage stage) {

        stage.setTitle("Tetris");

        JavaFxStartMenuView menu = new JavaFxStartMenuView(
            () -> startNewGame(stage),
            () -> showSettings(stage),
            () -> showScoreboard(stage),
            stage::close);
        stage.setScene(menu.getScene());
        stage.show();

        menu.focusFirstButton();
    }

    /**
     * 새로운 보드, 게임, 화면, 컨트롤러를 생성하여 게임을 시작한다
     *
     * @param stage 게임 화면을 표시할 창
     */
    private void startNewGame(Stage stage) {
        
        GameConfig config = new GameConfig(TEMP_BOARD_WIDTH, TEMP_BOARD_HEIGHT);
        Board board = new Board(config.getBoardWidth(), config.getBoardHeight());
        TetrominoGenerator generator = new TetrominoGenerator(new Random());
        Game game = new Game(board, generator);
        GameView view = new JavaFxGameView(stage, currentSettings);
        GameTimer timer = new JavaFxGameTimer(Game.getInitialDropInterval());
        GameController controller = new GameController(game, view, timer, () -> showStartMenu(stage));
        controller.start();
    }

    private void showSettings(Stage stage) {

        JavaFxSettingsView settingsView = new JavaFxSettingsView(
            currentSettings,
            newSettings -> saveSettings(stage, newSettings),
            () -> resetScoreboard(stage),
            () -> showStartMenu(stage));
        stage.setTitle("설정");
        stage.setScene(settingsView.getScene());
        stage.show();
        settingsView.getScene().getRoot().requestFocus();
    }

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
            Scoreboard scoreboard = new Scoreboard(scoreRepository.load());
            JavaFxScoreboardView scoreboardView = new JavaFxScoreboardView(
                scoreboard,
                null,
                () -> showStartMenu(stage));

            stage.setTitle("스코어보드");
            stage.setScene(scoreboardView.getScene());
            stage.show();
            scoreboardView.getScene().getRoot().requestFocus();
        } catch (IOException exception) {
            showError(stage, "스코어보드를 불러오지 못했습니다.");
        }
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
}