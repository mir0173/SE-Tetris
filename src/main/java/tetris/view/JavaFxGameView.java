package tetris.view;

import java.util.Map;
import java.util.function.Consumer;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import tetris.model.Board;
import tetris.model.GameCommand;
import tetris.model.GameSnapshot;
import tetris.settings.GameSettings;
public class JavaFxGameView implements GameView {

    private static final String EMPTY_CELL_SYMBOL = ".";
    private static final String FILLED_CELL_SYMBOL = "#";

    private final Stage stage;
    private final Text boardText;
    private final Map<GameCommand, String> keyBindings;

    private Consumer<GameCommand> commandHandler;

    public JavaFxGameView(Stage stage, GameSettings settings) {
        this.stage = stage;
        this.boardText = new Text();
        this.keyBindings = Map.copyOf(settings.getKeyBindings());

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
        for (Map.Entry<GameCommand, String> binding: keyBindings.entrySet()) {
            if (binding.getValue().equals(keyCode.name())) {
                return binding.getKey();
            }
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
        if (cell == Board.EMPTY_CELL) {
            return EMPTY_CELL_SYMBOL;
        }

        return FILLED_CELL_SYMBOL;
    }
}