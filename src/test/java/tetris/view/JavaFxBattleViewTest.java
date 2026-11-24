package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import tetris.battle.MatchResult;
import tetris.model.Difficulty;
import tetris.model.GameCommand;
import tetris.model.GameSnapshot;
import tetris.model.GameStatus;
import tetris.model.PieceType;
import tetris.settings.GameSettings;
import tetris.settings.WindowSize;

/**
 * 로컬 2인 대전 화면 (P2P 대전 화면 JavaFxP2pGameView와 같은 구성)
 *
 * 화면 약속
 * - 보드 두 개를 나란히: #battle-first-panel(1P, 왼쪽), #battle-second-panel(2P, 오른쪽). 각각 createEmbedded 화면
 * - #battle-result: 결과 글자, 결과 전에는 숨김. 형식 "1P 승리 · 게임오버 (1P 1200 : 2P 800)"
 * - #battle-time-left: "남은 시간 m:ss", 시간을 알리기 전에는 숨김 (시간제한 모드에서만 알림)
 * - #battle-rematch "다시 하기", #battle-back "메뉴로", #battle-exit "게임 종료": 결과 뒤에만 보임 (경기 중 중단은 ESC)
 * - 키는 창 전체에서 받아 1P / 2P / 공용으로 나눈다. P는 1P 게임에만 보낸다(MatchManager가 2P를 맞춤). ESC는 메뉴로
 * - 배치·창 크기·글자 크기·키 안내 같은 화면 모양은 계속 바뀔 수 있으므로 테스트하지 않는다 (직접 보며 조정)
 */
class JavaFxBattleViewTest {

    // AI-assisted code start
    private static final int BOARD_ROWS = 20;
    private static final int BOARD_COLUMNS = 10;

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

    // ---------- 화면 구성 ----------

    @Test
    void placeFirstPlayerOnLeftAndSecondPlayerOnRight() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());
            Scene scene = fixture.view.getScene();

            // when
            scene.getRoot().applyCss();
            scene.getRoot().layout();

            // then: 앉은 자리(1P 왼쪽, 2P 오른쪽)와 화면 위치가 같다
            Parent first = panel(fixture, "battle-first-panel");
            Parent second = panel(fixture, "battle-second-panel");
            double firstX = first.localToScene(first.getBoundsInLocal()).getMinX();
            double secondX = second.localToScene(second.getBoundsInLocal()).getMinX();
            assertTrue(firstX < secondX, "1P " + firstX + " / 2P " + secondX);
        });
    }

    @Test
    void showBoardAndGarbagePreviewInBothPanels() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            Parent first = panel(fixture, "battle-first-panel");
            Parent second = panel(fixture, "battle-second-panel");

            // then: 두 보드 모두 넘어온 줄 표시 칸이 보인다
            for (Parent panel : List.of(first, second)) {
                assertNotNull(panel.lookup("#game-board"));
                assertTrue(panel.lookup("#garbage-preview").isVisible());
            }
        });
    }

    @Test
    void labelPanelsAsFirstAndSecondPlayer() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            List<String> texts = labelTexts(fixture.view.getScene().getRoot());

            // then
            assertTrue(texts.contains("1P"), texts.toString());
            assertTrue(texts.contains("2P"), texts.toString());
        });
    }

    @Test
    void showSharedKeysInPauseNotice() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            String pauseText = ((Label) panel(fixture, "battle-first-panel").lookup("#pause-label")).getText();

            // then: 일시정지 안내에 공용 키(P, ESCAPE)가 나온다
            assertTrue(pauseText.contains("P: 재개"), pauseText);
            assertTrue(pauseText.contains("ESCAPE"), pauseText);
        });
    }

    @Test
    void renderEachPlayerOnOwnPanel() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            fixture.view.getFirstPlayerView().render(snapshot(100L, GameStatus.PLAYING, new int[BOARD_ROWS][BOARD_COLUMNS]));
            fixture.view.getSecondPlayerView().render(snapshot(250L, GameStatus.PLAYING, new int[BOARD_ROWS][BOARD_COLUMNS]));

            // then
            assertEquals("점수: 100", scoreText(fixture, "battle-first-panel"));
            assertEquals("점수: 250", scoreText(fixture, "battle-second-panel"));
        });
    }

    @ParameterizedTest
    @EnumSource(WindowSize.class)
    void applyWindowSizeToBothBoards(WindowSize windowSize) throws Exception {
        runOnFxThread(() -> {
            // given
            GameSettings settings = new GameSettings(
                    windowSize, GameSettings.getDefaultKeyBindings(), false, Difficulty.NORMAL);

            // when
            Fixture fixture = new Fixture(settings);
            fixture.view.getFirstPlayerView().render(snapshot(0L, GameStatus.PLAYING, new int[BOARD_ROWS][BOARD_COLUMNS]));
            fixture.view.getSecondPlayerView().render(snapshot(0L, GameStatus.PLAYING, new int[BOARD_ROWS][BOARD_COLUMNS]));

            // then: 화면 크기 설정(배율)이 두 보드에 모두 적용된다
            double scale = windowSize.getScale();
            for (String panelId : List.of("battle-first-panel", "battle-second-panel")) {
                Canvas board = (Canvas) panel(fixture, panelId).lookup("#game-board");
                assertEquals(20 * scale, board.getGraphicsContext2D().getFont().getSize(), 0.01);
            }
        });
    }

    @Test
    void applyColorBlindModeToBothBoards() throws Exception {
        runOnFxThread(() -> {
            // given
            GameSettings settings = new GameSettings(
                    GameSettings.getDefaultWindowSize(), GameSettings.getDefaultKeyBindings(), true, Difficulty.NORMAL);
            Fixture fixture = new Fixture(settings);
            int[][] cells = new int[BOARD_ROWS][BOARD_COLUMNS];
            cells[19][0] = PieceType.I.getCellId();

            // when
            fixture.view.getFirstPlayerView().render(snapshot(0L, GameStatus.PLAYING, cells));
            fixture.view.getSecondPlayerView().render(snapshot(0L, GameStatus.PLAYING, cells));

            // then: 색맹용 I 블록 색
            Color accessibleColor = Color.web("#56B4E9");
            for (String panelId : List.of("battle-first-panel", "battle-second-panel")) {
                Canvas board = (Canvas) panel(fixture, panelId).lookup("#game-board");
                assertTrue(containsColor(board, accessibleColor), panelId);
            }
        });
    }

    // ---------- 키 분배 ----------

    @Test
    void sendFirstPlayerKeysOnlyToFirstPlayer() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            pressKey(fixture, KeyCode.F);
            pressKey(fixture, KeyCode.S);

            // then
            assertEquals(List.of(GameCommand.MOVE_LEFT, GameCommand.HARD_DROP), fixture.firstCommands);
            assertTrue(fixture.secondCommands.isEmpty());
        });
    }

    @Test
    void sendSecondPlayerKeysOnlyToSecondPlayer() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            pressKey(fixture, KeyCode.LEFT);
            pressKey(fixture, KeyCode.PERIOD);

            // then
            assertEquals(List.of(GameCommand.MOVE_LEFT, GameCommand.HARD_DROP), fixture.secondCommands);
            assertTrue(fixture.firstCommands.isEmpty());
        });
    }

    @Test
    void handleBothPlayersPressingKeysInTurn() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when: 두 사람이 번갈아 누른다
            pressKey(fixture, KeyCode.F);
            pressKey(fixture, KeyCode.RIGHT);
            pressKey(fixture, KeyCode.F);
            pressKey(fixture, KeyCode.UP);

            // then: 각자 자기 키만 받는다
            assertEquals(List.of(GameCommand.MOVE_LEFT, GameCommand.MOVE_LEFT), fixture.firstCommands);
            assertEquals(List.of(GameCommand.MOVE_RIGHT, GameCommand.ROTATE_CLOCKWISE), fixture.secondCommands);
        });
    }

    @Test
    void keepRepeatedMovementKeys() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when: 키를 누르고 있으면 같은 키가 반복해서 들어온다
            pressKey(fixture, KeyCode.H);
            pressKey(fixture, KeyCode.H);
            pressKey(fixture, KeyCode.H);

            // then: 반복 입력에 매번 반응한다
            assertEquals(List.of(GameCommand.MOVE_RIGHT, GameCommand.MOVE_RIGHT, GameCommand.MOVE_RIGHT),
                    fixture.firstCommands);
        });
    }

    @Test
    void ignoreKeysNotUsedInBattle() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when: 1인 게임 키(SPACE, Z, C)
            pressKey(fixture, KeyCode.SPACE);
            pressKey(fixture, KeyCode.Z);
            pressKey(fixture, KeyCode.C);

            // then
            assertTrue(fixture.firstCommands.isEmpty());
            assertTrue(fixture.secondCommands.isEmpty());
            assertEquals(0, fixture.menuCount.get());
        });
    }

    @Test
    void sendSharedPauseOnlyToFirstPlayer() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            pressKey(fixture, KeyCode.P);

            // then: 2P는 MatchManager가 같이 멈춘다
            assertEquals(List.of(GameCommand.PAUSE), fixture.firstCommands);
            assertTrue(fixture.secondCommands.isEmpty());
        });
    }

    @Test
    void ignoreRepeatedPauseUntilKeyReleased() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when: P를 누르고 있다가 떼고 다시 누른다
            pressKey(fixture, KeyCode.P);
            pressKey(fixture, KeyCode.P);
            releaseKey(fixture, KeyCode.P);
            pressKey(fixture, KeyCode.P);

            // then: 누르고 있는 동안의 반복은 무시한다 (일시정지가 깜빡이지 않게)
            assertEquals(List.of(GameCommand.PAUSE, GameCommand.PAUSE), fixture.firstCommands);
        });
    }

    @Test
    void returnToMenuOnEscapeDuringMatch() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            pressKey(fixture, KeyCode.ESCAPE);

            // then: 게임 명령으로 보내지 않고 메뉴로 간다 (경기 중단)
            assertEquals(1, fixture.menuCount.get());
            assertTrue(fixture.firstCommands.isEmpty());
            assertTrue(fixture.secondCommands.isEmpty());
        });
    }

    // ---------- 남은 시간 ----------

    @Test
    void hideTimeLeftUntilItIsRendered() throws Exception {
        runOnFxThread(() -> {
            // given

            // when: 일반·아이템 모드처럼 남은 시간을 알리지 않으면
            Fixture fixture = new Fixture(GameSettings.defaults());

            // then
            assertFalse(label(fixture, "battle-time-left").isVisible());
        });
    }

    @Test
    void showRemainingSecondsLikeP2pView() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            fixture.view.renderRemainingSeconds(125);

            // then
            Label timeLeft = label(fixture, "battle-time-left");
            assertTrue(timeLeft.isVisible());
            assertEquals("남은 시간 2:05", timeLeft.getText());

            // when
            fixture.view.renderRemainingSeconds(9);

            // then
            assertEquals("남은 시간 0:09", timeLeft.getText());
        });
    }

    // ---------- 결과 ----------

    @Test
    void hideResultAndAfterMatchButtonsDuringMatch() throws Exception {
        runOnFxThread(() -> {
            // given

            // when
            Fixture fixture = new Fixture(GameSettings.defaults());

            // then
            assertFalse(label(fixture, "battle-result").isVisible());
            assertFalse(button(fixture, "battle-rematch").isVisible());
            assertFalse(button(fixture, "battle-exit").isVisible());
            assertFalse(button(fixture, "battle-back").isVisible()); // 경기 중 중단은 ESC로만 한다
        });
    }

    @Test
    void showFirstPlayerWinByGameOver() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            fixture.view.renderResult(new MatchResult(
                    MatchResult.Winner.PLAYER_ONE, MatchResult.EndReason.GAME_OVER, 1200, 800));

            // then
            Label result = label(fixture, "battle-result");
            assertTrue(result.isVisible());
            assertEquals("1P 승리 · 게임오버 (1P 1200 : 2P 800)", result.getText());
        });
    }

    @Test
    void showSecondPlayerWinByTimeUp() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            fixture.view.renderResult(new MatchResult(
                    MatchResult.Winner.PLAYER_TWO, MatchResult.EndReason.TIME_UP, 300, 500));

            // then
            assertEquals("2P 승리 · 시간 종료 (1P 300 : 2P 500)", label(fixture, "battle-result").getText());
        });
    }

    @Test
    void showDraw() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            fixture.view.renderResult(new MatchResult(
                    MatchResult.Winner.DRAW, MatchResult.EndReason.TIME_UP, 500, 500));

            // then
            assertEquals("무승부 · 시간 종료 (1P 500 : 2P 500)", label(fixture, "battle-result").getText());
        });
    }

    @Test
    void showAfterMatchButtonsWithResult() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            fixture.view.renderResult(firstPlayerWin());

            // then
            assertTrue(button(fixture, "battle-rematch").isVisible());
            assertEquals("다시 하기", button(fixture, "battle-rematch").getText());
            assertTrue(button(fixture, "battle-exit").isVisible());
            assertEquals("게임 종료", button(fixture, "battle-exit").getText());
            assertEquals("메뉴로", button(fixture, "battle-back").getText());
        });
    }

    @Test
    void runCallbackOfEachButton() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.view.renderResult(firstPlayerWin());

            // when
            button(fixture, "battle-rematch").fire();
            button(fixture, "battle-back").fire();
            button(fixture, "battle-exit").fire();

            // then
            assertEquals(1, fixture.rematchCount.get());
            assertEquals(1, fixture.menuCount.get());
            assertEquals(1, fixture.exitCount.get());
        });
    }

    @Test
    void ignoreGameKeysAfterResult() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.view.renderResult(firstPlayerWin());

            // when
            pressKey(fixture, KeyCode.F);
            pressKey(fixture, KeyCode.LEFT);
            pressKey(fixture, KeyCode.P);

            // then
            assertTrue(fixture.firstCommands.isEmpty());
            assertTrue(fixture.secondCommands.isEmpty());
        });
    }

    @Test
    void returnToMenuOnEscapeAfterResult() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.view.renderResult(firstPlayerWin());

            // when
            pressKey(fixture, KeyCode.ESCAPE);

            // then
            assertEquals(1, fixture.menuCount.get());
        });
    }

    // ---------- 생성 ----------

    @Test
    void rejectNullArguments() throws Exception {
        runOnFxThread(() -> {
            // given
            Runnable none = () -> {
            };

            // when & then
            assertThrows(NullPointerException.class, () -> new JavaFxBattleView(null, none, none, none));
            assertThrows(NullPointerException.class,
                    () -> new JavaFxBattleView(GameSettings.defaults(), null, none, none));
            assertThrows(NullPointerException.class,
                    () -> new JavaFxBattleView(GameSettings.defaults(), none, null, none));
            assertThrows(NullPointerException.class,
                    () -> new JavaFxBattleView(GameSettings.defaults(), none, none, null));
        });
    }

    // ---------- 도우미 ----------

    // 화면 하나와 콜백·명령 기록을 함께 만든다
    private static final class Fixture {
        private final List<GameCommand> firstCommands = new ArrayList<>();
        private final List<GameCommand> secondCommands = new ArrayList<>();
        private final AtomicInteger menuCount = new AtomicInteger();
        private final AtomicInteger rematchCount = new AtomicInteger();
        private final AtomicInteger exitCount = new AtomicInteger();
        private final JavaFxBattleView view;

        private Fixture(GameSettings settings) {
            view = new JavaFxBattleView(settings, menuCount::incrementAndGet, rematchCount::incrementAndGet,
                    exitCount::incrementAndGet);
            view.getFirstPlayerView().setCommandHandler(firstCommands::add);
            view.getSecondPlayerView().setCommandHandler(secondCommands::add);
            // ScrollPane은 CSS가 적용돼야 안쪽 내용을 자식으로 가지므로 lookup 전에 적용한다
            view.getScene().getRoot().applyCss();
        }
    }

    private static MatchResult firstPlayerWin() {
        return new MatchResult(MatchResult.Winner.PLAYER_ONE, MatchResult.EndReason.GAME_OVER, 100, 50);
    }

    private static Parent panel(Fixture fixture, String id) {
        Node node = fixture.view.getScene().lookup("#" + id);
        assertNotNull(node, "#" + id);
        return (Parent) node;
    }

    private static Label label(Fixture fixture, String id) {
        Node node = fixture.view.getScene().lookup("#" + id);
        assertNotNull(node, "#" + id);
        return (Label) node;
    }

    private static Button button(Fixture fixture, String id) {
        Node node = fixture.view.getScene().lookup("#" + id);
        assertNotNull(node, "#" + id);
        return (Button) node;
    }

    private static String scoreText(Fixture fixture, String panelId) {
        return ((Label) panel(fixture, panelId).lookup("#score-label")).getText();
    }

    private static List<String> labelTexts(Parent root) {
        List<String> texts = new ArrayList<>();
        for (Node node : root.lookupAll(".label")) {
            if (node instanceof Label label) {
                texts.add(label.getText());
            }
        }
        return texts;
    }

    private static void pressKey(Fixture fixture, KeyCode keyCode) {
        Event.fireEvent(fixture.view.getScene().getRoot(),
                new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode, false, false, false, false));
    }

    private static void releaseKey(Fixture fixture, KeyCode keyCode) {
        Event.fireEvent(fixture.view.getScene().getRoot(),
                new KeyEvent(KeyEvent.KEY_RELEASED, "", "", keyCode, false, false, false, false));
    }

    private static GameSnapshot snapshot(long score, GameStatus status, int[][] cells) {
        return new GameSnapshot(cells, List.of(PieceType.I), null, score, status, 1_000L, null, null, List.of(),
                Map.of(), List.of());
    }

    private static boolean containsColor(Canvas canvas, Color expected) {
        WritableImage image = canvas.snapshot(null, null);

        for (int y = 0; y < (int) image.getHeight(); y++) {
            for (int x = 0; x < (int) image.getWidth(); x++) {
                Color actual = image.getPixelReader().getColor(x, y);
                if (Math.abs(actual.getRed() - expected.getRed()) < 0.01
                        && Math.abs(actual.getGreen() - expected.getGreen()) < 0.01
                        && Math.abs(actual.getBlue() - expected.getBlue()) < 0.01) {
                    return true;
                }
            }
        }

        return false;
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
