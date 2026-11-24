package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import tetris.battle.BattleMode;
import tetris.battle.BattleSettings;
import tetris.model.Difficulty;
import tetris.settings.GameSettings;
import tetris.settings.WindowSize;

/**
 * 로컬 대전 모드 선택 화면 (P2P 대기실의 대전 설정 칸과 같은 구성)
 *
 * 화면 약속
 * - #battle-mode: 대전 모드(일반/아이템/제한시간), 처음 값은 일반
 * - #battle-difficulty: 난이도, 처음 값은 설정 화면에서 고른 난이도
 * - #battle-duration: 제한시간(초) 10~3600, 처음 값 120. 제한시간 모드일 때만 고를 수 있다
 * - #battle-start "게임 시작": 고른 값으로 BattleSettings를 만들어 넘긴다 (제한시간 모드가 아니면 시간은 0)
 * - #battle-setup-back "뒤로": 메뉴로 돌아간다
 * - #battle-key-settings "대전 키 설정": 대전 키 설정 화면을 연다
 * - Enter = 게임 시작 (단, 다른 버튼에 포커스가 있으면 그 버튼을 실행), ESC = 뒤로
 * - 배치·창 크기·키 안내 같은 화면 모양은 계속 바뀔 수 있으므로 테스트하지 않는다 (직접 보며 조정)
 */
class JavaFxBattleSetupViewTest {

    // AI-assisted code start
    private static final int DURATION_STEP_SECONDS = 10;

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

    // ---------- 처음 값 ----------

    @Test
    void showTitle() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            List<String> texts = new ArrayList<>();
            for (Node node : fixture.view.getScene().getRoot().lookupAll(".label")) {
                if (node instanceof Label label) {
                    texts.add(label.getText());
                }
            }

            // then
            assertTrue(texts.contains("로컬 대전"), texts.toString());
        });
    }

    @Test
    void offerEveryModeAndDifficulty() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            List<?> modes = fixture.modeBox().getItems();
            List<?> difficulties = fixture.difficultyBox().getItems();

            // then
            assertEquals(List.of(BattleMode.values()), modes);
            assertEquals(List.of(Difficulty.values()), difficulties);
        });
    }

    @Test
    void startWithNormalModeAndDefaultTime() throws Exception {
        runOnFxThread(() -> {
            // given

            // when
            Fixture fixture = new Fixture(GameSettings.defaults());

            // then
            assertEquals(BattleMode.NORMAL, fixture.modeBox().getValue());
            assertEquals(BattleSettings.DEFAULT_TIME_LIMIT_SECONDS, fixture.durationSpinner().getValue());
            assertTrue(fixture.durationSpinner().isDisabled());
        });
    }

    @Test
    void startWithDifficultyFromSettings() throws Exception {
        runOnFxThread(() -> {
            // given: 설정 화면에서 HARD를 골라 둔 상태
            GameSettings settings = new GameSettings(
                    WindowSize.MEDIUM, GameSettings.getDefaultKeyBindings(), false, Difficulty.HARD);

            // when
            Fixture fixture = new Fixture(settings);

            // then
            assertEquals(Difficulty.HARD, fixture.difficultyBox().getValue());
        });
    }

    @Test
    void limitDurationLikeP2pLobby() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.modeBox().setValue(BattleMode.TIME_LIMIT);
            Spinner<Integer> duration = fixture.durationSpinner();

            // when: 아주 많이 줄이거나 늘려도
            duration.decrement(1_000);
            int minimum = duration.getValue();
            duration.increment(1_000);
            int maximum = duration.getValue();

            // then
            assertEquals(BattleSettings.MIN_TIME_LIMIT_SECONDS, minimum);
            assertEquals(BattleSettings.MAX_TIME_LIMIT_SECONDS, maximum);
        });
    }

    // ---------- 제한시간 칸 ----------

    @Test
    void enableDurationOnlyInTimeLimitMode() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            fixture.modeBox().setValue(BattleMode.TIME_LIMIT);

            // then
            assertFalse(fixture.durationSpinner().isDisabled());

            // when
            fixture.modeBox().setValue(BattleMode.ITEM);

            // then
            assertTrue(fixture.durationSpinner().isDisabled());
        });
    }

    // ---------- 시작 ----------

    @Test
    void startNormalBattleWithoutTimeLimit() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            fixture.button("battle-start").fire();

            // then
            assertEquals(List.of(new BattleSettings(BattleMode.NORMAL, Difficulty.NORMAL, 0)), fixture.started);
        });
    }

    @Test
    void startItemBattleWithChosenDifficulty() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.modeBox().setValue(BattleMode.ITEM);
            fixture.difficultyBox().setValue(Difficulty.EASY);

            // when
            fixture.button("battle-start").fire();

            // then
            assertEquals(List.of(new BattleSettings(BattleMode.ITEM, Difficulty.EASY, 0)), fixture.started);
        });
    }

    @Test
    void startTimeLimitBattleWithChosenSeconds() throws Exception {
        runOnFxThread(() -> {
            // given: 제한시간 모드에서 120초 → 180초
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.modeBox().setValue(BattleMode.TIME_LIMIT);
            fixture.durationSpinner().increment(60 / DURATION_STEP_SECONDS);

            // when
            fixture.button("battle-start").fire();

            // then
            assertEquals(List.of(new BattleSettings(BattleMode.TIME_LIMIT, Difficulty.NORMAL, 180)), fixture.started);
        });
    }

    @Test
    void ignoreChosenSecondsOutsideTimeLimitMode() throws Exception {
        runOnFxThread(() -> {
            // given: 제한시간 모드에서 시간을 바꿨다가 일반 모드로 돌아왔다
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.modeBox().setValue(BattleMode.TIME_LIMIT);
            fixture.durationSpinner().increment(3);
            fixture.modeBox().setValue(BattleMode.NORMAL);

            // when
            fixture.button("battle-start").fire();

            // then: 일반 모드의 시간은 0
            assertEquals(List.of(new BattleSettings(BattleMode.NORMAL, Difficulty.NORMAL, 0)), fixture.started);
        });
    }

    @Test
    void startWithEnterKey() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.modeBox().setValue(BattleMode.ITEM);

            // when
            fixture.pressKey(KeyCode.ENTER);

            // then
            assertEquals(List.of(new BattleSettings(BattleMode.ITEM, Difficulty.NORMAL, 0)), fixture.started);
            assertEquals(0, fixture.backCount.get());
        });
    }

    // ---------- 뒤로 ----------

    @Test
    void goBackWithBackButton() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            fixture.button("battle-setup-back").fire();

            // then
            assertEquals(1, fixture.backCount.get());
            assertTrue(fixture.started.isEmpty());
        });
    }

    @Test
    void goBackWithEscapeKey() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            fixture.pressKey(KeyCode.ESCAPE);

            // then
            assertEquals(1, fixture.backCount.get());
            assertTrue(fixture.started.isEmpty());
        });
    }

    // ---------- 대전 키 설정으로 가기 ----------

    @Test
    void openKeySettingsWithButton() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());

            // when
            fixture.button("battle-key-settings").fire();

            // then: 키 설정 화면을 열고, 게임을 시작하거나 뒤로 가지 않는다
            assertEquals(1, fixture.keySettingsCount.get());
            assertTrue(fixture.started.isEmpty());
            assertEquals(0, fixture.backCount.get());
        });
    }

    @Test
    void openKeySettingsWithEnterWhenItsButtonIsFocused() throws Exception {
        runOnFxThread(() -> {
            // given: 키보드로 '대전 키 설정' 버튼에 포커스를 옮겼다
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.button("battle-key-settings").requestFocus();

            // when
            fixture.pressKey(KeyCode.ENTER);

            // then: Enter는 포커스된 버튼을 실행한다 (게임 시작이 아님)
            assertEquals(1, fixture.keySettingsCount.get());
            assertTrue(fixture.started.isEmpty());
        });
    }

    @Test
    void goBackWithEnterWhenBackButtonIsFocused() throws Exception {
        runOnFxThread(() -> {
            // given
            Fixture fixture = new Fixture(GameSettings.defaults());
            fixture.button("battle-setup-back").requestFocus();

            // when
            fixture.pressKey(KeyCode.ENTER);

            // then
            assertEquals(1, fixture.backCount.get());
            assertTrue(fixture.started.isEmpty());
        });
    }

    // ---------- 생성 ----------

    @Test
    void rejectNullArguments() throws Exception {
        runOnFxThread(() -> {
            // given
            Consumer<BattleSettings> start = settings -> {
            };
            Runnable none = () -> {
            };

            // when & then
            assertThrows(NullPointerException.class, () -> new JavaFxBattleSetupView(null, start, none, none));
            assertThrows(NullPointerException.class,
                    () -> new JavaFxBattleSetupView(GameSettings.defaults(), null, none, none));
            assertThrows(NullPointerException.class,
                    () -> new JavaFxBattleSetupView(GameSettings.defaults(), start, null, none));
            assertThrows(NullPointerException.class,
                    () -> new JavaFxBattleSetupView(GameSettings.defaults(), start, none, null));
        });
    }

    // ---------- 도우미 ----------

    private static final class Fixture {
        private final List<BattleSettings> started = new ArrayList<>();
        private final AtomicInteger keySettingsCount = new AtomicInteger();
        private final AtomicInteger backCount = new AtomicInteger();
        private final JavaFxBattleSetupView view;

        private Fixture(GameSettings settings) {
            view = new JavaFxBattleSetupView(settings, started::add, keySettingsCount::incrementAndGet,
                    backCount::incrementAndGet);
        }

        @SuppressWarnings("unchecked")
        private ComboBox<BattleMode> modeBox() {
            return (ComboBox<BattleMode>) find("battle-mode");
        }

        @SuppressWarnings("unchecked")
        private ComboBox<Difficulty> difficultyBox() {
            return (ComboBox<Difficulty>) find("battle-difficulty");
        }

        @SuppressWarnings("unchecked")
        private Spinner<Integer> durationSpinner() {
            return (Spinner<Integer>) find("battle-duration");
        }

        private Button button(String id) {
            return (Button) find(id);
        }

        private Node find(String id) {
            Node node = view.getScene().lookup("#" + id);
            assertNotNull(node, "#" + id);
            return node;
        }

        private void pressKey(KeyCode keyCode) {
            Event.fireEvent(view.getScene().getRoot(),
                    new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode, false, false, false, false));
        }
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
