package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;

/**
 * 화면 뒤 제도 도구 장식: 게임을 방해하지 않도록 흐리고, 마우스를 받지 않고, 창 크기에 맞춰 그린다
 */
class DraftingDeskBackgroundTest {

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

    @ParameterizedTest
    @EnumSource(DraftingDeskBackground.Layout.class)
    void staysFaintAndIgnoresMouse(DraftingDeskBackground.Layout layout) throws Exception {
        runOnFxThread(() -> {
            // given / when
            DraftingDeskBackground background = new DraftingDeskBackground(layout, false);

            // then
            assertEquals(DraftingDeskBackground.OPACITY, background.getOpacity(), 0.001);
            assertTrue(background.getOpacity() < 0.5);
            assertTrue(background.isMouseTransparent());
        });
    }

    @ParameterizedTest
    @EnumSource(DraftingDeskBackground.Layout.class)
    void drawsToolsToFillWindowSize(DraftingDeskBackground.Layout layout) throws Exception {
        runOnFxThread(() -> {
            // given
            DraftingDeskBackground background = new DraftingDeskBackground(layout, true);
            Scene scene = new Scene(new StackPane(background), 400, 300);

            // when
            scene.getRoot().applyCss();
            scene.getRoot().layout();

            // then: 캔버스가 창 크기만큼 커지고 무언가 그려져 있다
            Canvas canvas = (Canvas) background.getChildren().get(0);
            assertEquals(400, canvas.getWidth(), 0.01);
            assertEquals(300, canvas.getHeight(), 0.01);
            assertTrue(hasDrawnPixel(canvas));
        });
    }

    private static boolean hasDrawnPixel(Canvas canvas) {
        javafx.scene.SnapshotParameters parameters = new javafx.scene.SnapshotParameters();
        parameters.setFill(javafx.scene.paint.Color.TRANSPARENT);
        WritableImage image = canvas.snapshot(parameters, null);
        for (int y = 0; y < (int) image.getHeight(); y += 2) {
            for (int x = 0; x < (int) image.getWidth(); x += 2) {
                if (image.getPixelReader().getColor(x, y).getOpacity() > 0) {
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
