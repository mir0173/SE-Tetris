package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import tetris.model.PieceType;

/**
 * 블록 칸 그리기와 색 (1차 요구사항: 블록마다 다른 색, 색맹 모드에서 적록·청황 색맹도 구분 가능)
 */
class BlockRendererTest {

    // AI-assisted code start
    private static final List<PieceType> BASIC_PIECES = List.of(
            PieceType.I, PieceType.O, PieceType.T, PieceType.S, PieceType.Z, PieceType.J, PieceType.L);
    private static final double CELL = 40;
    private static final double COLOR_TOLERANCE = 0.02;
    /** 작은 칸에서 두 색이 헷갈리지 않는다고 볼 최소 색차(CIEDE2000). docs/redesign-proposal.md 8절 */
    private static final double MIN_DISTINGUISHABLE_DELTA_E = 15;

    // Machado(2009) 색각이상 시뮬레이션 행렬(심도 1.0, 선형 RGB)
    private static final Map<String, double[][]> COLOR_VISION_DEFICIENCIES = Map.of(
            "적색맹", new double[][] {{0.152286, 1.052583, -0.204868}, {0.114503, 0.786281, 0.099216},
                {-0.003882, -0.048116, 1.051998}},
            "녹색맹", new double[][] {{0.367322, 0.860646, -0.227968}, {0.280085, 0.672501, 0.047413},
                {-0.011820, 0.042940, 0.968881}},
            "청황색맹", new double[][] {{1.255528, -0.076749, -0.178779}, {-0.078411, 0.930809, 0.147602},
                {0.004733, 0.691367, 0.303900}});

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

    // ---------- 색 ----------

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void everyPieceTypeHasItsOwnColor(boolean colorBlindMode) {
        // given
        Map<PieceType, Color> colors = colorBlindMode ? BlockRenderer.COLOR_BLIND_COLORS : BlockRenderer.NORMAL_COLORS;

        // when
        Set<Color> distinct = new HashSet<>(colors.values());

        // then: 아이템 블록까지 모든 종류에 색이 있고 서로 다르다
        assertEquals(PieceType.values().length, colors.size());
        assertEquals(colors.size(), distinct.size());
    }

    @Test
    void colorOfFollowsColorBlindSetting() {
        // given
        BlockRenderer normal = new BlockRenderer(false);
        BlockRenderer colorBlind = new BlockRenderer(true);

        // when / then
        for (PieceType type : PieceType.values()) {
            assertEquals(BlockRenderer.NORMAL_COLORS.get(type), normal.colorOf(type));
            assertEquals(BlockRenderer.COLOR_BLIND_COLORS.get(type), colorBlind.colorOf(type));
        }
    }

    @Test
    void basicPiecesAreDistinguishableInNormalVision() {
        // given: 일반 모드 팔레트, 정상 시각

        // when
        double closest = closestPairDeltaE(BlockRenderer.NORMAL_COLORS, null);

        // then
        assertTrue(closest >= MIN_DISTINGUISHABLE_DELTA_E, "가장 비슷한 두 블록의 색차 " + closest);
    }

    @ParameterizedTest
    @ValueSource(strings = {"적색맹", "녹색맹", "청황색맹"})
    void colorBlindPaletteStaysDistinguishableForEachDeficiency(String deficiency) {
        // given: 색맹 모드 팔레트를 그 색각이상인 사람이 보는 색으로 바꾼다
        double[][] simulation = COLOR_VISION_DEFICIENCIES.get(deficiency);

        // when
        double closest = closestPairDeltaE(BlockRenderer.COLOR_BLIND_COLORS, simulation);

        // then: 블록 7종 중 가장 비슷한 두 색도 충분히 다르게 보인다
        assertTrue(closest >= MIN_DISTINGUISHABLE_DELTA_E, deficiency + " 가장 비슷한 두 블록의 색차 " + closest);
    }

    @Test
    void colorBlindModeGivesEveryBasicPieceItsOwnGlyph() {
        // given / when
        Set<String> glyphs = new HashSet<>();
        for (PieceType type : BASIC_PIECES) {
            glyphs.add(BlockRenderer.COLOR_BLIND_GLYPHS.get(type));
        }

        // then: 색 구분이 어려워도 글자 모양으로 7종을 구분한다
        assertEquals(BASIC_PIECES.size(), glyphs.size());
        assertFalse(glyphs.contains(null));
        assertFalse(glyphs.contains(BlockRenderer.BLOCK_GLYPH));
    }

    // ---------- 그리기 ----------

    @Test
    void drawBlockFillsTileWithPieceColor() throws Exception {
        runOnFxThread(() -> {
            // given
            Canvas canvas = new Canvas(CELL, CELL);
            BlockRenderer renderer = new BlockRenderer(false);

            // when
            renderer.drawBlock(canvas.getGraphicsContext2D(), 0, 0, CELL, PieceType.T, null);

            // then: 칸 모서리 근처(글자와 테두리를 피한 곳)는 블록 색
            assertColor(BlockRenderer.NORMAL_COLORS.get(PieceType.T), pixel(canvas, 5, 5));
        });
    }

    @Test
    void drawItemBlockShowsWhiteBoxWithItemLetter() throws Exception {
        runOnFxThread(() -> {
            // given
            Canvas canvas = new Canvas(CELL, CELL);
            BlockRenderer renderer = new BlockRenderer(false);

            // when
            renderer.drawBlock(canvas.getGraphicsContext2D(), 0, 0, CELL, PieceType.L, "L");

            // then: 테두리 쪽은 블록 색, 안쪽은 흰 칸, 아이템 글자는 빨간 색
            assertColor(BlockRenderer.NORMAL_COLORS.get(PieceType.L), pixel(canvas, 4, 4));
            assertColor(BlockRenderer.ITEM_BACKGROUND, pixel(canvas, 10, 10));
            assertTrue(containsColor(canvas, BlockRenderer.ITEM_GLYPH));
        });
    }

    @Test
    void drawGarbageUsesHatchPatternDifferentFromBlocks() throws Exception {
        runOnFxThread(() -> {
            // given
            Canvas canvas = new Canvas(CELL, CELL);
            BlockRenderer renderer = new BlockRenderer(true);

            // when
            renderer.drawGarbage(canvas.getGraphicsContext2D(), 0, 0, CELL);

            // then: 회색 면 위에 그보다 확실히 어두운 빗금이 있다 (무늬로도 직접 쌓은 블록과 구분)
            //       빗금은 1px 대각선이라 가장자리가 부드럽게 섞이므로 정확한 색 대신 밝기로 확인한다
            assertTrue(containsColor(canvas, BlockRenderer.GARBAGE_FILL));
            assertTrue(hasPixelDarkerThan(canvas, BlockRenderer.GARBAGE_FILL.getBrightness() - 0.1));
        });
    }

    @Test
    void drawClearingCoversWholeCellWithHighlight() throws Exception {
        runOnFxThread(() -> {
            // given
            Canvas canvas = new Canvas(CELL, CELL);
            BlockRenderer renderer = new BlockRenderer(false);

            // when
            renderer.drawClearing(canvas.getGraphicsContext2D(), 0, 0, CELL);

            // then: 빈 칸이어도 칸 끝까지 강조색으로 칠한다
            assertColor(BlockRenderer.CLEARING_FILL, pixel(canvas, 1, 1));
            assertColor(BlockRenderer.CLEARING_FILL, pixel(canvas, (int) CELL - 2, (int) CELL - 2));
            assertTrue(containsColor(canvas, BlockRenderer.CLEARING_GLYPH));
        });
    }

    @Test
    void drawGhostLeavesInsideEmpty() throws Exception {
        runOnFxThread(() -> {
            // given: 바탕을 칠한 칸
            Canvas canvas = new Canvas(CELL, CELL);
            BlockRenderer renderer = new BlockRenderer(false);
            renderer.drawBoardBackground(canvas.getGraphicsContext2D(), CELL, CELL, CELL);

            // when
            renderer.drawGhost(canvas.getGraphicsContext2D(), 0, 0, CELL);

            // then: 테두리만 그려지고 가운데는 바탕 그대로
            assertColor(BlockRenderer.BOARD_BACKGROUND, pixel(canvas, (int) CELL / 2, (int) CELL / 2));
        });
    }

    @Test
    void drawPuyoUsesColorBlindColorsAndHighlightWhenClearing() throws Exception {
        runOnFxThread(() -> {
            // given
            Canvas normal = new Canvas(CELL, CELL);
            Canvas clearing = new Canvas(CELL, CELL);
            BlockRenderer renderer = new BlockRenderer(true);

            // when
            renderer.drawPuyo(normal.getGraphicsContext2D(), 0, 0, CELL, 0, false);
            renderer.drawPuyo(clearing.getGraphicsContext2D(), 0, 0, CELL, 0, true);

            // then
            assertTrue(containsColor(normal, BlockRenderer.PUYO_COLOR_BLIND_COLORS.get(0)));
            assertTrue(containsColor(clearing, BlockRenderer.CLEARING_FILL));
        });
    }

    // ---------- 도우미: 색 ----------

    private static double closestPairDeltaE(Map<PieceType, Color> palette, double[][] simulation) {
        double closest = Double.MAX_VALUE;
        for (int first = 0; first < BASIC_PIECES.size(); first++) {
            for (int second = first + 1; second < BASIC_PIECES.size(); second++) {
                double[] a = toLab(simulate(palette.get(BASIC_PIECES.get(first)), simulation));
                double[] b = toLab(simulate(palette.get(BASIC_PIECES.get(second)), simulation));
                closest = Math.min(closest, deltaE2000(a, b));
            }
        }
        return closest;
    }

    private static double[] simulate(Color color, double[][] matrix) {
        double[] linear = {toLinear(color.getRed()), toLinear(color.getGreen()), toLinear(color.getBlue())};
        if (matrix == null) {
            return linear;
        }
        double[] result = new double[3];
        for (int row = 0; row < 3; row++) {
            double value = 0;
            for (int k = 0; k < 3; k++) {
                value += matrix[row][k] * linear[k];
            }
            result[row] = Math.min(1, Math.max(0, value));
        }
        return result;
    }

    private static double toLinear(double channel) {
        return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    // 선형 RGB → CIE Lab (D65)
    private static double[] toLab(double[] linear) {
        double x = (0.4124 * linear[0] + 0.3576 * linear[1] + 0.1805 * linear[2]) / 0.95047;
        double y = 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2];
        double z = (0.0193 * linear[0] + 0.1192 * linear[1] + 0.9505 * linear[2]) / 1.08883;
        double fx = labF(x);
        double fy = labF(y);
        double fz = labF(z);
        return new double[] {116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)};
    }

    private static double labF(double t) {
        return t > 216.0 / 24389 ? Math.cbrt(t) : (24389.0 / 27 * t + 16) / 116;
    }

    // CIEDE2000 색차 (Sharma, Wu, Dalal 2005)
    private static double deltaE2000(double[] lab1, double[] lab2) {
        double c1 = Math.hypot(lab1[1], lab1[2]);
        double c2 = Math.hypot(lab2[1], lab2[2]);
        double cBar = (c1 + c2) / 2;
        double g = 0.5 * (1 - Math.sqrt(Math.pow(cBar, 7) / (Math.pow(cBar, 7) + Math.pow(25, 7))));
        double a1 = (1 + g) * lab1[1];
        double a2 = (1 + g) * lab2[1];
        double c1p = Math.hypot(a1, lab1[2]);
        double c2p = Math.hypot(a2, lab2[2]);
        double h1p = (Math.toDegrees(Math.atan2(lab1[2], a1)) + 360) % 360;
        double h2p = (Math.toDegrees(Math.atan2(lab2[2], a2)) + 360) % 360;
        double deltaL = lab2[0] - lab1[0];
        double deltaC = c2p - c1p;
        double deltaH = 0;
        if (c1p * c2p != 0) {
            deltaH = h2p - h1p;
            if (deltaH > 180) {
                deltaH -= 360;
            } else if (deltaH < -180) {
                deltaH += 360;
            }
        }
        double deltaBigH = 2 * Math.sqrt(c1p * c2p) * Math.sin(Math.toRadians(deltaH / 2));
        double lBar = (lab1[0] + lab2[0]) / 2;
        double cBarP = (c1p + c2p) / 2;
        double hBarP = h1p + h2p;
        if (c1p * c2p != 0) {
            hBarP = Math.abs(h1p - h2p) <= 180 ? (h1p + h2p) / 2
                    : (h1p + h2p < 360 ? (h1p + h2p + 360) / 2 : (h1p + h2p - 360) / 2);
        }
        double t = 1 - 0.17 * Math.cos(Math.toRadians(hBarP - 30)) + 0.24 * Math.cos(Math.toRadians(2 * hBarP))
                + 0.32 * Math.cos(Math.toRadians(3 * hBarP + 6)) - 0.20 * Math.cos(Math.toRadians(4 * hBarP - 63));
        double deltaTheta = 30 * Math.exp(-Math.pow((hBarP - 275) / 25, 2));
        double rc = 2 * Math.sqrt(Math.pow(cBarP, 7) / (Math.pow(cBarP, 7) + Math.pow(25, 7)));
        double sl = 1 + 0.015 * Math.pow(lBar - 50, 2) / Math.sqrt(20 + Math.pow(lBar - 50, 2));
        double sc = 1 + 0.045 * cBarP;
        double sh = 1 + 0.015 * cBarP * t;
        double rt = -Math.sin(Math.toRadians(2 * deltaTheta)) * rc;
        return Math.sqrt(Math.pow(deltaL / sl, 2) + Math.pow(deltaC / sc, 2) + Math.pow(deltaBigH / sh, 2)
                + rt * (deltaC / sc) * (deltaBigH / sh));
    }

    // ---------- 도우미: 픽셀 ----------

    private static Color pixel(Canvas canvas, int x, int y) {
        WritableImage image = canvas.snapshot(null, null);
        return image.getPixelReader().getColor(x, y);
    }

    private static boolean containsColor(Canvas canvas, Color expected) {
        WritableImage image = canvas.snapshot(null, null);
        for (int y = 0; y < (int) image.getHeight(); y++) {
            for (int x = 0; x < (int) image.getWidth(); x++) {
                if (same(expected, image.getPixelReader().getColor(x, y))) {
                    return true;
                }
            }
        }
        return false;
    }

    // 테두리(먹선)를 피한 칸 안쪽에서 주어진 밝기보다 어두운 픽셀이 있는지
    private static boolean hasPixelDarkerThan(Canvas canvas, double brightness) {
        WritableImage image = canvas.snapshot(null, null);
        int margin = 5;
        for (int y = margin; y < (int) image.getHeight() - margin; y++) {
            for (int x = margin; x < (int) image.getWidth() - margin; x++) {
                if (image.getPixelReader().getColor(x, y).getBrightness() < brightness) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void assertColor(Color expected, Color actual) {
        assertTrue(same(expected, actual), "expected " + expected + " but was " + actual);
    }

    private static boolean same(Color expected, Color actual) {
        return Math.abs(actual.getRed() - expected.getRed()) < COLOR_TOLERANCE
                && Math.abs(actual.getGreen() - expected.getGreen()) < COLOR_TOLERANCE
                && Math.abs(actual.getBlue() - expected.getBlue()) < COLOR_TOLERANCE;
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
