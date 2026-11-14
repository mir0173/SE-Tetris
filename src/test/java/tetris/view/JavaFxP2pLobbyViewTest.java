package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import tetris.model.Difficulty;
import tetris.network.LobbyMode;
import tetris.network.LobbySettings;
import tetris.network.LobbyState;
import tetris.network.MatchStart;
import tetris.network.P2pLobbyController;

// AI-assisted code start
class JavaFxP2pLobbyViewTest {
    private final List<Endpoint> endpoints = new ArrayList<>();

    @BeforeAll
    static void initializeJavaFx() throws Exception {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // 다른 테스트가 시작한 JavaFX를 공유한다.
        }
        onFx(() -> { Platform.setImplicitExit(false); return null; });
    }

    @AfterEach
    void closeControllers() throws Exception {
        onFx(() -> { endpoints.forEach(endpoint -> endpoint.controller.close()); return null; });
    }

    @Test
    void disableReadyAndConfigurationBeforeConnection() throws Exception {
        Endpoint endpoint = endpoint();
        onFx(() -> {
            assertTrue(endpoint.button("p2p-ready").isDisabled());
            assertTrue(endpoint.view.getScene().lookup("#p2p-mode").isDisabled());
            assertFalse(endpoint.button("p2p-host").isDisabled());
            assertFalse(endpoint.button("p2p-connect").isDisabled());
            return null;
        });
    }

    @Test
    void showInvalidPortWithoutOpeningServer() throws Exception {
        Endpoint endpoint = endpoint();
        onFx(() -> {
            ((TextField) endpoint.view.getScene().lookup("#p2p-port")).setText("not-a-port");
            endpoint.button("p2p-host").fire();
            assertTrue(endpoint.label("p2p-status").getText().contains("포트"));
            assertEquals(LobbyState.Phase.IDLE, endpoint.controller.getState().phase());
            return null;
        });
    }

    @Test
    void synchronizeModeSelectionAndStartWithButtons() throws Exception {
        Endpoint host = endpoint();
        Endpoint client = endpoint();
        connect(host, client);
        onFx(() -> {
            host.modeBox().setValue(LobbyMode.ITEM);
            return null;
        });
        client.await(state -> state.settings().mode() == LobbyMode.ITEM);
        onFx(() -> {
            assertEquals(LobbyMode.ITEM, client.modeBox().getValue());
            assertTrue(client.modeBox().isDisabled());
            assertFalse(host.modeBox().isDisabled());
            host.button("p2p-ready").fire();
            client.button("p2p-ready").fire();
            return null;
        });
        assertNotNull(host.starts.poll(5, TimeUnit.SECONDS));
        assertNotNull(client.starts.poll(5, TimeUnit.SECONDS));
        onFx(() -> {
            assertTrue(host.label("p2p-match").getText().contains("아이템"));
            assertTrue(client.label("p2p-match").getText().contains("아이템"));
            assertTrue(host.button("p2p-ready").isDisabled());
            assertTrue(host.button("p2p-reset").isVisible());
            assertFalse(client.button("p2p-reset").isVisible());
            return null;
        });
        System.out.println("[P2P 화면] 서버 모드 선택 → 상대 표시 → 양쪽 버튼 준비 → 준비 완료 표시 PASS");
    }

    @Test
    void resetDisplayedReadinessAfterSettingsChange() throws Exception {
        Endpoint host = endpoint();
        Endpoint client = endpoint();
        connect(host, client);
        onFx(() -> { host.button("p2p-ready").fire(); return null; });
        client.await(LobbyState::remoteReady);
        onFx(() -> { host.modeBox().setValue(LobbyMode.TIME_LIMIT); return null; });
        client.await(state -> state.settings().mode() == LobbyMode.TIME_LIMIT);
        onFx(() -> {
            assertEquals("나: 대기", host.label("p2p-local-ready").getText());
            assertEquals("상대: 대기", client.label("p2p-remote-ready").getText());
            assertFalse(host.view.getScene().lookup("#p2p-duration").isDisabled());
            assertTrue(client.view.getScene().lookup("#p2p-duration").isDisabled());
            return null;
        });
    }

    @Test
    void closeControllerWhenReturningToMenu() throws Exception {
        Endpoint endpoint = endpoint();
        onFx(() -> { endpoint.button("p2p-back").fire(); return null; });
        assertEquals(LobbyState.Phase.CLOSED, endpoint.controller.getState().phase());
    }

    private Endpoint endpoint() throws Exception {
        Endpoint endpoint = onFx(Endpoint::new);
        endpoints.add(endpoint);
        return endpoint;
    }

    private void connect(Endpoint host, Endpoint client) throws Exception {
        onFx(() -> { host.controller.host(0); return null; });
        int port = host.await(state -> state.phase() == LobbyState.Phase.HOSTING && state.port() > 0).port();
        onFx(() -> { client.controller.connect("127.0.0.1", port); return null; });
        host.await(state -> state.phase() == LobbyState.Phase.CONNECTED);
        client.await(state -> state.phase() == LobbyState.Phase.CONNECTED);
    }

    private static final class Endpoint {
        private final JavaFxP2pLobbyView view = new JavaFxP2pLobbyView();
        private final BlockingQueue<LobbyState> states = new LinkedBlockingQueue<>();
        private final BlockingQueue<MatchStart> starts = new LinkedBlockingQueue<>();
        private final P2pLobbyController controller = new P2pLobbyController(Platform::runLater,
                state -> { view.render(state); states.add(state); },
                start -> { view.showMatchReady(start); starts.add(start); },
                new LobbySettings(LobbyMode.NORMAL, Difficulty.NORMAL, 0));

        private Endpoint() {
            view.bind(controller, controller::close);
        }

        private Button button(String id) {
            view.getScene().getRoot().applyCss();
            return (Button) view.getScene().lookup("#" + id);
        }

        private Label label(String id) {
            return (Label) view.getScene().lookup("#" + id);
        }

        @SuppressWarnings("unchecked")
        private ComboBox<LobbyMode> modeBox() {
            return (ComboBox<LobbyMode>) view.getScene().lookup("#p2p-mode");
        }

        private LobbyState await(Predicate<LobbyState> condition) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (true) {
                LobbyState state = states.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                assertNotNull(state, "화면 갱신 시간 초과: " + controller.getState());
                if (condition.test(state)) {
                    return state;
                }
            }
        }
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        try {
            return task.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof Error error) {
                throw error;
            }
            if (exception.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw new RuntimeException(exception.getCause());
        }
    }
}
// AI-assisted code end
