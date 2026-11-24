package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import tetris.model.Board;
import tetris.model.Difficulty;
import tetris.model.GameSnapshot;
import tetris.model.GameStatus;
import tetris.settings.GameSettings;
import tetris.settings.WindowSize;

/**
 * 넘어온 줄 표시 칸 (3차 요구사항: 넘어간 줄은 상대 화면의 "넘어온 줄 표시 칸"에 먼저 보인다)
 *
 * 화면 약속
 * - 대전용 화면(createEmbedded)에만 보인다. 1인 게임에는 받을 줄이 없으므로 숨기고 자리도 차지하지 않는다.
 * - #garbage-preview: 가로 10칸 x 세로 10줄(대기 줄 최대 10줄) 칸. 크기는 다음 블록 칸과 같다(100 x 100 x 배율).
 * - 받은 줄은 아래쪽부터 붙여 그린다. 목록의 마지막 줄이 맨 아래 → 보드에 실제로 쌓일 모양과 같다.
 * - 채워진 칸은 회색(Color.GRAY) 사각형, 빈 칸(구멍)과 남는 자리는 검은색.
 * - 10줄보다 많이 받으면 아래쪽 10줄만 그린다.
 * - #garbage-count-label: "넘어온 줄: N" (N은 받은 줄 전체 수)
 */
class JavaFxGameViewGarbagePreviewTest {

    // AI-assisted code start
    private static final int BOARD_ROWS = 20;
    private static final int BOARD_COLUMNS = 10;
    private static final int PREVIEW_ROWS = 10;
    private static final double PREVIEW_SIZE = 100;
    private static final double COLOR_TOLERANCE = 0.02;
    private static final Color FILLED_COLOR = Color.GRAY;
    private static final Color EMPTY_COLOR = Color.BLACK;

    @BeforeAll
    static void initializeJavaFx() throws Exception {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException alreadyStarted) {
            // 다른 테스트에서 JavaFX를 이미 초기화한 경우다.
        }

        runOnFxThread(() -> Platform.setImplicitExit(false));
    }

    // ---------- 보이는 곳 ----------

    @Test
    void hideGarbagePreviewInSinglePlayerView() throws Exception {
        runOnFxThread(() -> {
            // given
            Stage stage = new Stage();

            try {
                // when
                new JavaFxGameView(stage, GameSettings.defaults());
                Node preview = stage.getScene().lookup("#garbage-preview");
                Node countLabel = stage.getScene().lookup("#garbage-count-label");

                // then: 없거나, 있어도 안 보이고 자리도 차지하지 않는다
                assertHiddenWithoutSpace(preview);
                assertHiddenWithoutSpace(countLabel);
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void showGarbagePreviewInEmbeddedView() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxGameView view = JavaFxGameView.createEmbedded(GameSettings.defaults());

            // when
            view.render(snapshotWithPendingRows(List.of()));

            // then
            Canvas preview = findPreview(view);
            Label countLabel = findCountLabel(view);
            assertTrue(preview.isVisible());
            assertTrue(countLabel.isVisible());
            assertEquals("넘어온 줄: 0", countLabel.getText());
        });
    }

    @ParameterizedTest
    @EnumSource(WindowSize.class)
    void sizeGarbagePreviewLikeNextPreview(WindowSize windowSize) throws Exception {
        runOnFxThread(() -> {
            // given
            GameSettings settings = new GameSettings(
                    windowSize, GameSettings.getDefaultKeyBindings(), false, Difficulty.NORMAL);

            // when
            JavaFxGameView view = JavaFxGameView.createEmbedded(settings);

            // then
            double scale = windowSize.getScale();
            Canvas preview = findPreview(view);
            assertEquals(PREVIEW_SIZE * scale, preview.getWidth(), 0.01);
            assertEquals(PREVIEW_SIZE * scale, preview.getHeight(), 0.01);
        });
    }

    // ---------- 줄 수 ----------

    @Test
    void showPendingRowCount() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxGameView view = JavaFxGameView.createEmbedded(GameSettings.defaults());

            // when
            view.render(snapshotWithPendingRows(List.of(
                    rowWithHoleAt(0), rowWithHoleAt(1), rowWithHoleAt(2))));

            // then
            assertEquals("넘어온 줄: 3", findCountLabel(view).getText());
        });
    }

    // ---------- 그리기 ----------

    @Test
    void drawPendingRowsFromBottomWithHoles() throws Exception {
        runOnFxThread(() -> {
            // given: 위 줄은 0번 칸, 아래 줄은 9번 칸이 비어 있다
            JavaFxGameView view = JavaFxGameView.createEmbedded(GameSettings.defaults());

            // when
            view.render(snapshotWithPendingRows(List.of(rowWithHoleAt(0), rowWithHoleAt(9))));

            // then: 목록의 마지막 줄이 맨 아래(9행), 그 위 줄이 8행
            Canvas preview = findPreview(view);
            assertColor(EMPTY_COLOR, colorAtCell(preview, 8, 0));
            assertColor(FILLED_COLOR, colorAtCell(preview, 8, 1));
            assertColor(FILLED_COLOR, colorAtCell(preview, 9, 0));
            assertColor(EMPTY_COLOR, colorAtCell(preview, 9, 9));
        });
    }

    @Test
    void keepRowsAbovePendingRowsEmpty() throws Exception {
        runOnFxThread(() -> {
            // given
            JavaFxGameView view = JavaFxGameView.createEmbedded(GameSettings.defaults());

            // when: 2줄만 받으면
            view.render(snapshotWithPendingRows(List.of(rowWithHoleAt(0), rowWithHoleAt(9))));

            // then: 0~7행은 모두 비어 있다
            Canvas preview = findPreview(view);
            for (int row = 0; row < PREVIEW_ROWS - 2; row++) {
                for (int column = 0; column < BOARD_COLUMNS; column++) {
                    assertColor(EMPTY_COLOR, colorAtCell(preview, row, column));
                }
            }
        });
    }

    @Test
    void fillWholePreviewWhenTenRowsArePending() throws Exception {
        runOnFxThread(() -> {
            // given: 10줄 모두 4번 칸만 비어 있다
            JavaFxGameView view = JavaFxGameView.createEmbedded(GameSettings.defaults());
            List<int[]> rows = new ArrayList<>();
            for (int index = 0; index < PREVIEW_ROWS; index++) {
                rows.add(rowWithHoleAt(4));
            }

            // when
            view.render(snapshotWithPendingRows(rows));

            // then: 맨 위 줄부터 맨 아래 줄까지 모두 그려진다
            Canvas preview = findPreview(view);
            assertColor(FILLED_COLOR, colorAtCell(preview, 0, 0));
            assertColor(EMPTY_COLOR, colorAtCell(preview, 0, 4));
            assertColor(FILLED_COLOR, colorAtCell(preview, PREVIEW_ROWS - 1, 0));
            assertEquals("넘어온 줄: 10", findCountLabel(view).getText());
        });
    }

    @Test
    void drawOnlyBottomTenRowsWhenMoreArePending() throws Exception {
        runOnFxThread(() -> {
            // given: 12줄 중 위의 2줄은 5번 칸, 나머지 10줄은 9번 칸이 비어 있다
            JavaFxGameView view = JavaFxGameView.createEmbedded(GameSettings.defaults());
            List<int[]> rows = new ArrayList<>();
            rows.add(rowWithHoleAt(5));
            rows.add(rowWithHoleAt(5));
            for (int index = 0; index < PREVIEW_ROWS; index++) {
                rows.add(rowWithHoleAt(9));
            }

            // when
            view.render(snapshotWithPendingRows(rows));

            // then: 위의 2줄은 그리지 않으므로 맨 위 줄의 5번 칸도 채워져 있다
            Canvas preview = findPreview(view);
            assertColor(FILLED_COLOR, colorAtCell(preview, 0, 5));
            assertColor(EMPTY_COLOR, colorAtCell(preview, 0, 9));
            assertEquals("넘어온 줄: 12", findCountLabel(view).getText());
        });
    }

    @Test
    void clearPreviewAfterRowsAreApplied() throws Exception {
        runOnFxThread(() -> {
            // given: 받은 줄을 먼저 그린 뒤
            JavaFxGameView view = JavaFxGameView.createEmbedded(GameSettings.defaults());
            view.render(snapshotWithPendingRows(List.of(rowWithHoleAt(0), rowWithHoleAt(9))));

            // when: 줄이 보드에 들어가 대기 줄이 없어지면
            view.render(snapshotWithPendingRows(List.of()));

            // then: 회색 칸이 남지 않는다
            Canvas preview = findPreview(view);
            for (int row = 0; row < PREVIEW_ROWS; row++) {
                for (int column = 0; column < BOARD_COLUMNS; column++) {
                    assertColor(EMPTY_COLOR, colorAtCell(preview, row, column));
                }
            }
            assertEquals("넘어온 줄: 0", findCountLabel(view).getText());
        });
    }

    @Test
    void keepGarbageColorInColorBlindMode() throws Exception {
        runOnFxThread(() -> {
            // given
            GameSettings settings = new GameSettings(
                    GameSettings.getDefaultWindowSize(), GameSettings.getDefaultKeyBindings(), true,
                    Difficulty.NORMAL);
            JavaFxGameView view = JavaFxGameView.createEmbedded(settings);

            // when
            view.render(snapshotWithPendingRows(List.of(rowWithHoleAt(0))));

            // then: 색맹 모드에서도 회색 칸과 검은 구멍으로 구분된다
            Canvas preview = findPreview(view);
            assertColor(EMPTY_COLOR, colorAtCell(preview, 9, 0));
            assertColor(FILLED_COLOR, colorAtCell(preview, 9, 1));
        });
    }

    // ---------- 도우미 ----------

    private static void assertHiddenWithoutSpace(Node node) {
        if (node == null) {
            return;
        }
        assertFalse(node.isVisible(), node + " visible");
        assertFalse(node.isManaged(), node + " managed");
    }

    private static Canvas findPreview(JavaFxGameView view) {
        Parent content = view.getContent();
        Node node = content.lookup("#garbage-preview");
        assertNotNull(node, "#garbage-preview");
        return (Canvas) node;
    }

    private static Label findCountLabel(JavaFxGameView view) {
        Parent content = view.getContent();
        Node node = content.lookup("#garbage-count-label");
        assertNotNull(node, "#garbage-count-label");
        return (Label) node;
    }

    // 미리보기 칸을 10x10으로 나눴을 때 지정한 칸 한가운데 픽셀의 색
    private static Color colorAtCell(Canvas canvas, int row, int column) {
        WritableImage image = canvas.snapshot(null, null);
        double cellWidth = canvas.getWidth() / BOARD_COLUMNS;
        double cellHeight = canvas.getHeight() / PREVIEW_ROWS;
        int x = (int) (cellWidth * column + cellWidth / 2);
        int y = (int) (cellHeight * row + cellHeight / 2);
        return image.getPixelReader().getColor(x, y);
    }

    private static void assertColor(Color expected, Color actual) {
        boolean same = Math.abs(actual.getRed() - expected.getRed()) < COLOR_TOLERANCE
                && Math.abs(actual.getGreen() - expected.getGreen()) < COLOR_TOLERANCE
                && Math.abs(actual.getBlue() - expected.getBlue()) < COLOR_TOLERANCE;
        assertTrue(same, "expected " + expected + " but was " + actual);
    }

    // hole 번 칸만 비어 있고 나머지는 공격 줄 칸으로 채워진 한 줄
    private static int[] rowWithHoleAt(int hole) {
        int[] row = new int[BOARD_COLUMNS];
        Arrays.fill(row, Board.GARBAGE_CELL);
        row[hole] = Board.EMPTY_CELL;
        return row;
    }

    private static GameSnapshot snapshotWithPendingRows(List<int[]> pendingRows) {
        return new GameSnapshot(new int[BOARD_ROWS][BOARD_COLUMNS], List.of(), null, 0L, GameStatus.PLAYING,
                1_000L, null, null, List.of(), Map.of(), pendingRows);
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
