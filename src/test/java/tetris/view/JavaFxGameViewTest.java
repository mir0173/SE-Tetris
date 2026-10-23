package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import javafx.geometry.Bounds;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tetris.model.Board;
import tetris.model.GameSnapshot;
import tetris.model.GameStatus;
import tetris.model.PieceType;
import tetris.model.Difficulty;
import tetris.model.GameCommand;

import tetris.settings.GameSettings;
import tetris.settings.WindowSize;

class JavaFxGameViewTest {

    // AI-assisted code start
    private static final int BOARD_ROWS = 20;
    private static final int BOARD_COLUMNS = 10;
    private static final int BORDER_MARGIN_PIXELS = 2;
    private static final double WHITE_THRESHOLD = 0.99;

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

    @Test
    void dispatchDefaultKeyBindings() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                JavaFxGameView view =
                        new JavaFxGameView(stage, GameSettings.defaults());

                List<GameCommand> commands = new ArrayList<>();
                view.setCommandHandler(commands::add);

                for (Map.Entry<GameCommand, String> binding
                        : GameSettings.getDefaultKeyBindings().entrySet()) {
                    commands.clear();

                    pressKey(stage, KeyCode.valueOf(binding.getValue()));

                    assertEquals(List.of(binding.getKey()), commands);
                }
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void useCustomKeyAndIgnorePreviousKey() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                Map<GameCommand, String> bindings =
                        new HashMap<>(GameSettings.getDefaultKeyBindings());
                bindings.put(GameCommand.MOVE_LEFT, "A");

                GameSettings settings = new GameSettings(
                        GameSettings.getDefaultWindowSize(),
                        bindings,
                        false, 
                        Difficulty.NORMAL
                );

                JavaFxGameView view =
                        new JavaFxGameView(stage, settings);

                List<GameCommand> commands = new ArrayList<>();
                view.setCommandHandler(commands::add);

                pressKey(stage, KeyCode.LEFT);
                assertTrue(commands.isEmpty());

                pressKey(stage, KeyCode.A);
                assertEquals(List.of(GameCommand.MOVE_LEFT), commands);
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void useCustomQuitKey() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                Map<GameCommand, String> bindings =
                        new HashMap<>(GameSettings.getDefaultKeyBindings());
                bindings.put(GameCommand.QUIT_GAME, "Q");

                GameSettings settings = new GameSettings(
                        GameSettings.getDefaultWindowSize(),
                        bindings,
                        false, 
                        Difficulty.NORMAL
                );

                JavaFxGameView view =
                        new JavaFxGameView(stage, settings);

                List<GameCommand> commands = new ArrayList<>();
                view.setCommandHandler(commands::add);

                pressKey(stage, KeyCode.ESCAPE);
                assertTrue(commands.isEmpty());

                pressKey(stage, KeyCode.Q);
                assertEquals(List.of(GameCommand.QUIT_GAME), commands);
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void updateScoreAndDropInterval() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                JavaFxGameView view =
                        new JavaFxGameView(stage, GameSettings.defaults());

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(),
                        null,
                        100L,
                        GameStatus.PLAYING,
                        1_000L));

                Label scoreLabel = (Label) stage.getScene()
                        .lookup("#score-label");
                Label intervalLabel = (Label) stage.getScene()
                        .lookup("#interval-label");

                assertEquals("점수: 100", scoreLabel.getText());
                assertEquals(
                        "낙하 간격: 1000ms",
                        intervalLabel.getText());

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(),
                        null,
                        250L,
                        GameStatus.PLAYING,
                        900L));

                assertEquals("점수: 250", scoreLabel.getText());
                assertEquals(
                        "낙하 간격: 900ms",
                        intervalLabel.getText());
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void updateAndClearNextPiecePreview() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                JavaFxGameView view =
                        new JavaFxGameView(stage, GameSettings.defaults());

                Canvas preview = (Canvas) stage.getScene()
                        .lookup("#next-preview");

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(PieceType.O),
                        null,
                        0L,
                        GameStatus.PLAYING,
                        1_000L));

                assertTrue(containsColor(
                        preview,
                        Color.web("#FACC15")));

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(PieceType.I),
                        null,
                        0L,
                        GameStatus.PLAYING,
                        1_000L));

                assertTrue(containsColor(
                        preview,
                        Color.web("#22D3EE")));
                assertFalse(containsColor(
                        preview,
                        Color.web("#FACC15")));

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(),
                        null,
                        0L,
                        GameStatus.PLAYING,
                        1_000L));

                assertFalse(containsColor(
                        preview,
                        Color.web("#22D3EE")));
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void showAndClearHeldPiece() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                JavaFxGameView view =
                        new JavaFxGameView(stage, GameSettings.defaults());

                Canvas preview = (Canvas) stage.getScene()
                        .lookup("#hold-preview");

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(),
                        PieceType.T,
                        0L,
                        GameStatus.PLAYING,
                        1_000L));

                assertTrue(containsColor(
                        preview,
                        Color.web("#A855F7")));

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(),
                        null,
                        0L,
                        GameStatus.PLAYING,
                        1_000L));

                assertFalse(containsColor(
                        preview,
                        Color.web("#A855F7")));
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void showPauseNoticeOnlyWhilePaused() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                JavaFxGameView view =
                        new JavaFxGameView(stage, GameSettings.defaults());

                Label pauseLabel = (Label) stage.getScene()
                        .lookup("#pause-label");

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(),
                        null,
                        0L,
                        GameStatus.PAUSED,
                        1_000L));

                assertTrue(pauseLabel.isVisible());

                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(),
                        null,
                        0L,
                        GameStatus.PLAYING,
                        1_000L));

                assertFalse(pauseLabel.isVisible());
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void useColorBlindPalette() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                GameSettings settings = new GameSettings(
                        GameSettings.getDefaultWindowSize(),
                        GameSettings.getDefaultKeyBindings(),
                        true, 
                        Difficulty.NORMAL);

                JavaFxGameView view =
                        new JavaFxGameView(stage, settings);

                int[][] cells = new int[20][10];
                cells[0][0] = PieceType.I.getCellId();

                view.render(new GameSnapshot(
                        cells,
                        List.of(PieceType.I),
                        PieceType.I,
                        0L,
                        GameStatus.PLAYING,
                        1_000L));

                Canvas board = (Canvas) stage.getScene()
                        .lookup("#game-board");
                Canvas next = (Canvas) stage.getScene()
                        .lookup("#next-preview");
                Canvas hold = (Canvas) stage.getScene()
                        .lookup("#hold-preview");

                Color accessibleColor = Color.web("#56B4E9");

                assertTrue(containsColor(board, accessibleColor));
                assertTrue(containsColor(next, accessibleColor));
                assertTrue(containsColor(hold, accessibleColor));
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void ignoreRepeatedPauseUntilKeyReleased() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                Map<GameCommand, String> bindings =
                        new HashMap<>(GameSettings.getDefaultKeyBindings());
                bindings.put(GameCommand.PAUSE, "A");

                GameSettings settings = new GameSettings(
                        GameSettings.getDefaultWindowSize(), bindings, false, Difficulty.NORMAL);
                JavaFxGameView view = new JavaFxGameView(stage, settings);

                List<GameCommand> commands = new ArrayList<>();
                view.setCommandHandler(commands::add);

                pressKey(stage, KeyCode.A);
                pressKey(stage, KeyCode.A);
                assertEquals(List.of(GameCommand.PAUSE), commands);

                releaseKey(stage, KeyCode.A);
                pressKey(stage, KeyCode.A);

                assertEquals(
                        List.of(GameCommand.PAUSE, GameCommand.PAUSE), commands);
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void keepRepeatedMovementKeys() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                JavaFxGameView view =
                        new JavaFxGameView(stage, GameSettings.defaults());

                List<GameCommand> commands = new ArrayList<>();
                view.setCommandHandler(commands::add);

                pressKey(stage, KeyCode.LEFT);
                pressKey(stage, KeyCode.LEFT);

                assertEquals(
                        List.of(GameCommand.MOVE_LEFT, GameCommand.MOVE_LEFT),
                        commands);
            } finally {
                stage.close();
            }
        });
    }

    @ParameterizedTest
    @CsvSource({
        "SMALL, 400, 480, 0.8",
        "MEDIUM, 500, 600, 1.0",
        "LARGE, 600, 720, 1.2"
    })
    void applyWindowSizeToOpenStage(
            WindowSize windowSize,
            double width,
            double height,
            double scale) throws Exception {

        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                // 메뉴처럼 이미 열린 창에서 게임 화면으로 전환한다.
                stage.setScene(new Scene(new StackPane(), 500, 600));
                stage.show();

                GameSettings settings = new GameSettings(
                        windowSize,
                        GameSettings.getDefaultKeyBindings(),
                        false, 
                        Difficulty.NORMAL);

                JavaFxGameView view = new JavaFxGameView(stage, settings);
                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(PieceType.I),
                        PieceType.T,
                        100L,
                        GameStatus.PLAYING,
                        1_000L));
                view.show();

                Scene scene = stage.getScene();
                scene.getRoot().applyCss();
                scene.getRoot().layout();

                assertEquals(width, scene.getWidth(), 1.0);
                assertEquals(height, scene.getHeight(), 1.0);

                Canvas board = (Canvas) scene.lookup("#game-board");
                Canvas next = (Canvas) scene.lookup("#next-preview");
                Canvas hold = (Canvas) scene.lookup("#hold-preview");
                Label score = (Label) scene.lookup("#score-label");
                HBox root = (HBox) scene.getRoot();

                assertEquals(
                        20 * scale,
                        board.getGraphicsContext2D().getFont().getSize(),
                        0.01);
                assertEquals(100 * scale, next.getWidth(), 0.01);
                assertEquals(100 * scale, next.getHeight(), 0.01);
                assertEquals(100 * scale, hold.getWidth(), 0.01);
                assertEquals(100 * scale, hold.getHeight(), 0.01);
                assertEquals(20 * scale, score.getFont().getSize(), 0.01);
                assertEquals(20 * scale, root.getPadding().getTop(), 0.01);
            } finally {
                stage.close();
            }
        });
    }

    @ParameterizedTest
    @EnumSource(WindowSize.class)
    void keepGameContentWithinScene(WindowSize windowSize) throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();

            try {
                GameSettings settings = new GameSettings(
                        windowSize,
                        GameSettings.getDefaultKeyBindings(),
                        false, 
                        Difficulty.NORMAL);

                JavaFxGameView view = new JavaFxGameView(stage, settings);
                view.render(new GameSnapshot(
                        new int[20][10],
                        List.of(PieceType.I),
                        PieceType.T,
                        100L,
                        GameStatus.PAUSED,
                        1_000L));
                view.show();

                Scene scene = stage.getScene();
                scene.getRoot().applyCss();
                scene.getRoot().layout();

                assertInsideScene(scene.lookup("#game-board"), scene);
                assertInsideScene(scene.lookup("#pause-label"), scene);

                HBox root = (HBox) scene.getRoot();
                VBox sidebar = (VBox) root.getChildren().get(1);

                for (Node node : sidebar.getChildren()) {
                    assertInsideScene(node, scene);
                }
            } finally {
                stage.close();
            }
        });
    }

    // ---------- 줄 삭제 연출 ----------

    @Test
    void drawClearingRowsInWhite() throws Exception {
        runOnFxThread(() -> {
            // given
            Stage stage = new Stage();

            try {
                JavaFxGameView view = new JavaFxGameView(stage, GameSettings.defaults());
                Canvas board = (Canvas) stage.getScene().lookup("#game-board");

                // when
                view.render(snapshotWithFullRowsFrom(18, List.of(18, 19)));

                // then: 지워질 줄에는 흰색이 나타난다
                assertTrue(countWhitePixelsInRow(board, 18) > 0);
                assertTrue(countWhitePixelsInRow(board, 19) > 0);
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void keepOtherRowsWithoutWhite() throws Exception {
        runOnFxThread(() -> {
            // given
            Stage stage = new Stage();

            try {
                JavaFxGameView view = new JavaFxGameView(stage, GameSettings.defaults());
                Canvas board = (Canvas) stage.getScene().lookup("#game-board");

                // when
                view.render(snapshotWithFullRowsFrom(18, List.of(18, 19)));

                // then: 지워질 줄이 아닌 줄(일부만 채워진 17행, 빈 0행)에는 흰색이 없다
                assertEquals(0, countWhitePixelsInRow(board, 17));
                assertEquals(0, countWhitePixelsInRow(board, 0));
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void drawAllClearingRowsWhenSeveralRowsAreCleared() throws Exception {
        runOnFxThread(() -> {
            // given
            Stage stage = new Stage();

            try {
                JavaFxGameView view = new JavaFxGameView(stage, GameSettings.defaults());
                Canvas board = (Canvas) stage.getScene().lookup("#game-board");

                // when
                view.render(snapshotWithFullRowsFrom(16, List.of(16, 17, 18, 19)));

                // then
                for (int row = 16; row <= 19; row++) {
                    assertTrue(countWhitePixelsInRow(board, row) > 0, "row " + row);
                }
                assertEquals(0, countWhitePixelsInRow(board, 15));
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void drawRowsNormallyWhenNoRowIsClearing() throws Exception {
        runOnFxThread(() -> {
            // given
            Stage stage = new Stage();

            try {
                JavaFxGameView view = new JavaFxGameView(stage, GameSettings.defaults());
                Canvas board = (Canvas) stage.getScene().lookup("#game-board");

                // when
                view.render(snapshotWithFullRowsFrom(18, List.of()));

                // then: 꽉 찬 줄이어도 삭제 대기가 아니면 원래 색으로만 그려진다
                assertEquals(0, countWhitePixelsInRow(board, 18));
                assertEquals(0, countWhitePixelsInRow(board, 19));
                assertTrue(containsColor(board, Color.web("#22D3EE")));
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void drawRowsNormallyAfterClearingEnds() throws Exception {
        runOnFxThread(() -> {
            // given: 삭제 대기 중인 모습을 먼저 그린 뒤
            Stage stage = new Stage();

            try {
                JavaFxGameView view = new JavaFxGameView(stage, GameSettings.defaults());
                Canvas board = (Canvas) stage.getScene().lookup("#game-board");
                view.render(snapshotWithFullRowsFrom(18, List.of(18, 19)));

                // when: 지워질 줄 정보가 없는 스냅샷을 다시 그리면
                view.render(snapshotWithFullRowsFrom(18, List.of()));

                // then: 흰색이 사라지고 원래 색으로 그려진다
                assertEquals(0, countWhitePixelsInRow(board, 18));
                assertEquals(0, countWhitePixelsInRow(board, 19));
                assertTrue(containsColor(board, Color.web("#22D3EE")));
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void drawClearingRowsInWhiteInColorBlindMode() throws Exception {
        runOnFxThread(() -> {
            // given
            Stage stage = new Stage();

            try {
                GameSettings settings = new GameSettings(
                        GameSettings.getDefaultWindowSize(),
                        GameSettings.getDefaultKeyBindings(),
                        true,
                        Difficulty.NORMAL);
                JavaFxGameView view = new JavaFxGameView(stage, settings);
                Canvas board = (Canvas) stage.getScene().lookup("#game-board");

                // when
                view.render(snapshotWithFullRowsFrom(18, List.of(18, 19)));

                // then
                assertTrue(countWhitePixelsInRow(board, 18) > 0);
                assertTrue(countWhitePixelsInRow(board, 19) > 0);
                assertEquals(0, countWhitePixelsInRow(board, 17));
            } finally {
                stage.close();
            }
        });
    }

    @ParameterizedTest
    @EnumSource(WindowSize.class)
    void drawClearingRowsInWhiteOnEveryWindowSize(WindowSize windowSize) throws Exception {
        runOnFxThread(() -> {
            // given
            Stage stage = new Stage();

            try {
                GameSettings settings = new GameSettings(
                        windowSize,
                        GameSettings.getDefaultKeyBindings(),
                        false,
                        Difficulty.NORMAL);
                JavaFxGameView view = new JavaFxGameView(stage, settings);
                Canvas board = (Canvas) stage.getScene().lookup("#game-board");

                // when
                view.render(snapshotWithFullRowsFrom(19, List.of(19)));

                // then
                assertTrue(countWhitePixelsInRow(board, 19) > 0);
                assertEquals(0, countWhitePixelsInRow(board, 18));
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void drawEmptyCellsWhiteWhenClearingRowIsNotFull() throws Exception {
        runOnFxThread(() -> {
            // given: 19행은 왼쪽 5칸만 채워진 줄이다 (L 아이템으로 지워지는 줄처럼)
            Stage stage = new Stage();

            try {
                JavaFxGameView view = new JavaFxGameView(stage, GameSettings.defaults());
                Canvas board = (Canvas) stage.getScene().lookup("#game-board");

                // when
                view.render(snapshotWithHalfFilledRow(19, List.of(19)));

                // then: 빈 칸 자리까지 흰색이라 줄 전체가 하나의 띠로 보인다
                assertTrue(countWhitePixelsInCells(board, 19, 0, 5) > 0);
                assertTrue(countWhitePixelsInCells(board, 19, 5, BOARD_COLUMNS) > 0);
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void keepEmptyCellsDarkWhenRowIsNotClearing() throws Exception {
        runOnFxThread(() -> {
            // given
            Stage stage = new Stage();

            try {
                JavaFxGameView view = new JavaFxGameView(stage, GameSettings.defaults());
                Canvas board = (Canvas) stage.getScene().lookup("#game-board");

                // when
                view.render(snapshotWithHalfFilledRow(19, List.of()));

                // then
                assertEquals(0, countWhitePixelsInCells(board, 19, 5, BOARD_COLUMNS));
            } finally {
                stage.close();
            }
        });
    }

    @Test
    void drawEmptyRowWhiteWhenEmptyRowIsClearing() throws Exception {
        runOnFxThread(() -> {
            // given
            Stage stage = new Stage();

            try {
                JavaFxGameView view = new JavaFxGameView(stage, GameSettings.defaults());
                Canvas board = (Canvas) stage.getScene().lookup("#game-board");

                // when: 지워질 줄 번호가 빈 줄을 가리켜도 그리기가 깨지지 않는다
                view.render(snapshotWithHalfFilledRow(19, List.of(10)));

                // then
                assertTrue(countWhitePixelsInCells(board, 10, 0, BOARD_COLUMNS) > 0);
                assertEquals(0, countWhitePixelsInCells(board, 11, 0, BOARD_COLUMNS));
            } finally {
                stage.close();
            }
        });
    }

    private static void assertInsideScene(Node node, Scene scene) {
        Bounds bounds = node.localToScene(node.getBoundsInLocal());

        assertTrue(bounds.getMinX() >= -1.0, node + " left");
        assertTrue(bounds.getMinY() >= -1.0, node + " top");
        assertTrue(
                bounds.getMaxX() <= scene.getWidth() + 1.0,
                node + " right");
        assertTrue(
                bounds.getMaxY() <= scene.getHeight() + 1.0,
                node + " bottom");
    }

    private static void releaseKey(Stage stage, KeyCode keyCode) {
        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_RELEASED, "", "", keyCode,
                false, false, false, false);

        Event.fireEvent(stage.getScene().getRoot(), event);
    }

    private static void pressKey(Stage stage, KeyCode keyCode) {
        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "",
                "",
                keyCode,
                false,
                false,
                false,
                false
        );

        Event.fireEvent(stage.getScene().getRoot(), event);
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

    private static boolean containsColor(Canvas canvas, Color expected) {
        WritableImage image = canvas.snapshot(null, null);

        for (int row = 0; row < (int) image.getHeight(); row++) {
            for (int column = 0;
                    column < (int) image.getWidth();
                    column++) {

                Color actual = image.getPixelReader()
                        .getColor(column, row);

                if (Math.abs(actual.getRed() - expected.getRed()) < 0.01
                        && Math.abs(actual.getGreen()
                                - expected.getGreen()) < 0.01
                        && Math.abs(actual.getBlue()
                                - expected.getBlue()) < 0.01) {
                    return true;
                }
            }
        }

        return false;
    }
    
    // firstFullRow 아래는 I 블록으로 꽉 찬 줄, 바로 위 한 줄은 한 칸이 빈 20x10 보드의 스냅샷
    private static GameSnapshot snapshotWithFullRowsFrom(int firstFullRow, List<Integer> clearingRows) {
        int[][] cells = new int[BOARD_ROWS][BOARD_COLUMNS];

        for (int row = firstFullRow - 1; row < BOARD_ROWS; row++) {
            for (int column = 0; column < BOARD_COLUMNS; column++) {
                cells[row][column] = PieceType.I.getCellId();
            }
        }
        cells[firstFullRow - 1][BOARD_COLUMNS - 1] = Board.EMPTY_CELL;

        GameStatus status = clearingRows.isEmpty() ? GameStatus.PLAYING : GameStatus.CLEARING;
        return new GameSnapshot(cells, List.of(PieceType.I), null, 0L, status, 1_000L, null, null, clearingRows);
    }

    // firstColumn 이상 endColumn 미만 칸 영역(테두리 제외)에서 지정한 행의 순수한 흰색 픽셀 수를 센다
    private static int countWhitePixelsInCells(Canvas canvas, int row, int firstColumn, int endColumn) {
        WritableImage image = canvas.snapshot(null, null);
        double cellWidth = canvas.getWidth() / (BOARD_COLUMNS + 2);
        double cellHeight = canvas.getHeight() / (BOARD_ROWS + 2);
        int startX = (int) Math.ceil(cellWidth * (firstColumn + 1)) + BORDER_MARGIN_PIXELS;
        int endX = (int) Math.floor(cellWidth * (endColumn + 1)) - BORDER_MARGIN_PIXELS;
        int startY = (int) Math.ceil(cellHeight * (row + 1));
        int endY = (int) Math.floor(cellHeight * (row + 2));
        int count = 0;

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                Color color = image.getPixelReader().getColor(x, y);

                if (color.getRed() > WHITE_THRESHOLD
                        && color.getGreen() > WHITE_THRESHOLD
                        && color.getBlue() > WHITE_THRESHOLD) {
                    count++;
                }
            }
        }

        return count;
    }

    // 지정한 행의 순수한 흰색 픽셀 수를 센다
    private static int countWhitePixelsInRow(Canvas canvas, int row) {
        return countWhitePixelsInCells(canvas, row, 0, BOARD_COLUMNS);
    }

    // row 한 줄만 왼쪽 5칸이 I 블록으로 채워진 20x10 보드의 스냅샷
    private static GameSnapshot snapshotWithHalfFilledRow(int row, List<Integer> clearingRows) {
        int[][] cells = new int[BOARD_ROWS][BOARD_COLUMNS];

        for (int column = 0; column < BOARD_COLUMNS / 2; column++) {
            cells[row][column] = PieceType.I.getCellId();
        }

        GameStatus status = clearingRows.isEmpty() ? GameStatus.PLAYING : GameStatus.CLEARING;
        return new GameSnapshot(cells, List.of(PieceType.I), null, 0L, status, 1_000L, null, null, clearingRows);
    }

    // AI-assisted code end
}