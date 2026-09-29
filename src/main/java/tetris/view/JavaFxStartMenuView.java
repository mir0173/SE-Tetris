package tetris.view;

import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

public class JavaFxStartMenuView {
    
    private static final double MENU_WIDTH = 500;
    private static final double MENU_HEIGHT = 600;
    private static final double BUTTON_WIDTH = 200;

    private final Scene scene;
    private final List<Button> menuButtons; 

    /**
     * 시작 메뉴 화면을 구성하고 각 버튼에 실행할 동작을 연결한다
     *
     * @param onStartGame 게임 시작 버튼을 선택했을 때 실행할 동작
     * @param onSettings 설정 버튼을 선택했을 때 실행할 동작
     * @param onScoreboard 스코어보드 버튼을 선택했을 때 실행할 동작
     * @param onExit 종료 버튼을 선택했을 때 실행할 동작
     */
    public JavaFxStartMenuView(Runnable onStartGame, Runnable onSettings, Runnable onScoreboard, Runnable onExit) {
        Label title = new Label("Tetris");

        Button startButton = createButton("게임 시작", onStartGame);
        Button settingsButton = createButton("설정", onSettings);
        Button scoreboardButton = createButton("스코어보드", onScoreboard);
        Button exitButton = createButton("종료", onExit);

        menuButtons = List.of(startButton, settingsButton, scoreboardButton, exitButton);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(25));    

        root.getChildren().add(title);
        root.getChildren().addAll(menuButtons);

        scene = new Scene(root, MENU_WIDTH, MENU_HEIGHT);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleMenuKey);
    }

    public Scene getScene() {
        return scene;
    }

    /**
     * 게임 시작 버튼에 키보드 포커스를 요청한다
     */
    public void focusFirstButton() {
        menuButtons.get(0).requestFocus();
    }

    /**
     * 실행할 동작을 연결한 메뉴 버튼을 생성한다
     *
     * @param text 버튼에 표시할 문구
     * @param action 버튼을 선택했을 때 실행할 동작
     * @return 생성된 메뉴 버튼
     */
    private Button createButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setPrefWidth(BUTTON_WIDTH);
        button.setPrefHeight(40);
        button.setOnAction(event -> action.run());
        return button;
    }

    /**
     * 위아래 방향키로 이동하고 Enter 키로 실행한다
     * 처리한 키 이벤트는 소비한다.
     *
     * @param event 메뉴 화면에서 발생한 클릭 이벤트
     */
    private void handleMenuKey(KeyEvent event) {
        switch (event.getCode()) {
            case UP -> {
                moveFocus(-1);
                event.consume();
            }
            case DOWN -> {
                moveFocus(1);
                event.consume();
            }
            case ENTER -> {
                event.consume();
                activateFocusedButton();
            }
            default-> {

            }
        }
    }

    /**
     * 지정한 방향으로 키보드 포커스를 이동한다
     * 목록을 순환하며 선택된 메뉴가 없으면 첫 번째 메뉴로 포커스를 이동한다
     *
     * @param direction 이전 메뉴는 -1, 다음 메뉴는 1
     */
    private void moveFocus(int direction) {
        int currentIndex = menuButtons.indexOf(scene.getFocusOwner());
        int newIndex = currentIndex < 0 ? 0 : Math.floorMod(currentIndex + direction, menuButtons.size());
        menuButtons.get(newIndex).requestFocus();
    }

    /**
     * 현재 키보드 포커스가 있는 메뉴 버튼을 실행한다
     * 포커스가 없으면 아무 동작도 하지 않는다
     */
    private void activateFocusedButton() {
        int currentIndex = menuButtons.indexOf(scene.getFocusOwner());
        if (currentIndex >= 0) {
            menuButtons.get(currentIndex).fire();
        }
    }
}
