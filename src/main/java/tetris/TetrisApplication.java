package tetris;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import tetris.controller.GameController;
import tetris.model.Board;
import tetris.model.Game;
import tetris.model.GameConfig;
import tetris.view.GameView;
import tetris.view.JavaFxGameView;
import tetris.view.JavaFxStartMenuView;

/**
 * JavaFX 애플리케이션 진입점
 */
public class TetrisApplication extends Application {

    private static final int TEMP_BOARD_WIDTH = 10;
    private static final int TEMP_BOARD_HEIGHT = 20;

    @Override
    public void start(Stage stage) {
        showStartMenu(stage);
    }

    private void showStartMenu(Stage stage) {

        stage.setTitle("Tetris");

        JavaFxStartMenuView menu = new JavaFxStartMenuView(
                () -> startNewGame(stage),
                () -> showFeatureNotice(stage, "설정"),
                () -> showFeatureNotice(stage, "스코어보드"),
                stage::close
        );
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
        Game game = new Game(board);
        GameView view = new JavaFxGameView(stage);

        GameController controller = new GameController(game, view, () -> showStartMenu(stage));
        controller.start();
    }

    /**
     * 설정 및 스코어보드 화면을 연결하기 전까지 사용하는 임시 동작
     *
     * @param stage 안내창의 소유 창
     * @param featureName 안내창 제목에 표시할 기능 이름
     */

    private void showFeatureNotice(Stage stage, String featureName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.initOwner(stage);
        alert.setTitle(featureName);
        alert.setHeaderText(null);
        alert.setContentText(featureName);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}