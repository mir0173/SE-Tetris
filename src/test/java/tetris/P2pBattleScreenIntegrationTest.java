package tetris;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import tetris.battle.BattleMode;
import tetris.battle.BattleSettings;
import tetris.model.GameKind;

// AI-assisted code start
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class P2pBattleScreenIntegrationTest {
    private static final int SCREEN_LATENCY_LIMIT_MILLIS = 200;
    private static final int LATENCY_SAMPLES_PER_DIRECTION = 6;
    @TempDir Path data;
    private String previousHome;
    private Stage hostStage;
    private Stage clientStage;
    private TetrisApplication host;
    private TetrisApplication client;

    @BeforeAll
    static void initializeJavaFx() throws Exception {
        try { Platform.startup(() -> { }); } catch (IllegalStateException alreadyStarted) { }
        onFx(() -> { Platform.setImplicitExit(false); return null; });
    }

    @BeforeEach
    void openTwoApplications() throws Exception {
        previousHome = System.getProperty("user.home");
        System.setProperty("user.home", data.toString());
        onFx(() -> {
            hostStage = new Stage();
            clientStage = new Stage();
            host = new TetrisApplication();
            client = new TetrisApplication();
            host.start(hostStage);
            client.start(clientStage);
            return null;
        });
    }

    @AfterEach
    void closeApplications() throws Exception {
        try {
            onFx(() -> {
                if (host != null) { host.stop(); }
                if (client != null) { client.stop(); }
                if (hostStage != null) { hostStage.close(); }
                if (clientStage != null) { clientStage.close(); }
                return null;
            });
        } finally {
            System.setProperty("user.home", previousHome);
        }
    }

    @Test
    void reflectInputOnBothRemoteScreensWithinLatencyLimitOnLoopback() throws Exception {
        connectPeers();
        startMatch();
        await(() -> label(hostStage, "p2p-battle-health").getText().matches(".*[0-9]+\\s*ms")
                && label(clientStage, "p2p-battle-health").getText().matches(".*[0-9]+\\s*ms"));
        List<Double> hostToClient = measureScreenLatency(hostStage, clientStage);
        List<Double> clientToHost = measureScreenLatency(clientStage, hostStage);
        double maximum = Math.max(hostToClient.stream().mapToDouble(Double::doubleValue).max().orElseThrow(),
                clientToHost.stream().mapToDouble(Double::doubleValue).max().orElseThrow());
        System.out.printf("[T22 화면 지연, 같은 PC] HOST→CLIENT %s ms, CLIENT→HOST %s ms, 최대 %.2f ms (기준 %d ms)%n",
                hostToClient, clientToHost, maximum, SCREEN_LATENCY_LIMIT_MILLIS);
        assertTrue(maximum <= SCREEN_LATENCY_LIMIT_MILLIS, "키 입력부터 상대 화면 반영까지 " + maximum + "ms");
    }

    private List<Double> measureScreenLatency(Stage sender, Stage receiver) throws Exception {
        List<Double> samples = new ArrayList<>();
        for (int sample = 0; sample < LATENCY_SAMPLES_PER_DIRECTION; sample++) {
            CompletableFuture<Double> displayed = new CompletableFuture<>();
            Label remoteScore = onFx(() -> scoreLabel(receiver, "p2p-remote-panel"));
            ChangeListener<String> measure = onFx(() -> {
                Label localScore = scoreLabel(sender, "p2p-local-panel");
                long initialScore = scoreValue(localScore.getText());
                Node board = sender.getScene().lookup("#p2p-local-panel");
                long started = System.nanoTime();
                Event.fireEvent(board, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN,
                        false, false, false, false));
                long expected = scoreValue(localScore.getText());
                assertTrue(expected > initialScore, "소프트드롭 점수가 증가해야 합니다.");
                ChangeListener<String> listener = (ignored, previous, current) -> {
                    if (scoreValue(current) >= expected) {
                        displayed.complete((System.nanoTime() - started) / 1_000_000.0);
                    }
                };
                remoteScore.textProperty().addListener(listener);
                return listener;
            });
            try {
                samples.add(displayed.get(5, TimeUnit.SECONDS));
            } finally {
                onFx(() -> { remoteScore.textProperty().removeListener(measure); return null; });
            }
        }
        return samples;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void treatWindowCloseAsForfeitAndShowRemainingPlayerResult(boolean hostCloses) throws Exception {
        connectPeers();
        startMatch();
        Stage leaver = hostCloses ? hostStage : clientStage;
        Stage remaining = hostCloses ? clientStage : hostStage;
        onFx(() -> {
            Event.fireEvent(leaver, new WindowEvent(leaver, WindowEvent.WINDOW_CLOSE_REQUEST));
            return null;
        });
        await(() -> !leaver.isShowing() && result(remaining).startsWith("승리 기권"));
        await(() -> label(remaining, "p2p-battle-status").getText().contains("연결이"));
        assertTrue(onFx(() -> result(remaining).startsWith("승리 기권")));
        System.out.printf("[T22 창 닫기] %s 기권, 남은 쪽 승리 표시 유지 PASS%n", hostCloses ? "HOST" : "CLIENT");
    }

    @Test
    void showBothBoardsTransmitInputAndReturnWithoutReconnecting() throws Exception {
        connectPeers();
        assertEquals("127.0.0.1", Files.readString(data.resolve(".se-tetris/last-peer-ip.txt")).strip());
        startMatch();
        onFx(() -> {
            Parent local = (Parent) hostStage.getScene().lookup("#p2p-local-panel");
            assertFalse(((Label) local.lookup("#game-key-help")).getText().contains("일시정지"));
            assertTrue(((Label) local.lookup("#game-key-help")).getText().contains("대기실"));
            return null;
        });
        long before = System.nanoTime();
        long localScore = onFx(() -> {
            Node board = hostStage.getScene().lookup("#p2p-local-panel");
            Event.fireEvent(board, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE,
                    false, false, false, false));
            return score(hostStage, "p2p-local-panel");
        });
        assertTrue(localScore > 0);
        await(() -> score(clientStage, "p2p-remote-panel") >= localScore);
        double elapsed = (System.nanoTime() - before) / 1_000_000.0;
        System.out.printf("[T20 화면 통합] 두 앱 접속·양쪽 준비·입력 후 상대 점수 표시 PASS (관측 %.3fms)%n", elapsed);
        captureOptionalPreview();
        await(() -> label(hostStage, "p2p-battle-health").getText().matches(".*[0-9]+\\s*ms"));

        onFx(() -> { button(clientStage, "p2p-battle-back").fire(); return null; });
        await(() -> result(hostStage).startsWith("승리 기권")
                && result(clientStage).startsWith("패배 기권"));
        System.out.println(onFx(() -> "[T20 CLIENT 기권] HOST " + result(hostStage) + " / CLIENT " + result(clientStage)));
        onFx(() -> { button(clientStage, "p2p-battle-back").fire(); return null; });
        await(() -> hostStage.getScene().lookup("#p2p-ready") != null
                && clientStage.getScene().lookup("#p2p-ready") != null);
        await(() -> !button(hostStage, "p2p-ready").isDisabled()
                && !button(clientStage, "p2p-ready").isDisabled());
        startMatch();
        onFx(() -> { client.stop(); clientStage.close(); return null; });
        await(() -> hostStage.getScene().lookup("#p2p-ready") != null
                && !button(hostStage, "p2p-host").isDisabled());
        System.out.println("[T20 화면 수명] 클라이언트 대기실 복귀→재접속 없는 재진입→상대 앱 종료 후 대기실 복귀 PASS");
        // 초기 입력값과 다른 저장 주소로 다시 열었을 때 파일에서 불러오는지 확인한다.
        Files.writeString(data.resolve(".se-tetris/last-peer-ip.txt"), "192.0.2.1");
        onFx(() -> {
            assertTrue(label(hostStage, "p2p-status").getText().contains("연결이"));
            assertFalse(label(hostStage, "p2p-health").isVisible());
            button(hostStage, "p2p-back").fire();
            click(hostStage, "P2P 대전");
            assertEquals("192.0.2.1", field(hostStage, "p2p-address").getText());
            return null;
        });
        System.out.println("[T21 화면 통합] 경기 중 왕복 지연 표시·단절 후 초기 상태 복귀·최근 IP 채우기 PASS");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void showComplementaryGameOverResultsAndRetainThemAfterDisconnect(boolean hostLoses) throws Exception {
        connectPeers();
        startMatch();
        Stage loserStage = hostLoses ? hostStage : clientStage;
        Stage winnerStage = hostLoses ? clientStage : hostStage;
        long losingScore = onFx(() -> {
            Node board = loserStage.getScene().lookup("#p2p-local-panel");
            for (int drop = 0; drop < 35; drop++) {
                Event.fireEvent(board, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE,
                        false, false, false, false));
            }
            return score(loserStage, "p2p-local-panel");
        });
        await(() -> result(loserStage).startsWith("패배 게임오버")
                && result(winnerStage).startsWith("승리 게임오버"));
        String loss = onFx(() -> result(loserStage));
        String win = onFx(() -> result(winnerStage));
        long winningScore = scorePair(win)[0];
        assertArrayEquals(new long[]{losingScore, winningScore}, scorePair(loss));
        assertArrayEquals(new long[]{winningScore, losingScore}, scorePair(win));
        onFx(() -> {
            if (hostLoses) { host.stop(); } else { client.stop(); }
            loserStage.close();
            return null;
        });
        await(() -> label(winnerStage, "p2p-battle-status").getText().contains("연결이"));
        assertEquals(win, onFx(() -> result(winnerStage)));
        onFx(() -> { button(winnerStage, "p2p-battle-back").fire(); return null; });
        await(() -> winnerStage.getScene().lookup("#p2p-host") != null
                && !button(winnerStage, "p2p-host").isDisabled());
        System.out.printf("[T20 게임오버] %s: %s / 상대: %s / 단절 후 결과 유지 PASS%n",
                hostLoses ? "HOST" : "CLIENT", loss, win);
    }

    @Test
    void showHostForfeitResultOnBothScreensBeforeReturning() throws Exception {
        connectPeers();
        startMatch();
        onFx(() -> { button(hostStage, "p2p-battle-back").fire(); return null; });
        await(() -> result(hostStage).startsWith("패배 기권")
                && result(clientStage).startsWith("승리 기권"));
        onFx(() -> { button(hostStage, "p2p-battle-back").fire(); return null; });
        await(() -> hostStage.getScene().lookup("#p2p-ready") != null
                && clientStage.getScene().lookup("#p2p-ready") != null);
        System.out.println("[T20 HOST 기권] 양쪽 결과 표시 후 호스트 요청으로 대기실 복귀 PASS");
    }

    @Test
    void showSameCountdownAndHostScoreDecisionOnBothScreens() throws Exception {
        connectPeers();
        onFx(() -> {
            ComboBox<?> mode = (ComboBox<?>) hostStage.getScene().lookup("#p2p-mode");
            mode.getSelectionModel().select(mode.getItems().indexOf(BattleMode.TIME_LIMIT));
            Spinner<?> duration = (Spinner<?>) hostStage.getScene().lookup("#p2p-duration");
            duration.decrement(BattleSettings.DEFAULT_TIME_LIMIT_SECONDS - BattleSettings.MIN_TIME_LIMIT_SECONDS);
            return null;
        });
        await(() -> BattleMode.TIME_LIMIT.equals(((ComboBox<?>) clientStage.getScene().lookup("#p2p-mode")).getValue())
                && Integer.valueOf(BattleSettings.MIN_TIME_LIMIT_SECONDS).equals(
                        ((Spinner<?>) clientStage.getScene().lookup("#p2p-duration")).getValue()));
        startMatch();
        await(() -> !timeLeft(hostStage).isEmpty() && timeLeft(hostStage).equals(timeLeft(clientStage)));
        String startingTime = onFx(() -> timeLeft(hostStage));
        onFx(() -> {
            Node board = hostStage.getScene().lookup("#p2p-local-panel");
            Event.fireEvent(board, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE,
                    false, false, false, false));
            return null;
        });
        await(() -> result(hostStage).contains("시간 종료") && result(clientStage).contains("시간 종료"),
                BattleSettings.MIN_TIME_LIMIT_SECONDS + 5);
        onFx(() -> {
            assertEquals("남은 시간 0:00", timeLeft(hostStage));
            assertEquals(timeLeft(hostStage), timeLeft(clientStage));
            assertTrue(result(hostStage).startsWith("승리 시간 종료"));
            assertTrue(result(clientStage).startsWith("패배 시간 종료"));
            System.out.printf("[T20 화면 제한시간] %s → %s / HOST %s / CLIENT %s PASS%n",
                    startingTime, timeLeft(hostStage), result(hostStage), result(clientStage));
            return null;
        });
    }

    @Test
    void synchronizePuyoChoiceAndBothPuyoScreensOverLoopback() throws Exception {
        connectPeers();
        onFx(() -> {
            ComboBox<?> gameBox = (ComboBox<?>) hostStage.getScene().lookup("#p2p-game");
            gameBox.getSelectionModel().select(gameBox.getItems().indexOf(GameKind.PUYO));
            return null;
        });
        await(() -> GameKind.PUYO.equals(((ComboBox<?>) clientStage.getScene().lookup("#p2p-game")).getValue()));
        assertTrue(onFx(() -> clientStage.getScene().lookup("#p2p-game").isDisabled()));
        startMatch();
        await(() -> displaysPuyo(hostStage, "p2p-local-panel") && displaysPuyo(hostStage, "p2p-remote-panel")
                && displaysPuyo(clientStage, "p2p-local-panel") && displaysPuyo(clientStage, "p2p-remote-panel"));

        long hostScore = hardDropPuyo(hostStage);
        long clientScore = hardDropPuyo(clientStage);
        assertTrue(hostScore > 0);
        assertTrue(clientScore > 0);
        await(() -> score(clientStage, "p2p-remote-panel") == hostScore
                && score(hostStage, "p2p-remote-panel") == clientScore);
        onFx(() -> { button(clientStage, "p2p-battle-back").fire(); return null; });
        await(() -> result(hostStage).startsWith("승리 기권") && result(clientStage).startsWith("패배 기권"));
        System.out.printf("[P2P 뿌요] 양쪽 보드와 점수 동기화 %d:%d, 기권 처리 PASS%n", hostScore, clientScore);
    }

    private static boolean displaysPuyo(Stage stage, String panelId) {
        Parent panel = (Parent) stage.getScene().lookup("#" + panelId);
        return panel.lookup("#puyo-chain").isVisible();
    }

    private static long hardDropPuyo(Stage stage) throws Exception {
        return onFx(() -> {
            Node board = stage.getScene().lookup("#p2p-local-panel");
            Event.fireEvent(board, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE,
                    false, false, false, false));
            return score(stage, "p2p-local-panel");
        });
    }

    private void connectPeers() throws Exception {
        int port;
        try (ServerSocket reservation = new ServerSocket(0)) { port = reservation.getLocalPort(); }
        onFx(() -> {
            click(hostStage, "P2P 대전");
            click(clientStage, "P2P 대전");
            field(hostStage, "p2p-port").setText(String.valueOf(port));
            field(clientStage, "p2p-port").setText(String.valueOf(port));
            button(hostStage, "p2p-host").fire();
            return null;
        });
        await(() -> label(hostStage, "p2p-status").getText().contains("서버 대기"));
        onFx(() -> { button(clientStage, "p2p-connect").fire(); return null; });
        try {
            await(() -> !button(hostStage, "p2p-ready").isDisabled()
                    && !button(clientStage, "p2p-ready").isDisabled());
        } catch (AssertionError failure) {
            String statuses = onFx(() -> "HOST " + label(hostStage, "p2p-status").getText()
                    + " / CLIENT " + label(clientStage, "p2p-status").getText());
            throw new AssertionError(statuses, failure);
        }
    }

    private void startMatch() throws Exception {
        onFx(() -> {
            button(hostStage, "p2p-ready").fire();
            button(clientStage, "p2p-ready").fire();
            return null;
        });
        await(() -> hostStage.getScene().lookup("#p2p-local-panel") != null
                && clientStage.getScene().lookup("#p2p-local-panel") != null);
    }

    private void captureOptionalPreview() throws Exception {
        String output = System.getProperty("tetris.preview.output");
        if (output == null) { return; }
        onFx(() -> {
            var image = hostStage.getScene().getRoot().snapshot(null, null);
            BufferedImage result = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < result.getHeight(); y++) {
                for (int x = 0; x < result.getWidth(); x++) {
                    result.setRGB(x, y, image.getPixelReader().getArgb(x, y));
                }
            }
            ImageIO.write(result, "png", Path.of(output).toFile());
            return null;
        });
    }

    private static long score(Stage stage, String panelId) {
        return scoreValue(scoreLabel(stage, panelId).getText());
    }
    private static Label scoreLabel(Stage stage, String panelId) {
        Parent panel = (Parent) stage.getScene().lookup("#" + panelId);
        panel.applyCss();
        return (Label) panel.lookup("#score-label");
    }
    private static long scoreValue(String text) {
        return Long.parseLong(text.substring(text.indexOf(':') + 1).strip());
    }
    private static TextField field(Stage stage, String id) { return (TextField) stage.getScene().lookup("#" + id); }
    private static Button button(Stage stage, String id) { return (Button) stage.getScene().lookup("#" + id); }
    private static Label label(Stage stage, String id) { return (Label) stage.getScene().lookup("#" + id); }
    private static String result(Stage stage) { return label(stage, "p2p-battle-result").getText(); }
    private static String timeLeft(Stage stage) { return label(stage, "p2p-battle-time-left").getText(); }

    private static long[] scorePair(String result) {
        int local = result.indexOf("(나 ") + "(나 ".length();
        int remote = result.indexOf(" : 상대 ");
        return new long[]{
            Long.parseLong(result.substring(local, remote)),
            Long.parseLong(result.substring(remote + " : 상대 ".length(), result.length() - 1))
        };
    }

    private static void click(Stage stage, String text) {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().lookupAll(".button").stream().filter(Button.class::isInstance)
                .map(Button.class::cast).filter(button -> button.getText().equals(text)).findFirst().orElseThrow().fire();
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        await(condition, 5);
    }
    private static void await(Callable<Boolean> condition, int seconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < deadline) {
            if (onFx(condition)) { return; }
            Thread.sleep(10);
        }
        fail("양쪽 화면 상태가 " + seconds + "초 내에 갱신되지 않았습니다.");
    }
    private static <T> T onFx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        try { return task.get(10, TimeUnit.SECONDS); }
        catch (ExecutionException exception) {
            if (exception.getCause() instanceof Error error) { throw error; }
            if (exception.getCause() instanceof Exception cause) { throw cause; }
            throw new RuntimeException(exception.getCause());
        }
    }
}
// AI-assisted code end
