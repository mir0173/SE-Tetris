package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import tetris.model.Difficulty;
import tetris.model.GameCommand;
import tetris.settings.BattleKeyBindings;
import tetris.settings.GameSettings;
import tetris.settings.WindowSize;

/**
 * 대전 키 설정 화면 (설정 화면의 "대전 키 설정" 줄에서 들어온다)
 *
 * 동작 약속 (기존 설정 화면과 같은 조작)
 * - 줄 18개를 위에서 아래 순서로 선택한다: 1P 7개 → 2P 7개 → 공용 2개 → 대전 키 기본값 → 뒤로
 *   (명령 순서: 왼쪽 이동, 오른쪽 이동, 시계 방향 회전, 소프트 드롭, 하드 드롭, 홀드, 반시계 방향 회전 / 공용: 일시정지, 경기 중단)
 * - 줄 id: battle-key-{first|second|shared}-{GameCommand 이름}, battle-key-reset, battle-key-back. 메시지 #battle-key-message
 * - 줄 글자: 선택된 줄은 "▶ ", 아닌 줄은 "   "로 시작. 키 줄은 "1P 왼쪽 이동: F" 형식 (대상 + 명령 + ": " + 키)
 * - ↑/↓ 이동(양 끝 순환), Enter: 키 줄이면 입력 대기("키를 누르세요..."), 기본값 줄이면 재확인, 뒤로 줄이면 뒤로
 * - 입력 대기 중: 누른 키를 배정. ESC는 취소. 대전 키 16개 중 다른 명령이 쓰는 키면 거부하고 계속 대기(빨간 메시지).
 *   1인 게임 키와는 비교하지 않는다. Enter로 입력 대기를 시작했다면 Enter를 뗄 때까지 Enter 반복은 무시한다
 * - 재확인 중: Y면 대전 키만 기본값으로(1인 키 등 다른 설정은 그대로), 다른 키는 취소
 * - 평소 ESC: 뒤로
 * - 설정이 바뀔 때마다 새 GameSettings를 onChange로 알린다 (대전 키 외의 값은 그대로)
 * - 화면 모양(배치·크기·글꼴)은 테스트하지 않는다
 */
class JavaFxBattleKeySettingsViewTest {

    // AI-assisted code start
    private static final String SELECTED_MARKER = "▶ ";
    private static final String UNSELECTED_MARKER = "   ";
    private static final String WAITING_KEY_TEXT = "키를 누르세요...";
    private static final String RESET_ROW_TEXT = "대전 키 기본값으로 되돌리기";
    private static final String RESET_CONFIRMATION_TEXT = "대전 키 기본값으로 되돌리기: 정말 되돌리려면 Y, 취소는 ESC";
    private static final String RESET_MESSAGE = "대전 키를 기본값으로 되돌렸습니다";
    private static final String BACK_ROW_TEXT = "뒤로";

    private static final List<GameCommand> PLAYER_ORDER = List.of(
            GameCommand.MOVE_LEFT, GameCommand.MOVE_RIGHT, GameCommand.ROTATE_CLOCKWISE, GameCommand.SOFT_DROP,
            GameCommand.HARD_DROP, GameCommand.HOLD, GameCommand.ROTATE_COUNTERCLOCKWISE);
    private static final List<String> COMMAND_NAMES = List.of(
            "왼쪽 이동", "오른쪽 이동", "시계 방향 회전", "소프트 드롭", "하드 드롭", "홀드", "반시계 방향 회전");
    private static final List<String> ROW_IDS = createRowIds();
    private static final int SECOND_PLAYER_FIRST_ROW = 7;
    private static final int SHARED_FIRST_ROW = 14;
    private static final int RESET_ROW = 16;
    private static final int BACK_ROW = 17;

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

    // ---------- 표시 ----------

    @Test
    void showDefaultKeysInEveryRow() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            List<String> texts = new ArrayList<>();
            for (String id : ROW_IDS) {
                texts.add(fixture.rowText(id));
            }

            // then
            List<String> expected = new ArrayList<>();
            List<String> firstKeys = List.of("F", "H", "T", "G", "S", "D", "A");
            List<String> secondKeys = List.of("LEFT", "RIGHT", "UP", "DOWN", "PERIOD", "SLASH", "COMMA");
            for (int index = 0; index < PLAYER_ORDER.size(); index++) {
                expected.add("1P " + COMMAND_NAMES.get(index) + ": " + firstKeys.get(index));
            }
            for (int index = 0; index < PLAYER_ORDER.size(); index++) {
                expected.add("2P " + COMMAND_NAMES.get(index) + ": " + secondKeys.get(index));
            }
            expected.add("공용 일시정지: P");
            expected.add("공용 경기 중단: ESCAPE");
            expected.add(RESET_ROW_TEXT);
            expected.add(BACK_ROW_TEXT);
            assertEquals(expected, texts);
        });
    }

    @Test
    void showCustomBattleKeys() throws Exception {
        runOnFxThread(() -> {
            // given: 1P 왼쪽 이동이 Q로 바뀐 설정

            // when
            Fixture fixture = new Fixture(settingsWithFirstPlayerKey(GameCommand.MOVE_LEFT, "Q"));

            // then
            assertEquals("1P 왼쪽 이동: Q", fixture.rowText("battle-key-first-MOVE_LEFT"));
        });
    }

    // ---------- 선택 이동 ----------

    @Test
    void selectFirstRowAtStart() throws Exception {
        runOnFxThread(() -> {
            // given

            // when
            Fixture fixture = new Fixture(GameSettings.defaults());

            // then
            assertTrue(fixture.isSelected(ROW_IDS.get(0)));
            assertFalse(fixture.isSelected(ROW_IDS.get(1)));
        });
    }

    @Test
    void moveSelectionThroughEveryRowInOrder() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            for (int row = 1; row < ROW_IDS.size(); row++) {
                // when
                fixture.press(KeyCode.DOWN);

                // then
                assertTrue(fixture.isSelected(ROW_IDS.get(row)), ROW_IDS.get(row));
                assertFalse(fixture.isSelected(ROW_IDS.get(row - 1)), ROW_IDS.get(row - 1));
            }
        });
    }

    @Test
    void wrapSelectionAroundTopAndBottom() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when: 맨 위에서 위로
            fixture.press(KeyCode.UP);

            // then
            assertTrue(fixture.isSelected("battle-key-back"));

            // when: 맨 아래에서 아래로
            fixture.press(KeyCode.DOWN);

            // then
            assertTrue(fixture.isSelected(ROW_IDS.get(0)));
        });
    }

    // ---------- 키 바꾸기 ----------

    @Test
    void waitForKeyAfterEnter() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            fixture.press(KeyCode.ENTER);

            // then
            assertEquals("1P 왼쪽 이동: " + WAITING_KEY_TEXT, fixture.rowText("battle-key-first-MOVE_LEFT"));
            assertTrue(fixture.changes.isEmpty());
        });
    }

    @Test
    void assignPressedKeyToFirstPlayer() throws Exception {
        runOnFxThread(() -> {
            // given
            GameSettings settings = new GameSettings(WindowSize.LARGE, singleKeysWith(GameCommand.HOLD, "V"), true,
                    Difficulty.HARD);
            Fixture fixture = new Fixture(settings);

            // when
            fixture.press(KeyCode.ENTER);
            fixture.press(KeyCode.Q);

            // then: 대전 키만 바뀌고 나머지 설정은 그대로다
            GameSettings changed = fixture.lastChange();
            assertEquals("Q", changed.getBattleKeyBindings().getFirstPlayerKeys().get(GameCommand.MOVE_LEFT));
            assertEquals(WindowSize.LARGE, changed.getWindowSize());
            assertTrue(changed.isColorBlindMode());
            assertEquals(Difficulty.HARD, changed.getDifficulty());
            assertEquals("V", changed.getKeyBindings().get(GameCommand.HOLD));
            assertEquals("1P 왼쪽 이동: Q", fixture.rowText("battle-key-first-MOVE_LEFT"));
        });
    }

    @Test
    void assignPressedKeyToSecondPlayer() throws Exception {
        runOnFxThread(() -> {
            // given: 2P 홀드 줄
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.moveTo(SECOND_PLAYER_FIRST_ROW + PLAYER_ORDER.indexOf(GameCommand.HOLD));

            // when
            fixture.press(KeyCode.ENTER);
            fixture.press(KeyCode.SHIFT);

            // then
            assertEquals("SHIFT",
                    fixture.lastChange().getBattleKeyBindings().getSecondPlayerKeys().get(GameCommand.HOLD));
            assertEquals("2P 홀드: SHIFT", fixture.rowText("battle-key-second-HOLD"));
        });
    }

    @Test
    void assignPressedKeyToSharedPause() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.moveTo(SHARED_FIRST_ROW);

            // when
            fixture.press(KeyCode.ENTER);
            fixture.press(KeyCode.BACK_SPACE);

            // then
            assertEquals("BACK_SPACE",
                    fixture.lastChange().getBattleKeyBindings().getSharedKeys().get(GameCommand.PAUSE));
        });
    }

    @Test
    void rejectKeyUsedByOtherPlayer() throws Exception {
        runOnFxThread(() -> {
            // given: 1P 왼쪽 이동 입력 대기
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.press(KeyCode.ENTER);

            // when: 2P 왼쪽 이동 키(LEFT)를 누른다
            fixture.press(KeyCode.LEFT);

            // then: 바꾸지 않고, 누가 쓰는지 알려 주고, 계속 기다린다
            assertTrue(fixture.changes.isEmpty());
            assertTrue(fixture.message().contains("2P 왼쪽 이동"), fixture.message());
            assertEquals("1P 왼쪽 이동: " + WAITING_KEY_TEXT, fixture.rowText("battle-key-first-MOVE_LEFT"));

            // when: 다른 키를 누르면
            fixture.press(KeyCode.Q);

            // then: 배정되고 메시지가 지워진다
            assertEquals("Q", fixture.lastChange().getBattleKeyBindings().getFirstPlayerKeys().get(GameCommand.MOVE_LEFT));
            assertEquals("", fixture.message());
        });
    }

    @Test
    void rejectKeyUsedBySharedCommand() throws Exception {
        runOnFxThread(() -> {
            // given: 1P 홀드 입력 대기
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.moveTo(PLAYER_ORDER.indexOf(GameCommand.HOLD));
            fixture.press(KeyCode.ENTER);

            // when: 공용 일시정지 키(P)
            fixture.press(KeyCode.P);

            // then
            assertTrue(fixture.changes.isEmpty());
            assertTrue(fixture.message().contains("공용 일시정지"), fixture.message());
        });
    }

    @Test
    void rejectKeyUsedBySamePlayer() throws Exception {
        runOnFxThread(() -> {
            // given: 1P 왼쪽 이동 입력 대기
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.press(KeyCode.ENTER);

            // when: 1P 하드 드롭 키(S)
            fixture.press(KeyCode.S);

            // then
            assertTrue(fixture.changes.isEmpty());
            assertTrue(fixture.message().contains("1P 하드 드롭"), fixture.message());
        });
    }

    @Test
    void allowKeyUsedOnlyBySinglePlayerGame() throws Exception {
        runOnFxThread(() -> {
            // given: 1P 하드 드롭 입력 대기
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.moveTo(PLAYER_ORDER.indexOf(GameCommand.HARD_DROP));
            fixture.press(KeyCode.ENTER);

            // when: 1인 게임 하드 드롭 키(SPACE)
            fixture.press(KeyCode.SPACE);

            // then: 대전 키끼리만 비교하므로 배정된다
            assertEquals("SPACE",
                    fixture.lastChange().getBattleKeyBindings().getFirstPlayerKeys().get(GameCommand.HARD_DROP));
        });
    }

    @Test
    void keepSameKeyWithoutChange() throws Exception {
        runOnFxThread(() -> {
            // given: 1P 왼쪽 이동 입력 대기
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.press(KeyCode.ENTER);

            // when: 지금 키(F)를 다시 누른다
            fixture.press(KeyCode.F);

            // then: 충돌도, 변경 알림도 없고 입력 대기가 끝난다
            assertTrue(fixture.changes.isEmpty());
            assertEquals("", fixture.message());
            assertEquals("1P 왼쪽 이동: F", fixture.rowText("battle-key-first-MOVE_LEFT"));
        });
    }

    @Test
    void cancelKeyCaptureWithEscape() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.press(KeyCode.ENTER);

            // when
            fixture.press(KeyCode.ESCAPE);

            // then: 취소만 하고 뒤로 가지 않는다
            assertTrue(fixture.changes.isEmpty());
            assertEquals(0, fixture.backCount.get());
            assertEquals("1P 왼쪽 이동: F", fixture.rowText("battle-key-first-MOVE_LEFT"));
        });
    }

    @Test
    void ignoreRepeatedEnterUntilReleased() throws Exception {
        runOnFxThread(() -> {
            // given: Enter로 입력 대기를 시작했다
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.press(KeyCode.ENTER);

            // when: Enter를 누르고 있어 반복 입력이 들어온다
            fixture.press(KeyCode.ENTER);

            // then: Enter를 키로 배정하지 않고 계속 기다린다
            assertTrue(fixture.changes.isEmpty());
            assertEquals("1P 왼쪽 이동: " + WAITING_KEY_TEXT, fixture.rowText("battle-key-first-MOVE_LEFT"));
        });
    }

    @Test
    void clearConflictMessageWhenMovingSelection() throws Exception {
        runOnFxThread(() -> {
            // given: 충돌 메시지가 보이는 상태에서 입력을 취소했다
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.press(KeyCode.ENTER);
            fixture.press(KeyCode.LEFT);
            fixture.press(KeyCode.ESCAPE);

            // when
            fixture.press(KeyCode.DOWN);

            // then
            assertEquals("", fixture.message());
        });
    }

    // ---------- 기본값 ----------

    @Test
    void askConfirmationBeforeReset() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.moveTo(RESET_ROW);

            // when
            fixture.press(KeyCode.ENTER);

            // then
            assertEquals(RESET_CONFIRMATION_TEXT, fixture.rowText("battle-key-reset"));
            assertTrue(fixture.changes.isEmpty());
        });
    }

    @Test
    void resetOnlyBattleKeysWithY() throws Exception {
        runOnFxThread(() -> {
            // given: 1인 홀드 키와 1P 왼쪽 이동을 바꾼 설정
            GameSettings custom = settingsWithFirstPlayerKey(GameCommand.MOVE_LEFT, "Q");
            GameSettings settings = new GameSettings(WindowSize.SMALL, singleKeysWith(GameCommand.HOLD, "V"), true,
                    Difficulty.EASY, custom.getBattleKeyBindings());
            Fixture fixture = new Fixture(settings);
            fixture.moveTo(RESET_ROW);

            // when
            fixture.press(KeyCode.ENTER);
            fixture.press(KeyCode.Y);

            // then: 대전 키만 기본값, 나머지는 그대로
            GameSettings changed = fixture.lastChange();
            BattleKeyBindings defaults = BattleKeyBindings.defaults();
            assertEquals(defaults.getFirstPlayerKeys(), changed.getBattleKeyBindings().getFirstPlayerKeys());
            assertEquals(defaults.getSecondPlayerKeys(), changed.getBattleKeyBindings().getSecondPlayerKeys());
            assertEquals(defaults.getSharedKeys(), changed.getBattleKeyBindings().getSharedKeys());
            assertEquals("V", changed.getKeyBindings().get(GameCommand.HOLD));
            assertEquals(WindowSize.SMALL, changed.getWindowSize());
            assertTrue(changed.isColorBlindMode());
            assertEquals(Difficulty.EASY, changed.getDifficulty());
            assertEquals("1P 왼쪽 이동: F", fixture.rowText("battle-key-first-MOVE_LEFT"));
            assertEquals(RESET_MESSAGE, fixture.message());
            assertEquals(RESET_ROW_TEXT, fixture.rowText("battle-key-reset"));
        });
    }

    @Test
    void cancelResetWithOtherKey() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(settingsWithFirstPlayerKey(GameCommand.MOVE_LEFT, "Q"));
            fixture.moveTo(RESET_ROW);
            fixture.press(KeyCode.ENTER);

            // when
            fixture.press(KeyCode.N);

            // then
            assertTrue(fixture.changes.isEmpty());
            assertEquals(RESET_ROW_TEXT, fixture.rowText("battle-key-reset"));
            assertEquals(0, fixture.backCount.get());
        });
    }

    // ---------- 뒤로 ----------

    @Test
    void goBackWithEscape() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            fixture.press(KeyCode.ESCAPE);

            // then
            assertEquals(1, fixture.backCount.get());
        });
    }

    @Test
    void goBackWithBackRow() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.moveTo(BACK_ROW);

            // when
            fixture.press(KeyCode.ENTER);

            // then
            assertEquals(1, fixture.backCount.get());
            assertTrue(fixture.changes.isEmpty());
        });
    }

    // ---------- 생성 ----------

    @Test
    void rejectNullArguments() throws Exception {
        runOnFxThread(() -> {
            // given
            Consumer<GameSettings> change = settings -> {
            };
            Runnable back = () -> {
            };

            // when & then
            assertThrows(NullPointerException.class, () -> new JavaFxBattleKeySettingsView(null, change, back));
            assertThrows(NullPointerException.class,
                    () -> new JavaFxBattleKeySettingsView(GameSettings.defaults(), null, back));
            assertThrows(NullPointerException.class,
                    () -> new JavaFxBattleKeySettingsView(GameSettings.defaults(), change, null));
        });
    }

    // ---------- 도우미 ----------

    private static final class Fixture {
        private final List<GameSettings> changes = new ArrayList<>();
        private final AtomicInteger backCount = new AtomicInteger();
        private final JavaFxBattleKeySettingsView view;

        private Fixture(GameSettings settings) {
            view = new JavaFxBattleKeySettingsView(settings, changes::add, backCount::incrementAndGet);
        }

        private GameSettings lastChange() {
            assertFalse(changes.isEmpty(), "변경 알림이 없음");
            return changes.get(changes.size() - 1);
        }

        private Labeled row(String id) {
            Node node = view.getScene().lookup("#" + id);
            assertNotNull(node, "#" + id);
            return (Labeled) node;
        }

        // 선택 표시를 뗀 줄 글자
        private String rowText(String id) {
            String text = row(id).getText();
            if (text.startsWith(SELECTED_MARKER)) {
                return text.substring(SELECTED_MARKER.length());
            }
            if (text.startsWith(UNSELECTED_MARKER)) {
                return text.substring(UNSELECTED_MARKER.length());
            }
            return text;
        }

        private boolean isSelected(String id) {
            return row(id).getText().startsWith(SELECTED_MARKER);
        }

        private String message() {
            Node node = view.getScene().lookup("#battle-key-message");
            assertNotNull(node, "#battle-key-message");
            return ((Label) node).getText();
        }

        private void moveTo(int row) {
            for (int index = 0; index < row; index++) {
                press(KeyCode.DOWN);
            }
        }

        private void press(KeyCode keyCode) {
            fire(KeyEvent.KEY_PRESSED, keyCode);
        }

        private void fire(EventType<KeyEvent> type, KeyCode keyCode) {
            Event.fireEvent(view.getScene().getRoot(),
                    new KeyEvent(type, "", "", keyCode, false, false, false, false));
        }
    }

    private static List<String> createRowIds() {
        List<String> ids = new ArrayList<>();
        for (GameCommand command : PLAYER_ORDER) {
            ids.add("battle-key-first-" + command.name());
        }
        for (GameCommand command : PLAYER_ORDER) {
            ids.add("battle-key-second-" + command.name());
        }
        ids.add("battle-key-shared-" + GameCommand.PAUSE.name());
        ids.add("battle-key-shared-" + GameCommand.QUIT_GAME.name());
        ids.add("battle-key-reset");
        ids.add("battle-key-back");
        return List.copyOf(ids);
    }

    private static GameSettings settingsWithFirstPlayerKey(GameCommand command, String keyName) {
        Map<GameCommand, String> first = new EnumMap<>(GameCommand.class);
        first.putAll(BattleKeyBindings.defaults().getFirstPlayerKeys());
        first.put(command, keyName);
        BattleKeyBindings battleKeys = new BattleKeyBindings(first,
                BattleKeyBindings.defaults().getSecondPlayerKeys(), BattleKeyBindings.defaults().getSharedKeys());
        return new GameSettings(WindowSize.MEDIUM, GameSettings.getDefaultKeyBindings(), false, Difficulty.NORMAL,
                battleKeys);
    }

    private static Map<GameCommand, String> singleKeysWith(GameCommand command, String keyName) {
        Map<GameCommand, String> keys = new HashMap<>(GameSettings.getDefaultKeyBindings());
        keys.put(command, keyName);
        return keys;
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
