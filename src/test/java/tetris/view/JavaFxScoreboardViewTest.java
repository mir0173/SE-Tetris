package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;

import tetris.scoreboard.ScoreEntry;
import tetris.scoreboard.Scoreboard;

class JavaFxScoreboardViewTest {

    // AI-assisted code start
    private static final String TITLE_TEXT = "스코어보드";
    private static final String EMPTY_TEXT = "기록이 없습니다";
    private static final String BACK_BUTTON_TEXT = "메뉴로";
    private static final String MARKER_TEXT = "★";
    private static final String RANK_SUFFIX = "위";

    private static final int RANK_COLUMN = 0;
    private static final int NAME_COLUMN = 1;
    private static final int SCORE_COLUMN = 2;
    private static final int MARKER_COLUMN = 3;
    private static final int MAX_ROWS = 10;

    private int backCalls;

    @BeforeAll
    static void initializeJavaFx() throws Exception {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException alreadyStarted) {

        }

        runOnFxThread(() -> Platform.setImplicitExit(false));
    }

    @BeforeEach
    void setUp() {
        backCalls = 0;
    }

    @Test
    void showTitleAndBackButton() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(createEntry("alice", 100L)), null);

            // when & then
            assertTrue(hasLabelWithText(view, TITLE_TEXT));
            assertEquals(BACK_BUTTON_TEXT, findBackButton(view).getText());
        });
    }

    @Test
    void showRankNameAndScoreInRankOrder() throws Exception {
        runOnFxThread(() -> {
            // given: 점수가 섞인 순서로 넘긴 기록
            JavaFxScoreboardView view = createView(List.of(
                    createEntry("low", 100L),
                    createEntry("high", 300L),
                    createEntry("middle", 200L)), null);

            // when
            GridPane grid = getGrid(view);

            // then: 점수 높은 순으로 1위부터 표시된다
            assertEquals("1" + RANK_SUFFIX, findCellText(grid, RANK_COLUMN, 0));
            assertEquals("high", findCellText(grid, NAME_COLUMN, 0));
            assertEquals("300", findCellText(grid, SCORE_COLUMN, 0));

            assertEquals("2" + RANK_SUFFIX, findCellText(grid, RANK_COLUMN, 1));
            assertEquals("middle", findCellText(grid, NAME_COLUMN, 1));
            assertEquals("200", findCellText(grid, SCORE_COLUMN, 1));

            assertEquals("3" + RANK_SUFFIX, findCellText(grid, RANK_COLUMN, 2));
            assertEquals("low", findCellText(grid, NAME_COLUMN, 2));
            assertEquals("100", findCellText(grid, SCORE_COLUMN, 2));
        });
    }

    @Test
    void showKoreanName() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(createEntry("홍길동", 500L)), null);

            // when
            GridPane grid = getGrid(view);

            // then
            assertEquals("홍길동", findCellText(grid, NAME_COLUMN, 0));
        });
    }

    @Test
    void showOnlyTopEntriesWhenMoreThanMaximum() throws Exception {
        runOnFxThread(() -> {
            // given: 기록 12개
            List<ScoreEntry> entries = new ArrayList<>();
            for (long score = 1L; score <= MAX_ROWS + 2; score++) {
                entries.add(createEntry("player" + score, score));
            }
            JavaFxScoreboardView view = createView(entries, null);

            // when
            GridPane grid = getGrid(view);

            // then: 상위 10개만 표시된다
            assertEquals(MAX_ROWS, countCellsInColumn(grid, RANK_COLUMN));
        });
    }

    @Test
    void showMarkerOnlyOnHighlightedRow() throws Exception {
        runOnFxThread(() -> {
            // given: 2등이 될 기록을 강조 대상으로 지정
            ScoreEntry highlighted = createEntry("me", 200L);
            JavaFxScoreboardView view = createView(List.of(
                    createEntry("first", 300L),
                    highlighted,
                    createEntry("third", 100L)), highlighted.getId());

            // when
            GridPane grid = getGrid(view);

            // then: 별 표시는 강조 대상 줄(2등, 행 번호 1)에만 하나 있다
            assertEquals(1, countMarkers(grid));
            assertEquals(MARKER_TEXT, findCellText(grid, MARKER_COLUMN, 1));
            assertNull(findCellText(grid, MARKER_COLUMN, 0));
            assertNull(findCellText(grid, MARKER_COLUMN, 2));
        });
    }

    @Test
    void showNoMarkerWhenHighlightIdIsNull() throws Exception {
        runOnFxThread(() -> {
            // given: 메뉴에서 스코어보드를 열 때처럼 강조 대상이 없는 경우
            JavaFxScoreboardView view = createView(List.of(createEntry("alice", 100L)), null);

            // when
            GridPane grid = getGrid(view);

            // then
            assertEquals(0, countMarkers(grid));
        });
    }

    @Test
    void showNoMarkerWhenHighlightIdIsNotOnBoard() throws Exception {
        runOnFxThread(() -> {
            // given: 순위 밖이라 스코어보드에 없는 id
            JavaFxScoreboardView view = createView(
                    List.of(createEntry("alice", 100L)), UUID.randomUUID());

            // when
            GridPane grid = getGrid(view);

            // then
            assertEquals(0, countMarkers(grid));
        });
    }

    @Test
    void showEmptyMessageWhenNoEntries() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), null);

            // when & then: 순위 표 대신 안내 문구를 보여 준다
            assertTrue(hasLabelWithText(view, EMPTY_TEXT));
            assertFalse(findGrid(view).isPresent());
        });
    }

    @Test
    void notShowEmptyMessageWhenEntriesExist() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(createEntry("alice", 100L)), null);

            // when & then
            assertFalse(hasLabelWithText(view, EMPTY_TEXT));
        });
    }

    @Test
    void backButtonRunsOnBack() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(createEntry("alice", 100L)), null);

            // when
            findBackButton(view).fire();

            // then
            assertEquals(1, backCalls);
        });
    }

    @ParameterizedTest
    @CsvSource({
            "ESCAPE",
            "ENTER"
    })
    void keyRunsOnBack(KeyCode key) throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(createEntry("alice", 100L)), null);

            // when
            pressKey(view, key);

            // then: ESC, Enter 로 메뉴로 돌아간다
            assertEquals(1, backCalls);
        });
    }

    @ParameterizedTest
    @CsvSource({
            "UP",
            "DOWN",
            "SPACE",
            "A"
    })
    void otherKeyDoesNotRunOnBack(KeyCode key) throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(createEntry("alice", 100L)), null);

            // when
            pressKey(view, key);

            // then
            assertEquals(0, backCalls);
        });
    }

    private JavaFxScoreboardView createView(List<ScoreEntry> entries, UUID highlightedEntryId) {
        return new JavaFxScoreboardView(new Scoreboard(entries), highlightedEntryId, () -> backCalls++);
    }

    private ScoreEntry createEntry(String name, long score) {
        return new ScoreEntry(UUID.randomUUID(), name, score);
    }

    private Optional<GridPane> findGrid(JavaFxScoreboardView view) {
        return view.getScene()
                .getRoot()
                .getChildrenUnmodifiable()
                .stream()
                .filter(GridPane.class::isInstance)
                .map(GridPane.class::cast)
                .findFirst();
    }

    private GridPane getGrid(JavaFxScoreboardView view) {
        return findGrid(view).orElseThrow(
                () -> new AssertionError("순위 표를 찾을 수 없음"));
    }

    private Button findBackButton(JavaFxScoreboardView view) {
        return view.getScene()
                .getRoot()
                .getChildrenUnmodifiable()
                .stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> BACK_BUTTON_TEXT.equals(button.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("버튼을 찾을 수 없음: " + BACK_BUTTON_TEXT));
    }

    /**
     * 화면 바로 아래에 있는 Label 중 해당 문구를 가진 것이 있는지 확인한다.
     */
    private boolean hasLabelWithText(JavaFxScoreboardView view, String text) {
        return view.getScene()
                .getRoot()
                .getChildrenUnmodifiable()
                .stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .anyMatch(label -> text.equals(label.getText()));
    }

    /**
     * 순위 표의 (열, 행) 칸에 있는 글자를 반환한다. 해당 칸이 비어 있으면 null 이다.
     */
    private String findCellText(GridPane grid, int column, int row) {
        for (Node child : grid.getChildren()) {
            if (GridPane.getColumnIndex(child) == column && GridPane.getRowIndex(child) == row) {
                return ((Label) child).getText();
            }
        }

        return null;
    }

    private int countCellsInColumn(GridPane grid, int column) {
        int count = 0;

        for (Node child : grid.getChildren()) {
            if (GridPane.getColumnIndex(child) == column) {
                count++;
            }
        }

        return count;
    }

    private int countMarkers(GridPane grid) {
        int count = 0;

        for (Node child : grid.getChildren()) {
            if (child instanceof Label label && MARKER_TEXT.equals(label.getText())) {
                count++;
            }
        }

        return count;
    }

    private void pressKey(JavaFxScoreboardView view, KeyCode key) {
        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "",
                "",
                key,
                false,
                false,
                false,
                false
        );

        Event.fireEvent(view.getScene().getRoot(), event);
    }

    private static void runOnFxThread(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);

        try {
            task.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof Error error) {
                throw error;
            }

            if (cause instanceof Exception error) {
                throw error;
            }

            throw new RuntimeException(cause);
        }
    }
    // AI-assisted code end
}
