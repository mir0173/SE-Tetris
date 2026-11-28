package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * 블루프린트 테마: 글꼴 파일 등록, 스타일시트 붙이기, Label 글꼴 바꾸기
 */
class ThemeTest {

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
    @ValueSource(strings = {
            "/fonts/IBMPlexSansKR-Regular.ttf",
            "/fonts/IBMPlexSansKR-Bold.ttf",
            "/fonts/IBMPlexMono-Regular.ttf",
            "/fonts/IBMPlexMono-Bold.ttf",
            "/fonts/OFL-IBMPlexSansKR.txt",
            "/fonts/OFL-IBMPlexMono.txt"
    })
    void bundleFontFilesWithTheirLicense(String path) throws Exception {
        // given: 배포 파일에 들어갈 글꼴과 라이선스(OFL) 파일

        // when
        try (InputStream input = Theme.class.getResourceAsStream(path)) {
            // then
            assertNotNull(input, path);
        }
    }

    @Test
    void loadFontsRegistersBothThemeFamilies() throws Exception {
        runOnFxThread(() -> {
            // given / when
            Theme.loadFonts();

            // then
            assertTrue(Font.getFamilies().contains(Theme.UI_FONT_FAMILY));
            assertTrue(Font.getFamilies().contains(Theme.MONO_FONT_FAMILY));
        });
    }

    @Test
    void fontHelpersUseThemeFamilyWeightAndSize() throws Exception {
        runOnFxThread(() -> {
            // given / when
            Font ui = Theme.uiFont(FontWeight.BOLD, 18);
            Font mono = Theme.monoFont(FontWeight.BOLD, 28);

            // then
            assertEquals(Theme.UI_FONT_FAMILY, ui.getFamily());
            assertTrue(ui.getStyle().contains("Bold"), ui.getStyle());
            assertEquals(18, ui.getSize(), 0.01);
            assertEquals(Theme.MONO_FONT_FAMILY, mono.getFamily());
            assertEquals(28, mono.getSize(), 0.01);
        });
    }

    @Test
    void applyAddsThemeStylesheetOnlyOnce() throws Exception {
        runOnFxThread(() -> {
            // given
            Scene scene = new Scene(new VBox());

            // when
            Theme.apply(scene);
            Theme.apply(scene);

            // then
            assertEquals(1, scene.getStylesheets().size());
            assertEquals(Theme.getStylesheetUrl(), scene.getStylesheets().get(0));
        });
    }

    @Test
    void applyChangesDefaultLabelFontFamilyButKeepsSizeAndWeight() throws Exception {
        runOnFxThread(() -> {
            // given: 코드에서 크기·굵기만 정한 Label (글꼴 이름은 기본값)
            Label label = new Label("설정");
            label.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 22));
            Scene scene = new Scene(new VBox(label));

            // when
            Theme.apply(scene);

            // then
            assertEquals(Theme.UI_FONT_FAMILY, label.getFont().getFamily());
            assertEquals(22, label.getFont().getSize(), 0.01);
            assertTrue(label.getFont().getStyle().contains("Bold"), label.getFont().getStyle());
        });
    }

    @Test
    void applyKeepsFontThatWasAlreadyChosen() throws Exception {
        runOnFxThread(() -> {
            // given: 고정폭 글꼴을 따로 고른 Label
            Label label = new Label("1P");
            label.setFont(Theme.monoFont(FontWeight.BOLD, 28));
            Scene scene = new Scene(new VBox(label));

            // when
            Theme.apply(scene);

            // then
            assertEquals(Theme.MONO_FONT_FAMILY, label.getFont().getFamily());
            assertEquals(28, label.getFont().getSize(), 0.01);
        });
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
