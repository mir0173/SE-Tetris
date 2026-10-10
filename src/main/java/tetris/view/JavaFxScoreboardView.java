package tetris.view;

import java.util.List;
import java.util.UUID;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import tetris.scoreboard.ScoreEntry;
import tetris.scoreboard.Scoreboard;

// AI-assisted code start
public class JavaFxScoreboardView {
    private static final double SCOREBOARD_WIDTH = 500;
    private static final double SCOREBOARD_HEIGHT = 600;
    private static final double ROOT_SPACING = 20;
    private static final double ROOT_PADDING = 25;
    private static final double COLUMN_GAP = 20;
    private static final double ROW_GAP = 8;
    private static final double ROW_FONT_SIZE = 18;
    private static final double BUTTON_WIDTH = 200;
    private static final double BUTTON_HEIGHT = 40;
    private static final int FIRST_RANK = 1;
    private static final int RANK_COLUMN = 0;
    private static final int NAME_COLUMN = 1;
    private static final int SCORE_COLUMN = 2;
    private static final int MARKER_COLUMN = 3;

    private static final String TITLE_TEXT = "스코어보드";
    private static final String EMPTY_TEXT = "기록이 없습니다";
    private static final String BACK_BUTTON_TEXT = "메뉴로";
    private static final String HIGHLIGHT_MARKER = "★";
    private static final String RANK_SUFFIX = "위";

    private final Scene scene;

    public JavaFxScoreboardView(Scoreboard scoreboard, UUID highlightEntryId, Runnable onBack, Runnable onExit) {
        Label title = new Label(TITLE_TEXT);
        Node rows = createRows(scoreboard, highlightEntryId);

        Button backButton = new Button(BACK_BUTTON_TEXT);
        backButton.setPrefSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        backButton.setOnAction(event -> onBack.run());

        VBox root = new VBox(ROOT_SPACING);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(ROOT_PADDING));
        root.getChildren().addAll(title, rows, backButton);

        if (onExit != null) {
            Button exitButton = new Button("종료");
            exitButton.setPrefSize(BUTTON_WIDTH, BUTTON_HEIGHT);
            exitButton.setOnAction(event -> onExit.run());
            
            Label help = new Label("Enter / ESC: 메뉴 · Q: 종료");
            root.getChildren().addAll(exitButton, help);
        }

        scene = new Scene(root, SCOREBOARD_WIDTH, SCOREBOARD_HEIGHT);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> handleKey(event, onBack, onExit));
    }

    public Scene getScene() {
        return scene;
    }

    private Node createRows(Scoreboard scoreboard, UUID highlightedEntryId) {
        List<ScoreEntry> entries = scoreboard.getEntries();
        if (entries.isEmpty()) {
            return new Label(EMPTY_TEXT);
        }

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(COLUMN_GAP);
        grid.setVgap(ROW_GAP);

        int highlightedRank = scoreboard.getRank(highlightedEntryId);
        for (int i = 0; i < entries.size(); i++) {
            int rank = i + FIRST_RANK;
            ScoreEntry entry = entries.get(i);
            boolean highlighted = rank == highlightedRank;

            grid.add(createCell(rank + RANK_SUFFIX, Pos.CENTER_RIGHT), RANK_COLUMN, i);
            grid.add(createCell(entry.getName(), Pos.CENTER_LEFT), NAME_COLUMN, i);
            grid.add(createCell(String.valueOf(entry.getScore()), Pos.CENTER_RIGHT), SCORE_COLUMN, i);
            if (highlighted) {
                grid.add(createCell(HIGHLIGHT_MARKER, Pos.CENTER), MARKER_COLUMN, i);
            }
        }

        return grid;
    }

    private Label createCell(String text, Pos alignment) {
        Label label = new Label(text);
        label.setFont(Font.font(ROW_FONT_SIZE));
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(alignment);
        return label;
    }

    private void handleKey(KeyEvent event, Runnable onBack, Runnable onExit) {
        if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.ENTER) {
            event.consume();
            onBack.run();
        } else if (event.getCode() == KeyCode.Q && onExit != null) {
            event.consume();
            onExit.run();
        }
    }
}
// AI-assisted code end
