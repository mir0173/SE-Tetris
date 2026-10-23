package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
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
import javafx.scene.layout.Border;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import tetris.model.Difficulty;
import tetris.model.GameMode;
import tetris.scoreboard.ScoreCategory;
import tetris.scoreboard.ScoreEntry;
import tetris.scoreboard.ScoreboardGroup;

/**
 * 스코어보드 화면 테스트.
 *
 * 화면 구성 약속 (테스트가 노드를 찾는 방법):
 * - 제목 Label, #modeRow(모드 탭), #difficultyRow(난이도 탭), #scoreGrid(10줄 순위 표), #menuBox(아래 메뉴)
 * - 선택된 탭 Label은 아래쪽 테두리(Border)가 있고, 선택되지 않은 탭은 테두리가 없다
 * - 순위 표 열: 0 순위, 1 이름, 2 점선, 3 점수, 4 별(★)
 * - 포커스 줄은 4개: 0 모드, 1 난이도, 2 메뉴로 돌아가기, 3 게임 종료 (처음 포커스는 0)
 * - 모드 줄과 난이도 줄은 HBox이고 첫 자식이 ◀ Label, 마지막 자식이 ▶ Label이며 포커스된 줄만 화살표가 보인다
 * - 메뉴 행은 HBox [왼쪽 마커 Label, Button, 오른쪽 마커 Label]이고 포커스된 행만 마커가 보인다
 * - ←/→는 포커스가 모드 줄이면 모드만, 난이도 줄이면 난이도만 바꾼다 (각각 양 끝이 이어짐)
 */
class JavaFxScoreboardViewTest {

    // AI-assisted code start
    private static final String TITLE_TEXT = "★ H I G H  S C O R E ★";
    private static final String NORMAL_MODE_TEXT = "일 반";
    private static final String ITEM_MODE_TEXT = "아이템";
    private static final String EASY_TEXT = "EASY";
    private static final String NORMAL_TEXT = "NORMAL";
    private static final String HARD_TEXT = "HARD";
    private static final String LEFT_ARROW_TEXT = "◀";
    private static final String RIGHT_ARROW_TEXT = "▶";
    private static final String BACK_BUTTON_TEXT = "메뉴로 돌아가기";
    private static final String EXIT_BUTTON_TEXT = "게임 종료";
    private static final String MARKER_TEXT = "★";
    private static final String EMPTY_NAME_TEXT = "---";
    private static final String EMPTY_SCORE_TEXT = "000000";
    private static final String LEADER_TEXT = "·········";

    private static final String MODE_ROW_ID = "#modeRow";
    private static final String DIFFICULTY_ROW_ID = "#difficultyRow";
    private static final String SCORE_GRID_ID = "#scoreGrid";
    private static final String MENU_BOX_ID = "#menuBox";

    private static final int RANK_COLUMN = 0;
    private static final int NAME_COLUMN = 1;
    private static final int LEADER_COLUMN = 2;
    private static final int SCORE_COLUMN = 3;
    private static final int MARKER_COLUMN = 4;
    private static final int MAX_ROWS = 10;
    private static final int BACK_MENU_INDEX = 0;
    private static final int EXIT_MENU_INDEX = 1;
    private static final int MENU_COUNT = 2;
    private static final int COLUMN_COUNT = 5;
    private static final int FOCUS_MODE = 0;
    private static final int FOCUS_DIFFICULTY = 1;
    private static final int FOCUS_BACK = 2;
    private static final int FOCUS_EXIT = 3;
    private static final int FOCUS_COUNT = 4;

    private static final ScoreCategory NORMAL_EASY = new ScoreCategory(GameMode.NORMAL, Difficulty.EASY);
    private static final ScoreCategory NORMAL_NORMAL = new ScoreCategory(GameMode.NORMAL, Difficulty.NORMAL);
    private static final ScoreCategory NORMAL_HARD = new ScoreCategory(GameMode.NORMAL, Difficulty.HARD);
    private static final ScoreCategory ITEM_EASY = new ScoreCategory(GameMode.ITEM, Difficulty.EASY);
    private static final ScoreCategory ITEM_NORMAL = new ScoreCategory(GameMode.ITEM, Difficulty.NORMAL);
    private static final ScoreCategory ITEM_HARD = new ScoreCategory(GameMode.ITEM, Difficulty.HARD);

    private int backCalls;
    private int exitCalls;

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
        exitCalls = 0;
    }

    // ---------- 제목, 탭 ----------

    @Test
    void showTitle() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when & then
            assertTrue(hasLabelWithText(view, TITLE_TEXT));
        });
    }

    @Test
    void showModeAndDifficultyTabs() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when
            HBox modeRow = getRow(view, MODE_ROW_ID);
            HBox difficultyRow = getRow(view, DIFFICULTY_ROW_ID);

            // then: 모드 2개와 난이도 3개, 두 줄 모두 양옆에 이동 안내 화살표가 있다
            assertNotNull(findLabel(modeRow, NORMAL_MODE_TEXT));
            assertNotNull(findLabel(modeRow, ITEM_MODE_TEXT));
            assertNotNull(findLabel(modeRow, LEFT_ARROW_TEXT));
            assertNotNull(findLabel(modeRow, RIGHT_ARROW_TEXT));
            assertNotNull(findLabel(difficultyRow, EASY_TEXT));
            assertNotNull(findLabel(difficultyRow, NORMAL_TEXT));
            assertNotNull(findLabel(difficultyRow, HARD_TEXT));
            assertNotNull(findLabel(difficultyRow, LEFT_ARROW_TEXT));
            assertNotNull(findLabel(difficultyRow, RIGHT_ARROW_TEXT));
        });
    }

    @Test
    void underlineModeAndDifficultyOfInitialCategory() throws Exception {
        runOnFxThread(() -> {
            // given & when & then: 처음 보여줄 조합 6가지 모두 해당 탭에만 밑줄이 있다
            for (ScoreCategory category : ScoreCategory.getAllCategories()) {
                JavaFxScoreboardView view = createView(List.of(), category, null);

                assertSelectedTabs(view, category);
            }
        });
    }

    @Test
    void focusModeRowAtStart() throws Exception {
        runOnFxThread(() -> {
            // given & when
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // then: 처음 포커스는 모드 줄이다
            assertEquals(FOCUS_MODE, findFocusedRowIndex(view));
        });
    }

    @Test
    void showArrowsOnlyAroundFocusedTabRow() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when & then: 처음에는 모드 줄에만 ◀ ▶가 보인다
            assertTrue(isArrowVisible(getRow(view, MODE_ROW_ID)));
            assertFalse(isArrowVisible(getRow(view, DIFFICULTY_ROW_ID)));

            // when & then: 아래로 내려가면 난이도 줄에만 보인다
            pressKey(view, KeyCode.DOWN);

            assertFalse(isArrowVisible(getRow(view, MODE_ROW_ID)));
            assertTrue(isArrowVisible(getRow(view, DIFFICULTY_ROW_ID)));
        });
    }

    @Test
    void changeModeWithArrowKeysOnModeRow() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_EASY, null);

            // when & then: →로 아이템, ←로 다시 일반
            pressKey(view, KeyCode.RIGHT);
            assertSelectedTabs(view, ITEM_EASY);

            pressKey(view, KeyCode.LEFT);
            assertSelectedTabs(view, NORMAL_EASY);
        });
    }

    @Test
    void keepDifficultyWhenChangingMode() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_HARD, null);

            // when
            pressKey(view, KeyCode.RIGHT);

            // then: 같은 난이도(HARD)에서 모드만 아이템으로 바뀐다
            assertSelectedTabs(view, ITEM_HARD);
        });
    }

    @Test
    void wrapModeAroundBothEnds() throws Exception {
        runOnFxThread(() -> {
            // given: 마지막 모드(아이템)
            JavaFxScoreboardView lastView = createView(List.of(), ITEM_NORMAL, null);

            // when & then: 아이템에서 →는 일반으로
            pressKey(lastView, KeyCode.RIGHT);
            assertSelectedTabs(lastView, NORMAL_NORMAL);

            // given: 첫 번째 모드(일반)
            JavaFxScoreboardView firstView = createView(List.of(), NORMAL_NORMAL, null);

            // when & then: 일반에서 ←는 아이템으로
            pressKey(firstView, KeyCode.LEFT);
            assertSelectedTabs(firstView, ITEM_NORMAL);
        });
    }

    @Test
    void changeDifficultyWithArrowKeysOnDifficultyRow() throws Exception {
        runOnFxThread(() -> {
            // given: 난이도 줄로 내려간다
            JavaFxScoreboardView view = createView(List.of(), NORMAL_EASY, null);
            pressKey(view, KeyCode.DOWN);

            // when & then: EASY → NORMAL → HARD, ←로 다시 NORMAL
            pressKey(view, KeyCode.RIGHT);
            assertSelectedTabs(view, NORMAL_NORMAL);

            pressKey(view, KeyCode.RIGHT);
            assertSelectedTabs(view, NORMAL_HARD);

            pressKey(view, KeyCode.LEFT);
            assertSelectedTabs(view, NORMAL_NORMAL);
        });
    }

    @Test
    void keepModeWhenChangingDifficulty() throws Exception {
        runOnFxThread(() -> {
            // given: 아이템 모드에서 난이도 줄로 내려간다
            JavaFxScoreboardView view = createView(List.of(), ITEM_EASY, null);
            pressKey(view, KeyCode.DOWN);

            // when
            pressKey(view, KeyCode.RIGHT);

            // then: 난이도만 바뀌고 모드는 아이템 그대로
            assertSelectedTabs(view, ITEM_NORMAL);
        });
    }

    @Test
    void wrapDifficultyAroundBothEnds() throws Exception {
        runOnFxThread(() -> {
            // given: 마지막 난이도(HARD)
            JavaFxScoreboardView lastView = createView(List.of(), NORMAL_HARD, null);
            pressKey(lastView, KeyCode.DOWN);

            // when & then: HARD에서 →는 EASY로 (모드 그대로)
            pressKey(lastView, KeyCode.RIGHT);
            assertSelectedTabs(lastView, NORMAL_EASY);

            // given: 첫 번째 난이도(EASY)
            JavaFxScoreboardView firstView = createView(List.of(), ITEM_EASY, null);
            pressKey(firstView, KeyCode.DOWN);

            // when & then: EASY에서 ←는 HARD로 (모드 그대로)
            pressKey(firstView, KeyCode.LEFT);
            assertSelectedTabs(firstView, ITEM_HARD);
        });
    }

    @Test
    void visitEveryDifficultyWhenPressingRightRepeatedlyOnDifficultyRow() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_EASY, null);
            pressKey(view, KeyCode.DOWN);

            // when & then: 3번 누르면 NORMAL, HARD, EASY 순서로 지나 처음으로 돌아온다
            pressKey(view, KeyCode.RIGHT);
            assertSelectedTabs(view, NORMAL_NORMAL);
            pressKey(view, KeyCode.RIGHT);
            assertSelectedTabs(view, NORMAL_HARD);
            pressKey(view, KeyCode.RIGHT);
            assertSelectedTabs(view, NORMAL_EASY);
        });
    }

    @Test
    void notChangeCategoryWhenPressingLeftRightOnMenuRows() throws Exception {
        runOnFxThread(() -> {
            // given: 메뉴로 돌아가기 줄로 내려간다
            JavaFxScoreboardView view = createView(List.of(), ITEM_NORMAL, null);
            pressKeyTimes(view, KeyCode.DOWN, FOCUS_BACK);

            // when
            pressKey(view, KeyCode.LEFT);
            pressKey(view, KeyCode.RIGHT);

            // then: 메뉴 줄에서는 ←/→가 조합을 바꾸지 않고 포커스도 그대로
            assertSelectedTabs(view, ITEM_NORMAL);
            assertEquals(FOCUS_BACK, findFocusedRowIndex(view));
        });
    }

    // ---------- 순위 표 ----------

    @Test
    void showRankNameLeaderAndScoreInRankOrder() throws Exception {
        runOnFxThread(() -> {
            // given: 점수가 섞인 순서로 넘긴 기록
            JavaFxScoreboardView view = createView(List.of(
                    createEntry("low", 100L),
                    createEntry("high", 300L),
                    createEntry("middle", 200L)), NORMAL_NORMAL, null);

            // when
            GridPane grid = getGrid(view);

            // then: 점수 높은 순으로 순위는 두 자리, 점수는 6자리로 표시되고 이름과 점수 사이에 점선이 있다
            assertEquals("01", findCellText(grid, RANK_COLUMN, 0));
            assertEquals("high", findCellText(grid, NAME_COLUMN, 0));
            assertEquals(LEADER_TEXT, findCellText(grid, LEADER_COLUMN, 0));
            assertEquals("000300", findCellText(grid, SCORE_COLUMN, 0));

            assertEquals("02", findCellText(grid, RANK_COLUMN, 1));
            assertEquals("middle", findCellText(grid, NAME_COLUMN, 1));
            assertEquals("000200", findCellText(grid, SCORE_COLUMN, 1));

            assertEquals("03", findCellText(grid, RANK_COLUMN, 2));
            assertEquals("low", findCellText(grid, NAME_COLUMN, 2));
            assertEquals("000100", findCellText(grid, SCORE_COLUMN, 2));
        });
    }

    @Test
    void showKoreanName() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(createEntry("홍길동", 500L)), NORMAL_NORMAL, null);

            // when
            GridPane grid = getGrid(view);

            // then
            assertEquals("홍길동", findCellText(grid, NAME_COLUMN, 0));
        });
    }

    @Test
    void showScoreWithoutTruncationWhenLongerThanSixDigits() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(createEntry("big", 1234567L)), NORMAL_NORMAL, null);

            // when
            GridPane grid = getGrid(view);

            // then: 6자리보다 길어도 잘리지 않는다
            assertEquals("1234567", findCellText(grid, SCORE_COLUMN, 0));
        });
    }

    @Test
    void fillRemainingRowsWithPlaceholdersToKeepTenRows() throws Exception {
        runOnFxThread(() -> {
            // given: 기록 3개
            JavaFxScoreboardView view = createView(List.of(
                    createEntry("first", 300L),
                    createEntry("second", 200L),
                    createEntry("third", 100L)), NORMAL_NORMAL, null);

            // when
            GridPane grid = getGrid(view);

            // then: 항상 10줄이고, 나머지 줄은 순위만 있는 빈 줄이다
            assertEquals(MAX_ROWS, countCellsInColumn(grid, RANK_COLUMN));
            assertEquals("third", findCellText(grid, NAME_COLUMN, 2));
            for (int row = 3; row < MAX_ROWS; row++) {
                assertEquals(String.format("%02d", row + 1), findCellText(grid, RANK_COLUMN, row));
                assertEquals(EMPTY_NAME_TEXT, findCellText(grid, NAME_COLUMN, row));
                assertEquals(LEADER_TEXT, findCellText(grid, LEADER_COLUMN, row));
                assertEquals(EMPTY_SCORE_TEXT, findCellText(grid, SCORE_COLUMN, row));
            }
        });
    }

    @Test
    void showTenPlaceholderRowsWhenNoEntries() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when
            GridPane grid = getGrid(view);

            // then: 기록이 없어도 10줄이 모두 빈 줄이다
            assertEquals(MAX_ROWS, countCellsInColumn(grid, RANK_COLUMN));
            for (int row = 0; row < MAX_ROWS; row++) {
                assertEquals(EMPTY_NAME_TEXT, findCellText(grid, NAME_COLUMN, row));
                assertEquals(EMPTY_SCORE_TEXT, findCellText(grid, SCORE_COLUMN, row));
            }
        });
    }

    @Test
    void showOnlyTopEntriesWhenMoreThanMaximum() throws Exception {
        runOnFxThread(() -> {
            // given: 한 조합에 기록 12개
            List<ScoreEntry> entries = new ArrayList<>();
            for (long score = 1L; score <= MAX_ROWS + 2; score++) {
                entries.add(createEntry("player" + score, score));
            }
            JavaFxScoreboardView view = createView(entries, NORMAL_NORMAL, null);

            // when
            GridPane grid = getGrid(view);

            // then: 상위 10개만 표시된다 (12점이 1위, 3점이 10위)
            assertEquals(MAX_ROWS, countCellsInColumn(grid, RANK_COLUMN));
            assertEquals("player12", findCellText(grid, NAME_COLUMN, 0));
            assertEquals("player3", findCellText(grid, NAME_COLUMN, MAX_ROWS - 1));
        });
    }

    @Test
    void showOnlyEntriesOfCurrentCategory() throws Exception {
        runOnFxThread(() -> {
            // given: 서로 다른 조합의 기록
            JavaFxScoreboardView view = createView(List.of(
                    createEntry("normalEasy", 100L, NORMAL_EASY),
                    createEntry("itemEasy", 200L, ITEM_EASY)), NORMAL_EASY, null);

            // when
            GridPane grid = getGrid(view);

            // then: 현재 조합의 기록만 보인다
            assertEquals("normalEasy", findCellText(grid, NAME_COLUMN, 0));
            assertEquals(EMPTY_NAME_TEXT, findCellText(grid, NAME_COLUMN, 1));
        });
    }

    @Test
    void changeRowsWhenMovingToAnotherCategory() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(
                    createEntry("normalEasy", 100L, NORMAL_EASY),
                    createEntry("normalNormal", 200L, NORMAL_NORMAL)), NORMAL_EASY, null);

            // when: 난이도 줄로 내려가서 →로 난이도를 NORMAL로 바꾼다
            pressKey(view, KeyCode.DOWN);
            pressKey(view, KeyCode.RIGHT);
            GridPane grid = getGrid(view);

            // then: 일반 NORMAL의 기록이 보인다
            assertEquals("normalNormal", findCellText(grid, NAME_COLUMN, 0));
            assertEquals(EMPTY_NAME_TEXT, findCellText(grid, NAME_COLUMN, 1));
        });
    }

    @Test
    void showPlaceholderRowsForCategoryWithoutEntries() throws Exception {
        runOnFxThread(() -> {
            // given: 일반 EASY에만 기록이 있다
            JavaFxScoreboardView view = createView(List.of(
                    createEntry("normalEasy", 100L, NORMAL_EASY)), NORMAL_EASY, null);

            // when: 모드를 아이템으로 바꿔 기록이 없는 조합으로 넘긴다
            pressKey(view, KeyCode.RIGHT);
            GridPane grid = getGrid(view);

            // then: 10줄 모두 빈 줄
            for (int row = 0; row < MAX_ROWS; row++) {
                assertEquals(EMPTY_NAME_TEXT, findCellText(grid, NAME_COLUMN, row));
            }
        });
    }

    @Test
    void keepTenRowsWhenMovingThroughEveryCategory() throws Exception {
        runOnFxThread(() -> {
            // given: 조합마다 기록 수가 다르다
            List<ScoreEntry> entries = new ArrayList<>();
            entries.add(createEntry("a", 100L, NORMAL_EASY));
            for (long score = 1L; score <= MAX_ROWS + 2; score++) {
                entries.add(createEntry("b" + score, score, ITEM_EASY));
            }
            JavaFxScoreboardView view = createView(entries, NORMAL_EASY, null);

            // when & then: 어느 조합으로 넘겨도 항상 10줄이라 화면 높이가 변하지 않는다
            // (모드 줄에서 일반 → 아이템, 난이도 줄에서 EASY → NORMAL → HARD를 차례로 본다)
            assertEquals(MAX_ROWS, countCellsInColumn(getGrid(view), RANK_COLUMN));
            pressKey(view, KeyCode.RIGHT);
            assertEquals(MAX_ROWS, countCellsInColumn(getGrid(view), RANK_COLUMN));
            pressKey(view, KeyCode.DOWN);
            for (int i = 0; i < Difficulty.values().length; i++) {
                pressKey(view, KeyCode.RIGHT);
                assertEquals(MAX_ROWS, countCellsInColumn(getGrid(view), RANK_COLUMN));
            }
        });
    }

    // ---------- 방금 기록 강조 (★) ----------

    @Test
    void showMarkerOnlyOnHighlightedRow() throws Exception {
        runOnFxThread(() -> {
            // given: 2등이 될 기록을 강조 대상으로 지정
            ScoreEntry highlighted = createEntry("me", 200L);
            JavaFxScoreboardView view = createView(List.of(
                    createEntry("first", 300L),
                    highlighted,
                    createEntry("third", 100L)), NORMAL_NORMAL, highlighted.getId());

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
            // given
            JavaFxScoreboardView view = createView(List.of(createEntry("alice", 100L)), NORMAL_NORMAL, null);

            // when
            GridPane grid = getGrid(view);

            // then
            assertEquals(0, countMarkers(grid));
        });
    }

    @Test
    void showNoMarkerWhenHighlightIdIsNotOnBoard() throws Exception {
        runOnFxThread(() -> {
            // given: 어느 조합에도 없는 id
            JavaFxScoreboardView view = createView(
                    List.of(createEntry("alice", 100L)), NORMAL_NORMAL, UUID.randomUUID());

            // when
            GridPane grid = getGrid(view);

            // then
            assertEquals(0, countMarkers(grid));
        });
    }

    @Test
    void showMarkerOnlyWhenHighlightedEntryCategoryIsShown() throws Exception {
        runOnFxThread(() -> {
            // given: 강조할 기록은 아이템 HARD에 있고, 화면은 일반 EASY에서 시작한다
            ScoreEntry highlighted = createEntry("me", 200L, ITEM_HARD);
            JavaFxScoreboardView view = createView(List.of(
                    createEntry("other", 300L, NORMAL_EASY),
                    highlighted), NORMAL_EASY, highlighted.getId());

            // when & then: 다른 조합에서는 별이 없다가, 그 조합(아이템 HARD)으로 넘기면 별이 나타난다
            assertEquals(0, countMarkers(getGrid(view)));

            pressKey(view, KeyCode.LEFT);
            pressKey(view, KeyCode.DOWN);
            pressKey(view, KeyCode.LEFT);

            assertSelectedTabs(view, ITEM_HARD);
            assertEquals(1, countMarkers(getGrid(view)));
            assertEquals(MARKER_TEXT, findCellText(getGrid(view), MARKER_COLUMN, 0));
        });
    }

    @Test
    void showNoMarkerOnPlaceholderRows() throws Exception {
        runOnFxThread(() -> {
            // given
            ScoreEntry highlighted = createEntry("me", 100L);
            JavaFxScoreboardView view = createView(List.of(highlighted), NORMAL_NORMAL, highlighted.getId());

            // when
            GridPane grid = getGrid(view);

            // then: 별은 실제 기록 줄에만 있고 빈 줄에는 없다
            assertEquals(1, countMarkers(grid));
            for (int row = 1; row < MAX_ROWS; row++) {
                assertNull(findCellText(grid, MARKER_COLUMN, row));
            }
        });
    }

    // ---------- 포커스와 아래 메뉴 ----------

    @Test
    void showBackAndExitMenuItemsInOrder() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when
            VBox menuBox = getMenuBox(view);

            // then: 메뉴로 돌아가기, 게임 종료 두 항목이 이 순서로 있다
            assertEquals(MENU_COUNT, menuBox.getChildren().size());
            assertEquals(BACK_BUTTON_TEXT, getMenuButton(view, BACK_MENU_INDEX).getText());
            assertEquals(EXIT_BUTTON_TEXT, getMenuButton(view, EXIT_MENU_INDEX).getText());
        });
    }

    @Test
    void hideMenuMarkersWhenFocusIsOnTabRows() throws Exception {
        runOnFxThread(() -> {
            // given & when: 처음 포커스는 모드 줄
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // then: 두 메뉴 항목 모두 마커가 숨겨져 있다
            for (int i = 0; i < MENU_COUNT; i++) {
                assertFalse(getLeftMarker(getMenuRow(view, i)).isVisible());
                assertFalse(getRightMarker(getMenuRow(view, i)).isVisible());
            }
        });
    }

    @Test
    void showMarkersAroundFocusedMenuItemOnly() throws Exception {
        runOnFxThread(() -> {
            // given: 메뉴로 돌아가기 줄로 내려간다
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);
            pressKeyTimes(view, KeyCode.DOWN, FOCUS_BACK);

            // when
            HBox focusedRow = getMenuRow(view, BACK_MENU_INDEX);
            HBox otherRow = getMenuRow(view, EXIT_MENU_INDEX);

            // then: 포커스된 항목은 양옆에 ▶ ◀가 보이고 다른 항목은 마커가 숨겨진다
            assertEquals(RIGHT_ARROW_TEXT, getLeftMarker(focusedRow).getText());
            assertEquals(LEFT_ARROW_TEXT, getRightMarker(focusedRow).getText());
            assertTrue(getLeftMarker(focusedRow).isVisible());
            assertTrue(getRightMarker(focusedRow).isVisible());
            assertFalse(getLeftMarker(otherRow).isVisible());
            assertFalse(getRightMarker(otherRow).isVisible());
        });
    }

    @Test
    void moveFocusDownThroughAllRowsAndWrapToFirst() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when & then: 모드 → 난이도 → 메뉴로 → 종료, 맨 아래에서 ↓는 맨 위로
            assertEquals(FOCUS_MODE, findFocusedRowIndex(view));
            pressKey(view, KeyCode.DOWN);
            assertEquals(FOCUS_DIFFICULTY, findFocusedRowIndex(view));
            pressKey(view, KeyCode.DOWN);
            assertEquals(FOCUS_BACK, findFocusedRowIndex(view));
            pressKey(view, KeyCode.DOWN);
            assertEquals(FOCUS_EXIT, findFocusedRowIndex(view));
            pressKey(view, KeyCode.DOWN);
            assertEquals(FOCUS_MODE, findFocusedRowIndex(view));
        });
    }

    @Test
    void moveFocusUpThroughAllRowsAndWrapToLast() throws Exception {
        runOnFxThread(() -> {
            // given: 첫 번째 줄(모드)에 포커스
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when & then: 맨 위에서 ↑는 맨 아래(종료)로, 이어서 위로 올라간다
            pressKey(view, KeyCode.UP);
            assertEquals(FOCUS_EXIT, findFocusedRowIndex(view));
            pressKey(view, KeyCode.UP);
            assertEquals(FOCUS_BACK, findFocusedRowIndex(view));
            pressKey(view, KeyCode.UP);
            assertEquals(FOCUS_DIFFICULTY, findFocusedRowIndex(view));
            pressKey(view, KeyCode.UP);
            assertEquals(FOCUS_MODE, findFocusedRowIndex(view));
        });
    }

    @Test
    void keepCategoryWhenMovingFocus() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), ITEM_EASY, null);

            // when: 모든 줄을 한 바퀴 돈다
            pressKeyTimes(view, KeyCode.DOWN, FOCUS_COUNT);

            // then: 포커스만 이동했을 뿐 보고 있는 조합은 그대로
            assertSelectedTabs(view, ITEM_EASY);
        });
    }

    @Test
    void keepFocusWhenChangingValue() throws Exception {
        runOnFxThread(() -> {
            // given & when & then: 모드를 바꿔도 포커스는 모드 줄에 남는다
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);
            pressKey(view, KeyCode.RIGHT);
            assertEquals(FOCUS_MODE, findFocusedRowIndex(view));

            // when & then: 난이도를 바꿔도 포커스는 난이도 줄에 남는다
            pressKey(view, KeyCode.DOWN);
            pressKey(view, KeyCode.RIGHT);
            assertEquals(FOCUS_DIFFICULTY, findFocusedRowIndex(view));
        });
    }

    // ---------- 실행 (Enter, ESC, Q, 버튼) ----------

    @Test
    void enterDoesNothingOnTabRows() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when: 모드 줄과 난이도 줄에서 Enter
            pressKey(view, KeyCode.ENTER);
            pressKey(view, KeyCode.DOWN);
            pressKey(view, KeyCode.ENTER);

            // then: 아무 동작도 하지 않는다
            assertEquals(0, backCalls);
            assertEquals(0, exitCalls);
        });
    }

    @Test
    void enterRunsOnBackWhenBackItemIsFocused() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);
            pressKeyTimes(view, KeyCode.DOWN, FOCUS_BACK);

            // when
            pressKey(view, KeyCode.ENTER);

            // then
            assertEquals(1, backCalls);
            assertEquals(0, exitCalls);
        });
    }

    @Test
    void enterRunsOnExitWhenExitItemIsFocused() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);
            pressKeyTimes(view, KeyCode.DOWN, FOCUS_EXIT);

            // when
            pressKey(view, KeyCode.ENTER);

            // then
            assertEquals(0, backCalls);
            assertEquals(1, exitCalls);
        });
    }

    @Test
    void escapeRunsOnBackFromAnyFocusedRow() throws Exception {
        runOnFxThread(() -> {
            for (int focusIndex = 0; focusIndex < FOCUS_COUNT; focusIndex++) {
                // given: 모드, 난이도, 메뉴로, 종료 줄 각각에 포커스
                backCalls = 0;
                exitCalls = 0;
                JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);
                pressKeyTimes(view, KeyCode.DOWN, focusIndex);

                // when
                pressKey(view, KeyCode.ESCAPE);

                // then: ESC는 포커스와 상관없이 메뉴로 돌아간다
                assertEquals(1, backCalls, "포커스 줄: " + focusIndex);
                assertEquals(0, exitCalls, "포커스 줄: " + focusIndex);
            }
        });
    }

    @Test
    void qRunsOnExit() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when
            pressKey(view, KeyCode.Q);

            // then: Q는 게임 종료 단축키로 유지된다
            assertEquals(0, backCalls);
            assertEquals(1, exitCalls);
        });
    }

    @Test
    void backButtonRunsOnBack() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when: 마우스 클릭처럼 버튼을 직접 실행
            getMenuButton(view, BACK_MENU_INDEX).fire();

            // then
            assertEquals(1, backCalls);
            assertEquals(0, exitCalls);
        });
    }

    @Test
    void exitButtonRunsOnExit() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when
            getMenuButton(view, EXIT_MENU_INDEX).fire();

            // then
            assertEquals(0, backCalls);
            assertEquals(1, exitCalls);
        });
    }

    @ParameterizedTest
    @CsvSource({
            "LEFT",
            "RIGHT",
            "UP",
            "DOWN",
            "SPACE",
            "A"
    })
    void otherKeysDoNotRunCallbacks(KeyCode key) throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when
            pressKey(view, key);

            // then: 이동 키와 관련 없는 키는 돌아가기, 종료를 실행하지 않는다
            assertEquals(0, backCalls);
            assertEquals(0, exitCalls);
        });
    }

    @ParameterizedTest
    @CsvSource({
            "LEFT",
            "RIGHT",
            "UP",
            "DOWN",
            "ENTER",
            "ESCAPE",
            "Q"
    })
    void consumeHandledKeys(KeyCode key) throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when
            boolean reachedTarget = pressKeyAndCheckReachedTarget(view, key);

            // then: 처리한 키는 화면이 소비해서 버튼 포커스 이동 같은 기본 동작으로 넘어가지 않는다
            assertFalse(reachedTarget);
        });
    }

    @Test
    void notConsumeUnrelatedKeys() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when
            boolean reachedTarget = pressKeyAndCheckReachedTarget(view, KeyCode.A);

            // then
            assertTrue(reachedTarget);
        });
    }

    // ---------- 생성자 ----------

    @Test
    void rejectNullScoreboardGroup() throws Exception {
        runOnFxThread(() -> assertThrows(NullPointerException.class,
                () -> new JavaFxScoreboardView(null, NORMAL_NORMAL, null, () -> backCalls++, () -> exitCalls++)));
    }

    @Test
    void rejectNullInitialCategory() throws Exception {
        runOnFxThread(() -> assertThrows(NullPointerException.class,
                () -> new JavaFxScoreboardView(
                        new ScoreboardGroup(List.of()), null, null, () -> backCalls++, () -> exitCalls++)));
    }

    // ---------- 레이아웃 ----------

    @Test
    void showTitleLargerThanTableText() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(createEntry("alice", 100L)), NORMAL_NORMAL, null);

            // when
            Label title = findTitleLabel(view);
            Label tableCell = findCellLabel(getGrid(view), NAME_COLUMN, 0);

            // then: 제목이 순위 표 글자보다 크다
            assertTrue(title.getFont().getSize() > tableCell.getFont().getSize());
        });
    }

    @Test
    void fixTableColumnWidths() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when
            List<ColumnConstraints> constraints = getGrid(view).getColumnConstraints();

            // then: 5개 열 모두 너비가 고정되어 있다 (최소 = 선호 = 최대)
            assertEquals(COLUMN_COUNT, constraints.size());
            for (ColumnConstraints constraint : constraints) {
                assertTrue(constraint.getPrefWidth() > 0);
                assertEquals(constraint.getPrefWidth(), constraint.getMinWidth());
                assertEquals(constraint.getPrefWidth(), constraint.getMaxWidth());
            }
        });
    }

    @Test
    void keepTableWidthRegardlessOfEntries() throws Exception {
        runOnFxThread(() -> {
            // given: 기록이 하나도 없는 화면과, 긴 이름·큰 점수·★이 있는 화면
            ScoreEntry highlighted = createEntry("아주아주아주긴이름의플레이어", 12345678L);
            JavaFxScoreboardView emptyView = createView(List.of(), NORMAL_NORMAL, null);
            JavaFxScoreboardView fullView = createView(List.of(highlighted), NORMAL_NORMAL, highlighted.getId());

            // when
            double emptyWidth = getGrid(emptyView).prefWidth(-1);
            double fullWidth = getGrid(fullView).prefWidth(-1);

            // then: 기록 내용에 따라 순위 표 너비가 달라지지 않는다
            assertEquals(emptyWidth, fullWidth, 0.01);
        });
    }

    @Test
    void keepTableWidthWhenMarkerAppearsAfterMovingCategory() throws Exception {
        runOnFxThread(() -> {
            // given: 강조할 기록이 다른 조합(아이템 HARD)에 있다
            ScoreEntry highlighted = createEntry("me", 200L, ITEM_HARD);
            JavaFxScoreboardView view = createView(List.of(highlighted), NORMAL_EASY, highlighted.getId());
            double widthWithoutMarker = getGrid(view).prefWidth(-1);

            // when: ★이 있는 조합(아이템 HARD)으로 넘긴다
            pressKey(view, KeyCode.LEFT);
            pressKey(view, KeyCode.DOWN);
            pressKey(view, KeyCode.LEFT);

            // then: ★이 생겨도 너비가 변하지 않는다
            assertEquals(1, countMarkers(getGrid(view)));
            assertEquals(widthWithoutMarker, getGrid(view).prefWidth(-1), 0.01);
        });
    }

    @Test
    void keepSpaceBetweenMenuMarkersAndButton() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxScoreboardView view = createView(List.of(), NORMAL_NORMAL, null);

            // when & then: 마커와 버튼 사이에 간격이 있다
            for (int i = 0; i < MENU_COUNT; i++) {
                assertTrue(getMenuRow(view, i).getSpacing() > 0);
            }
        });
    }

    // ---------- 도우미 ----------

    private JavaFxScoreboardView createView(
            List<ScoreEntry> entries, ScoreCategory initialCategory, UUID highlightedEntryId) {
        return new JavaFxScoreboardView(
                new ScoreboardGroup(entries), initialCategory, highlightedEntryId,
                () -> backCalls++, () -> exitCalls++);
    }

    private ScoreEntry createEntry(String name, long score) {
        return createEntry(name, score, NORMAL_NORMAL);
    }

    private ScoreEntry createEntry(String name, long score, ScoreCategory category) {
        return new ScoreEntry(UUID.randomUUID(), name, score, category.mode(), category.difficulty());
    }

    private Node getNode(JavaFxScoreboardView view, String selector) {
        Node node = view.getScene().getRoot().lookup(selector);

        assertNotNull(node, "노드를 찾을 수 없음: " + selector);
        return node;
    }

    private HBox getRow(JavaFxScoreboardView view, String selector) {
        return (HBox) getNode(view, selector);
    }

    private GridPane getGrid(JavaFxScoreboardView view) {
        return (GridPane) getNode(view, SCORE_GRID_ID);
    }

    private VBox getMenuBox(JavaFxScoreboardView view) {
        return (VBox) getNode(view, MENU_BOX_ID);
    }

    private HBox getMenuRow(JavaFxScoreboardView view, int index) {
        return (HBox) getMenuBox(view).getChildren().get(index);
    }

    private Label getLeftMarker(HBox menuRow) {
        return (Label) menuRow.getChildren().get(0);
    }

    private Button getMenuButton(JavaFxScoreboardView view, int index) {
        return (Button) getMenuRow(view, index).getChildren().get(1);
    }

    private Label getRightMarker(HBox menuRow) {
        return (Label) menuRow.getChildren().get(2);
    }

    /**
     * 포커스가 있는 줄의 번호를 반환한다. (0 모드, 1 난이도, 2 메뉴로 돌아가기, 3 게임 종료)
     * 줄의 첫 자식과 마지막 자식(화살표 또는 마커)이 보이는 줄이 포커스된 줄이고, 정확히 하나여야 한다.
     */
    private int findFocusedRowIndex(JavaFxScoreboardView view) {
        int focusedIndex = -1;

        for (int i = 0; i < FOCUS_COUNT; i++) {
            HBox row = getFocusableRow(view, i);
            Node left = row.getChildren().get(0);
            Node right = row.getChildren().get(row.getChildren().size() - 1);

            assertEquals(left.isVisible(), right.isVisible(),
                    "왼쪽과 오른쪽 표시는 함께 보이거나 함께 숨겨져야 함");
            if (left.isVisible()) {
                assertEquals(-1, focusedIndex, "포커스된 줄은 하나여야 함");
                focusedIndex = i;
            }
        }

        assertTrue(focusedIndex >= 0, "포커스된 줄이 없음");
        return focusedIndex;
    }

    private HBox getFocusableRow(JavaFxScoreboardView view, int rowIndex) {
        if (rowIndex == FOCUS_MODE) {
            return getRow(view, MODE_ROW_ID);
        }
        if (rowIndex == FOCUS_DIFFICULTY) {
            return getRow(view, DIFFICULTY_ROW_ID);
        }

        return getMenuRow(view, rowIndex - FOCUS_BACK);
    }

    /**
     * 모드 줄 또는 난이도 줄의 ◀ ▶ 이동 안내 화살표가 보이는지 반환한다. 둘은 함께 보이거나 함께 숨겨져야 한다.
     */
    private boolean isArrowVisible(HBox tabRow) {
        Label left = findLabel(tabRow, LEFT_ARROW_TEXT);
        Label right = findLabel(tabRow, RIGHT_ARROW_TEXT);

        assertNotNull(left, "왼쪽 화살표를 찾을 수 없음");
        assertNotNull(right, "오른쪽 화살표를 찾을 수 없음");
        assertEquals(left.isVisible(), right.isVisible(), "화살표는 함께 보이거나 숨겨져야 함");
        return left.isVisible();
    }

    private void pressKeyTimes(JavaFxScoreboardView view, KeyCode key, int count) {
        for (int i = 0; i < count; i++) {
            pressKey(view, key);
        }
    }

    /**
     * 모드 줄과 난이도 줄에서 주어진 조합의 탭에만 밑줄이 있는지 확인한다.
     */
    private void assertSelectedTabs(JavaFxScoreboardView view, ScoreCategory expected) {
        HBox modeRow = getRow(view, MODE_ROW_ID);
        HBox difficultyRow = getRow(view, DIFFICULTY_ROW_ID);

        assertEquals(expected.mode() == GameMode.NORMAL, isUnderlined(findLabel(modeRow, NORMAL_MODE_TEXT)),
                "일반 탭 밑줄: " + expected);
        assertEquals(expected.mode() == GameMode.ITEM, isUnderlined(findLabel(modeRow, ITEM_MODE_TEXT)),
                "아이템 탭 밑줄: " + expected);
        assertEquals(expected.difficulty() == Difficulty.EASY, isUnderlined(findLabel(difficultyRow, EASY_TEXT)),
                "EASY 탭 밑줄: " + expected);
        assertEquals(expected.difficulty() == Difficulty.NORMAL, isUnderlined(findLabel(difficultyRow, NORMAL_TEXT)),
                "NORMAL 탭 밑줄: " + expected);
        assertEquals(expected.difficulty() == Difficulty.HARD, isUnderlined(findLabel(difficultyRow, HARD_TEXT)),
                "HARD 탭 밑줄: " + expected);
    }

    /**
     * 줄 안에서 해당 글자의 Label을 찾는다. 없으면 null 이다.
     */
    private Label findLabel(HBox row, String text) {
        for (Node child : row.getChildren()) {
            if (child instanceof Label label && text.equals(label.getText())) {
                return label;
            }
        }

        return null;
    }

    /**
     * Label에 아래쪽 테두리(밑줄)가 있는지 확인한다.
     */
    private boolean isUnderlined(Label label) {
        assertNotNull(label, "탭 Label을 찾을 수 없음");

        Border border = label.getBorder();
        if (border == null || border.getStrokes().isEmpty()) {
            return false;
        }

        return border.getStrokes().get(0).getWidths().getBottom() > 0;
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
        Label cell = findCellLabel(grid, column, row);

        return cell == null ? null : cell.getText();
    }

    /**
     * 순위 표의 (열, 행) 칸에 있는 Label을 반환한다. 해당 칸이 비어 있으면 null 이다.
     */
    private Label findCellLabel(GridPane grid, int column, int row) {
        for (Node child : grid.getChildren()) {
            if (GridPane.getColumnIndex(child) == column && GridPane.getRowIndex(child) == row) {
                return (Label) child;
            }
        }

        return null;
    }

    private Label findTitleLabel(JavaFxScoreboardView view) {
        return view.getScene()
                .getRoot()
                .getChildrenUnmodifiable()
                .stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .filter(label -> TITLE_TEXT.equals(label.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("제목을 찾을 수 없음"));
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

    /**
     * 키를 누른 뒤 그 이벤트가 화면 루트의 핸들러까지 도달했는지 반환한다.
     * 화면이 Scene 필터에서 소비한 키는 루트까지 도달하지 못한다.
     */
    private boolean pressKeyAndCheckReachedTarget(JavaFxScoreboardView view, KeyCode key) {
        boolean[] reached = {false};

        view.getScene().getRoot().addEventHandler(KeyEvent.KEY_PRESSED, event -> reached[0] = true);
        pressKey(view, key);

        return reached[0];
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
