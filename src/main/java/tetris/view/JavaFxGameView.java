package tetris.view;

import java.util.function.Consumer;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import tetris.model.GameCommand;
import tetris.model.GameSnapshot;

public class JavaFxGameView implements GameView {

    private static final int EMPTY_CELL = 0;

    private static final String EMPTY_CELL_SYMBOL = ".";
    private static final String FILLED_CELL_SYMBOL = "#";

    private final Stage stage;
    private final Text boardText;

    private Consumer<GameCommand> commandHandler;

    public JavaFxGameView(Stage stage) {
        this.stage = stage;
        this.boardText = new Text();

        initializeView();
    }

    @Override
    public void setCommandHandler(Consumer<GameCommand> commandHandler) {
        this.commandHandler = commandHandler;
    }

    @Override
    public void render(GameSnapshot snapshot) {
        boardText.setText(createBoardText(snapshot.getCells()));
    }

    @Override
    public void show() {
        stage.show();
    }

    @Override
    public void close() {
        stage.close();
    }

    private void initializeView() {
        boardText.setFont(Font.font("Monospaced", 20));

        StackPane root = new StackPane(boardText);
        Scene scene = new Scene(root, 500, 600);

        scene.setOnKeyPressed(event -> handleKey(event.getCode()));

        stage.setTitle("SE Tetris");
        stage.setScene(scene);
    }

    private void handleKey(KeyCode keyCode) {
        GameCommand command = mapKey(keyCode);

        if (command == GameCommand.NONE || commandHandler == null) {
            return;
        }

        commandHandler.accept(command);
    }

    private GameCommand mapKey(KeyCode keyCode) {
        if (keyCode == KeyCode.ESCAPE) {
            return GameCommand.QUIT_GAME;
        }

        return GameCommand.NONE;
    }

    private String createBoardText(int[][] cells) {
        StringBuilder builder = new StringBuilder();

        for (int[] row : cells) {
            for (int cell : row) {
                builder.append(getCellSymbol(cell));
            }

            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }

    private String getCellSymbol(int cell) {
        if (cell == EMPTY_CELL) {
            return EMPTY_CELL_SYMBOL;
        }

        return FILLED_CELL_SYMBOL;
    }
}