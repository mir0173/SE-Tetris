package tetris.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import tetris.model.Difficulty;
import tetris.model.GameMode;
import tetris.scoreboard.ScoreCategory;
import tetris.scoreboard.ScoreEntry;
import tetris.scoreboard.Scoreboard;
import tetris.scoreboard.ScoreboardGroup;

// AI-assisted code start
public class JavaFxScoreboardView {
    private static final double SCOREBOARD_WIDTH = 500;
    private static final double SCOREBOARD_HEIGHT = 600;
    private static final double ROOT_SPACING = 20;
    private static final double ROOT_PADDING = 25;
    private static final double TAB_SPACING = 20;
    private static final double COLUMN_GAP = 12;
    private static final double ROW_GAP = 8;
    private static final double MENU_SPACING = 10;
    private static final double UNSELECTED_TAB_OPACITY = 0.5;
    private static final double UNDERLINE_WIDTH = 2;
    private static final double PLACEHOLDER_OPACITY = 0.5;
    private static final double SCORE_FONT_SIZE = 18;
    private static final double TITLE_FONT_SIZE = 30;
    private static final double MENU_BUTTON_WIDTH = 180;
    private static final double MENU_MARKER_SPACING = 16;
    private static final double RANK_COLUMN_WIDTH = 36;
    private static final double NAME_COLUMN_WIDTH = 130;
    private static final double LEADER_COLUMN_WIDTH = 70;
    private static final double SCORE_COLUMN_WIDTH = 90;
    private static final double MARKER_COLUMN_WIDTH = 28;

    private static final String TITLE_TEXT = "★ H I G H  S C O R E ★";
    private static final String MODE_SEPARATOR_TEXT = "·";
    private static final String NORMAL_MODE_TEXT = "일 반";
    private static final String ITEM_MODE_TEXT = "아이템";
    private static final String LEFT_ARROW_TEXT = "◀";
    private static final String RIGHT_ARROW_TEXT = "▶";
    private static final String LEADER_TEXT = "·········";
    private static final String EMPTY_NAME_TEXT = "---";
    private static final String EMPTY_SCORE_TEXT = "000000";
    private static final String HIGHLIGHT_MARKER_TEXT = "★";
    private static final String MENU_BACK_TEXT = "메뉴로 돌아가기";
    private static final String MENU_EXIT_TEXT = "게임 종료";
    private static final String MENU_LEFT_MARKER_TEXT = "▶";
    private static final String MENU_RIGHT_MARKER_TEXT = "◀";
    private static final String MODE_ROW_ID = "modeRow";
    private static final String DIFFICULTY_ROW_ID = "difficultyRow";
    private static final String SCORE_GRID_ID = "scoreGrid";
    private static final String MENU_BOX_ID = "menuBox";
    private static final int MAX_ROWS = 10;
    private static final int RANK_COLUMN = 0;
    private static final int NAME_COLUMN = 1;
    private static final int LEADER_COLUMN = 2;
    private static final int SCORE_COLUMN = 3;
    private static final int MARKER_COLUMN = 4;
    private static final int FOCUS_MODE = 0;
    private static final int FOCUS_DIFFICULTY = 1;
    private static final int FOCUS_BACK = 2;
    private static final int FOCUS_EXIT = 3;
    private static final int FOCUS_ROW_COUNT = 4;

    private final ScoreboardGroup group;
    private final UUID highlightEntryId;
    private final Runnable onBack;
    private final Runnable onExit;
    private final HBox modeRow = new HBox(TAB_SPACING);
    private final HBox difficultyRow = new HBox(TAB_SPACING);
    private final GridPane scoreGrid = new GridPane();
    private final VBox menuBox = new VBox(MENU_SPACING);
    private final List<Runnable> menuActions = new ArrayList<>();
    private final Scene scene;

    private ScoreCategory currentCategory;
    private int focusedRow;

    public JavaFxScoreboardView(ScoreboardGroup group, ScoreCategory initialCategory,
        UUID highlightEntryId, Runnable onBack, Runnable onExit) {
        this.group = Objects.requireNonNull(group, "group is null");
        this.currentCategory = Objects.requireNonNull(initialCategory, "initialCategory is null");
        this.highlightEntryId = highlightEntryId;
        this.onBack = Objects.requireNonNull(onBack, "onBack is null");
        this.onExit = Objects.requireNonNull(onExit, "onExit is null");

        modeRow.setId(MODE_ROW_ID);
        modeRow.setAlignment(Pos.CENTER);
        difficultyRow.setId(DIFFICULTY_ROW_ID);
        difficultyRow.setAlignment(Pos.CENTER);
        scoreGrid.setId(SCORE_GRID_ID);
        scoreGrid.setAlignment(Pos.CENTER);
        scoreGrid.setHgap(COLUMN_GAP);
        scoreGrid.setVgap(ROW_GAP);
        double[] columnWidths = {
            RANK_COLUMN_WIDTH,
            NAME_COLUMN_WIDTH,
            LEADER_COLUMN_WIDTH,
            SCORE_COLUMN_WIDTH,
            MARKER_COLUMN_WIDTH
        };
        for (double columnWidth : columnWidths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setMinWidth(columnWidth);
            column.setPrefWidth(columnWidth);
            column.setMaxWidth(columnWidth);
            scoreGrid.getColumnConstraints().add(column);
        }
        menuBox.setId(MENU_BOX_ID);
        menuBox.setAlignment(Pos.CENTER);

        createMenu();
        updateTabs();
        updateRows();
        updateFocus();

        VBox root = new VBox(ROOT_SPACING);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(ROOT_PADDING));
        Label title = new Label(TITLE_TEXT);
        title.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, TITLE_FONT_SIZE));
        root.getChildren().addAll(title, modeRow, difficultyRow, scoreGrid, menuBox);

        scene = new Scene(root, SCOREBOARD_WIDTH, SCOREBOARD_HEIGHT);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKey);
    }

    public Scene getScene() {
        return scene;
    }

        /**
     * 현재 조합에 맞게 모드 줄과 난이도 줄을 다시 만든다.
     * 현재 모드와 난이도의 탭에만 밑줄을 그린다.
     */
    private void updateTabs() {
        modeRow.getChildren().clear();
        difficultyRow.getChildren().clear();

        GameMode selectedMode = currentCategory.mode();
        modeRow.getChildren().add(new Label(LEFT_ARROW_TEXT));
        for (int index = 0; index < GameMode.values().length; index++) {
            GameMode mode = GameMode.values()[index];
            String modeText = switch (mode) {
                case NORMAL -> NORMAL_MODE_TEXT;
                case ITEM -> ITEM_MODE_TEXT;
            };
            Label modeLabel = new Label(modeText);
            if (mode == selectedMode) {
                modeLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD,
                    Font.getDefault().getSize()));
                modeLabel.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY, new BorderWidths(0, 0, UNDERLINE_WIDTH, 0))));
            } else {
                modeLabel.setOpacity(UNSELECTED_TAB_OPACITY);
            }
            modeRow.getChildren().add(modeLabel);

            if (index < GameMode.values().length - 1) {
                modeRow.getChildren().add(new Label(MODE_SEPARATOR_TEXT));
            }
        }
        modeRow.getChildren().add(new Label(RIGHT_ARROW_TEXT));

        difficultyRow.getChildren().add(new Label(LEFT_ARROW_TEXT));
        for (Difficulty difficulty : Difficulty.values()) {
            Label difficultyLabel = new Label(difficulty.name());
            if (difficulty == currentCategory.difficulty()) {
                difficultyLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD,
                    Font.getDefault().getSize()));
                difficultyLabel.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY, new BorderWidths(0, 0, UNDERLINE_WIDTH, 0))));
            } else {
                difficultyLabel.setOpacity(UNSELECTED_TAB_OPACITY);
            }
            difficultyRow.getChildren().add(difficultyLabel);
        }
        difficultyRow.getChildren().add(new Label(RIGHT_ARROW_TEXT));
    }

    /**
     * 현재 조합의 기록으로 순위 표를 10줄로 다시 채운다.
     */
    private void updateRows() {
        scoreGrid.getChildren().clear();

        Scoreboard scoreboard = group.getScoreboard(currentCategory);
        List<ScoreEntry> entries = scoreboard.getEntries();
        int highlightedRank = scoreboard.getRank(highlightEntryId);

        for (int row = 0; row < MAX_ROWS; row++) {
            boolean hasEntry = row < entries.size();
            ScoreEntry entry = hasEntry ? entries.get(row) : null;

            Label rankLabel = createCell(String.format("%02d", row + 1), Pos.CENTER_RIGHT);
            Label nameLabel = createCell(hasEntry ? entry.getName() : EMPTY_NAME_TEXT,
                Pos.CENTER_LEFT);
            Label leaderLabel = createCell(LEADER_TEXT, Pos.CENTER);
            Label scoreLabel = createCell(hasEntry
                ? String.format("%06d", entry.getScore()) : EMPTY_SCORE_TEXT, Pos.CENTER_RIGHT);

            if (!hasEntry) {
                rankLabel.setOpacity(PLACEHOLDER_OPACITY);
                nameLabel.setOpacity(PLACEHOLDER_OPACITY);
                leaderLabel.setOpacity(PLACEHOLDER_OPACITY);
                scoreLabel.setOpacity(PLACEHOLDER_OPACITY);
            }

            scoreGrid.add(rankLabel, RANK_COLUMN, row);
            scoreGrid.add(nameLabel, NAME_COLUMN, row);
            scoreGrid.add(leaderLabel, LEADER_COLUMN, row);
            scoreGrid.add(scoreLabel, SCORE_COLUMN, row);

            if (row + 1 == highlightedRank) {
                scoreGrid.add(createCell(HIGHLIGHT_MARKER_TEXT, Pos.CENTER), MARKER_COLUMN, row);
            }
        }
    }

    /**
     * 순위 표의 한 칸을 만든다.
     *
     * @param text 표시할 문자열
     * @param alignment 문자열 정렬 방향
     * @return 설정된 순위 표 셀
     */
    private Label createCell(String text, Pos alignment) {
        Label cell = new Label(text);
        cell.setFont(Font.font(SCORE_FONT_SIZE));
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.setAlignment(alignment);
        return cell;
    }

    /**
     * 아래 메뉴(메뉴로 돌아가기, 게임 종료)를 만든다.
     */
    private void createMenu() {
        menuActions.clear();
        menuBox.getChildren().clear();

        List<String> menuTexts = List.of(MENU_BACK_TEXT, MENU_EXIT_TEXT);
        List<Runnable> actions = List.of(onBack, onExit);
        for (int index = 0; index < menuTexts.size(); index++) {
            Runnable action = actions.get(index);
            menuActions.add(action);

            Label leftMarker = new Label(MENU_LEFT_MARKER_TEXT);
            Button button = new Button(menuTexts.get(index));
            button.setPrefWidth(MENU_BUTTON_WIDTH);
            button.setFocusTraversable(false);
            button.setOnAction(event -> action.run());
            Label rightMarker = new Label(MENU_RIGHT_MARKER_TEXT);

            HBox menuRow = new HBox(MENU_MARKER_SPACING, leftMarker, button, rightMarker);
            menuRow.setAlignment(Pos.CENTER);
            menuBox.getChildren().add(menuRow);
        }
    }

    /**
     * 선택된 메뉴 항목의 양옆 마커만 보이게 한다.
     */
    private void updateFocus() {
        modeRow.getChildren().get(0).setVisible(focusedRow == FOCUS_MODE);
        modeRow.getChildren().get(modeRow.getChildren().size() - 1)
            .setVisible(focusedRow == FOCUS_MODE);
        difficultyRow.getChildren().get(0).setVisible(focusedRow == FOCUS_DIFFICULTY);
        difficultyRow.getChildren().get(difficultyRow.getChildren().size() - 1)
            .setVisible(focusedRow == FOCUS_DIFFICULTY);

        for (int index = 0; index < menuBox.getChildren().size(); index++) {
            HBox menuRow = (HBox) menuBox.getChildren().get(index);
            boolean selected = focusedRow == FOCUS_BACK + index;
            menuRow.getChildren().get(0).setVisible(selected);
            menuRow.getChildren().get(2).setVisible(selected);
        }
    }

    /**
     * 키 입력을 처리한다.
     */
    private void handleKey(KeyEvent event) {
        switch (event.getCode()) {
            case LEFT -> {
                event.consume();
                changeValue(-1);
            }
            case RIGHT -> {
                event.consume();
                changeValue(1);
            }
            case UP -> {
                event.consume();
                moveFocus(-1);
            }
            case DOWN -> {
                event.consume();
                moveFocus(1);
            }
            case ENTER -> {
                event.consume();
                if (focusedRow >= FOCUS_BACK) {
                    menuActions.get(focusedRow - FOCUS_BACK).run();
                }
            }
            case ESCAPE -> {
                event.consume();
                onBack.run();
            }
            case Q -> {
                event.consume();
                onExit.run();
            }
            default -> {
            }
        }
    }

    /**
     * 포커스 줄을 이동한다.
     *
     * @param direction 이동할 줄 수
     */
    private void moveFocus(int direction) {
        focusedRow = Math.floorMod(focusedRow + direction, FOCUS_ROW_COUNT);
        updateFocus();
    }

    /**
     * 현재 포커스 줄의 값을 변경한다.
     *
     * @param direction 이동할 값의 수
     */
    private void changeValue(int direction) {
        if (focusedRow == FOCUS_MODE) {
            currentCategory = currentCategory.moveMode(direction);
        } else if (focusedRow == FOCUS_DIFFICULTY) {
            currentCategory = currentCategory.moveDifficulty(direction);
        } else {
            return;
        }

        updateTabs();
        updateRows();
        updateFocus();
    }
}
// AI-assisted code end
