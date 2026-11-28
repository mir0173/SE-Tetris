package tetris;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.image.Image;

/**
 * 프로그램 아이콘: 창 아이콘 PNG들과 배포용 실행 파일 아이콘(.ico)
 * (1차 요구사항: 배포 패키지는 아이콘이 포함된 실행 파일을 가져야 함)
 */
class AppIconTest {

    // AI-assisted code start
    private static final String ICO_PATH = "/icons/se-tetris.ico";
    private static final int ICO_TYPE_ICON = 1;

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
    void everyWindowIconLoadsAsSquareImage() throws Exception {
        runOnFxThread(() -> {
            for (String path : TetrisApplication.APP_ICON_PATHS) {
                // given
                InputStream input = TetrisApplication.class.getResourceAsStream(path);
                assertNotNull(input, path);

                // when
                Image image = new Image(input);

                // then
                assertFalse(image.isError(), path);
                assertEquals(image.getWidth(), image.getHeight(), 0.01, path);
            }
        });
    }

    @Test
    void executableIconContainsSmallAndLargeSizes() throws IOException {
        // given
        try (InputStream input = TetrisApplication.class.getResourceAsStream(ICO_PATH)) {
            assertNotNull(input, ICO_PATH);

            // when: ICO 헤더(예약 2바이트, 종류 2바이트, 그림 수 2바이트)를 읽는다
            ByteBuffer header = ByteBuffer.wrap(input.readNBytes(6)).order(ByteOrder.LITTLE_ENDIAN);
            header.getShort();
            int type = header.getShort();
            int count = header.getShort();

            // then: 아이콘 파일이고 작은 크기(작업 표시줄)부터 큰 크기(바탕화면)까지 여러 장이 들어 있다
            assertEquals(ICO_TYPE_ICON, type);
            assertEquals(7, count);
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
