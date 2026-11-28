package tetris.view;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

// AI-assisted code start
/**
 * 블루프린트(설계 도면) 테마의 글꼴과 스타일시트를 한 곳에서 관리한다.
 * 화면 클래스는 글꼴을 직접 고르지 않고 이 클래스의 메서드를 쓰고,
 * Scene을 만든 뒤 {@link #apply(Scene)}로 테마 스타일시트를 붙인다.
 */
public final class Theme {

    public static final String UI_FONT_FAMILY = "IBM Plex Sans KR";
    public static final String MONO_FONT_FAMILY = "IBM Plex Mono";
    public static final String STYLESHEET_PATH = "/theme/theme.css";

    private static final Logger LOGGER = Logger.getLogger(Theme.class.getName());
    private static final String FONT_DIRECTORY = "/fonts/";
    private static final double FONT_LOAD_SIZE = 12;
    private static final String[] FONT_FILES = {
        "IBMPlexSansKR-Regular.ttf",
        "IBMPlexSansKR-Bold.ttf",
        "IBMPlexMono-Regular.ttf",
        "IBMPlexMono-Bold.ttf"
    };

    private static boolean fontsLoaded;

    private Theme() {
    }

    /**
     * 테마 글꼴을 불러오고 Scene에 테마 스타일시트를 붙인다. 여러 번 불러도 한 번만 붙는다.
     *
     * @param scene 테마를 적용할 Scene
     */
    public static void apply(Scene scene) {
        loadFonts();
        String stylesheet = getStylesheetUrl();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
        useThemeFonts(scene.getRoot());
    }

    /**
     * 노드 아래 모든 Label의 글꼴 이름을 테마 글꼴로 바꾼다. 크기와 굵기는 그대로 둔다.
     * 이미 다른 글꼴(예: 고정폭 글꼴)을 정한 Label은 건드리지 않는다.
     * 나중에 새로 만드는 Label은 이 메서드를 다시 부르거나 {@link #uiFont}로 글꼴을 정한다.
     *
     * @param node 글꼴을 바꿀 화면 노드
     */
    public static void useThemeFonts(Node node) {
        if (node instanceof Label label) {
            Font font = label.getFont();
            if (font.getFamily().equals(Font.getDefault().getFamily())) {
                label.setFont(uiFont(isBold(font) ? FontWeight.BOLD : FontWeight.NORMAL, font.getSize()));
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                useThemeFonts(child);
            }
        }
    }

    /**
     * resources의 글꼴 파일을 JavaFX에 등록한다. 처음 한 번만 실제로 읽는다.
     * 글꼴 파일을 읽지 못하면 기록만 남기고 기본 글꼴로 동작한다.
     */
    public static synchronized void loadFonts() {
        if (fontsLoaded) {
            return;
        }
        for (String fileName : FONT_FILES) {
            try (InputStream input = Theme.class.getResourceAsStream(FONT_DIRECTORY + fileName)) {
                if (input == null || Font.loadFont(input, FONT_LOAD_SIZE) == null) {
                    LOGGER.warning("글꼴을 불러오지 못했습니다: " + fileName);
                }
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "글꼴 파일을 읽지 못했습니다: " + fileName, exception);
            }
        }
        fontsLoaded = true;
    }

    /** 한글이 들어가는 일반 글자(메뉴, 설명)용 글꼴 */
    public static Font uiFont(double size) {
        return uiFont(FontWeight.NORMAL, size);
    }

    /** 한글이 들어가는 일반 글자(메뉴, 설명)용 글꼴 */
    public static Font uiFont(FontWeight weight, double size) {
        loadFonts();
        return Font.font(UI_FONT_FAMILY, weight, size);
    }

    /** 숫자, 영문 라벨(SCORE, 1P 등), 키 이름용 고정폭 글꼴. 한글은 넣지 않는다 */
    public static Font monoFont(FontWeight weight, double size) {
        loadFonts();
        return Font.font(MONO_FONT_FAMILY, weight, size);
    }

    private static boolean isBold(Font font) {
        return font.getStyle().toLowerCase(Locale.ROOT).contains("bold");
    }

    /** 테마 스타일시트의 주소 */
    public static String getStylesheetUrl() {
        URL url = Theme.class.getResource(STYLESHEET_PATH);
        if (url == null) {
            throw new IllegalStateException("테마 스타일시트가 없습니다: " + STYLESHEET_PATH);
        }
        return url.toExternalForm();
    }
}
// AI-assisted code end
