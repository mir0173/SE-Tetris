package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * 화면 아래 키 안내 줄: 안내 하나 = 키캡(키 이름) 여러 개 + 설명 글자
 */
class KeyHintBarTest {

    // AI-assisted code start
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
    void newBarIsEmptyAndHasStableIdAndStyleClass() throws Exception {
        runOnFxThread(() -> {
            // given / when
            KeyHintBar bar = new KeyHintBar();

            // then
            assertEquals(KeyHintBar.ID, bar.getId());
            assertTrue(bar.getStyleClass().contains(KeyHintBar.STYLE_CLASS));
            assertTrue(bar.getChildren().isEmpty());
        });
    }

    @Test
    void addShowsKeycapsInOrderFollowedByDescription() throws Exception {
        runOnFxThread(() -> {
            // given
            KeyHintBar bar = new KeyHintBar();

            // when
            bar.add("이동", "UP", "DOWN");

            // then: 키 이름은 화면용 글자로 바뀌고, 설명은 맨 뒤에 온다
            assertEquals(1, bar.getChildren().size());
            List<Node> parts = hint(bar, 0).getChildren();
            assertEquals(List.of("↑", "↓", "이동"), texts(parts));
            assertTrue(parts.get(0).getStyleClass().contains(KeyHintBar.KEYCAP_STYLE_CLASS));
            assertTrue(parts.get(1).getStyleClass().contains(KeyHintBar.KEYCAP_STYLE_CLASS));
            assertTrue(parts.get(2).getStyleClass().contains(KeyHintBar.TEXT_STYLE_CLASS));
        });
    }

    @Test
    void addReturnsSameBarSoHintsCanBeChained() throws Exception {
        runOnFxThread(() -> {
            // given
            KeyHintBar bar = new KeyHintBar();

            // when
            KeyHintBar returned = bar.add("선택", "ENTER").add("메뉴로", "ESCAPE");

            // then
            assertSame(bar, returned);
            assertEquals(2, bar.getChildren().size());
            assertEquals(List.of("Enter", "선택"), texts(hint(bar, 0).getChildren()));
            assertEquals(List.of("ESC", "메뉴로"), texts(hint(bar, 1).getChildren()));
            assertTrue(hint(bar, 0).getStyleClass().contains(KeyHintBar.HINT_STYLE_CLASS));
        });
    }

    @Test
    void unassignedKeyIsShownAsPlaceholder() throws Exception {
        runOnFxThread(() -> {
            // given: 설정에 키가 없는 경우(null)
            KeyHintBar bar = new KeyHintBar();

            // when
            bar.add("일시정지", (String) null);

            // then
            assertEquals(List.of(KeyNames.UNASSIGNED, "일시정지"), texts(hint(bar, 0).getChildren()));
        });
    }

    @Test
    void clearRemovesAllHintsForNewScreenState() throws Exception {
        runOnFxThread(() -> {
            // given
            KeyHintBar bar = new KeyHintBar().add("일시정지", "P").add("경기 중단", "ESCAPE");

            // when
            KeyHintBar returned = bar.clear().add("선택", "ENTER");

            // then
            assertSame(bar, returned);
            assertEquals(1, bar.getChildren().size());
            assertEquals(List.of("Enter", "선택"), texts(hint(bar, 0).getChildren()));
        });
    }

    private static HBox hint(KeyHintBar bar, int index) {
        return (HBox) bar.getChildren().get(index);
    }

    private static List<String> texts(List<Node> nodes) {
        return nodes.stream().map(node -> ((Label) node).getText()).toList();
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
