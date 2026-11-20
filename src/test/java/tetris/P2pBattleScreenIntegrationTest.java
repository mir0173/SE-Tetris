package tetris;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

// AI-assisted code start
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class P2pBattleScreenIntegrationTest {
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
    void showBothBoardsTransmitInputAndReturnWithoutReconnecting() throws Exception {
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
        await(() -> !button(hostStage, "p2p-ready").isDisabled()
                && !button(clientStage, "p2p-ready").isDisabled());
        onFx(() -> {
            button(hostStage, "p2p-ready").fire();
            button(clientStage, "p2p-ready").fire();
            return null;
        });
        await(() -> hostStage.getScene().lookup("#p2p-local-panel") != null
                && clientStage.getScene().lookup("#p2p-local-panel") != null);
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

        onFx(() -> { button(clientStage, "p2p-battle-back").fire(); return null; });
        await(() -> hostStage.getScene().lookup("#p2p-ready") != null
                && clientStage.getScene().lookup("#p2p-ready") != null);
        await(() -> !button(hostStage, "p2p-ready").isDisabled()
                && !button(clientStage, "p2p-ready").isDisabled());
        onFx(() -> {
            button(hostStage, "p2p-ready").fire();
            button(clientStage, "p2p-ready").fire();
            return null;
        });
        await(() -> hostStage.getScene().lookup("#p2p-local-panel") != null
                && clientStage.getScene().lookup("#p2p-local-panel") != null);
        onFx(() -> { client.stop(); clientStage.close(); return null; });
        await(() -> hostStage.getScene().lookup("#p2p-ready") != null
                && !button(hostStage, "p2p-host").isDisabled());
        System.out.println("[T20 화면 수명] 클라이언트 대기실 복귀→재접속 없는 재진입→상대 앱 종료 후 대기실 복귀 PASS");
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
        Parent panel = (Parent) stage.getScene().lookup("#" + panelId);
        panel.applyCss();
        String text = ((Label) panel.lookup("#score-label")).getText();
        return Long.parseLong(text.substring(text.indexOf(':') + 1).strip());
    }
    private static TextField field(Stage stage, String id) { return (TextField) stage.getScene().lookup("#" + id); }
    private static Button button(Stage stage, String id) { return (Button) stage.getScene().lookup("#" + id); }
    private static Label label(Stage stage, String id) { return (Label) stage.getScene().lookup("#" + id); }
    private static void click(Stage stage, String text) {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().lookupAll(".button").stream().filter(Button.class::isInstance)
                .map(Button.class::cast).filter(button -> button.getText().equals(text)).findFirst().orElseThrow().fire();
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (onFx(condition)) { return; }
            Thread.sleep(10);
        }
        fail("양쪽 화면 상태가 5초 내에 갱신되지 않았습니다.");
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
