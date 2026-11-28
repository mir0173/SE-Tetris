package tetris.view;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import tetris.model.PieceType;

// AI-assisted code start
/**
 * 블루프린트 테마의 블록 칸 그리기. 모든 칸은 "색 면 + 먹선 테두리 + 글자"로 그린다(텍스트 기반 게임).
 * <ul>
 *   <li>일반 블록: 블록 색 + 흐린 O. 색맹 모드에서는 색각이상 3종에서도 구분되는 색 + 블록마다 다른 모양 글자</li>
 *   <li>아이템 칸: 흰 칸 안에 빨간 아이템 글자 (어떤 블록 색 위에서도 보이도록)</li>
 *   <li>넘어온 줄: 회색 빗금 (직접 쌓은 블록과 무늬로도 구분)</li>
 *   <li>고스트: 먹 점선 테두리</li>
 *   <li>줄 삭제 연출: 파란 펜 색으로 줄 전체를 칠하고 흰 O를 찍음</li>
 * </ul>
 * 색 값과 검증 근거는 docs/redesign-proposal.md 8절.
 */
public final class BlockRenderer {

    public static final Color BOARD_BACKGROUND = Color.web("#fbfdff");
    public static final Color GRID_LINE = Color.web("#dae6f3");
    public static final Color INK = Color.web("#14243d");
    public static final Color GARBAGE_FILL = Color.web("#c3cddb");
    public static final Color GARBAGE_HATCH = Color.web("#7d8ea3");
    public static final Color CLEARING_FILL = Color.web("#1f5fd6");
    public static final Color CLEARING_GLYPH = Color.WHITE;
    public static final Color ITEM_BACKGROUND = Color.WHITE;
    public static final Color ITEM_GLYPH = Color.web("#e0442c");
    public static final Color GHOST_STROKE = Color.web("#14243d", 0.55);

    /** 일반 모드 블록 색 (정상 시각에서 가장 비슷한 두 블록의 색차 20.7) */
    public static final Map<PieceType, Color> NORMAL_COLORS = createColors(
            "#74d3ec", "#f6d453", "#c98af2", "#7fd69a", "#ef6f6f", "#4f7be3", "#f7a04f",
            "#f49ac1", "#5b6b82", "#a7c957", "#e8eef6");

    /** 색맹 모드 블록 색 (적색맹·녹색맹·청황색맹 모두 가장 비슷한 두 블록의 색차 19.6 이상) */
    public static final Map<PieceType, Color> COLOR_BLIND_COLORS = createColors(
            "#82dfff", "#fde24b", "#c859ff", "#129e78", "#873e36", "#1f4299", "#de5800",
            "#f7b6d2", "#3b3b3b", "#9a8f86", "#ffffff");

    /** 색맹 모드에서 블록마다 다른 모양 글자. 색 구분이 어려워도 글자로 구분한다 */
    public static final Map<PieceType, String> COLOR_BLIND_GLYPHS = Map.ofEntries(
            Map.entry(PieceType.I, "▮"), Map.entry(PieceType.O, "■"), Map.entry(PieceType.T, "▲"),
            Map.entry(PieceType.S, "◆"), Map.entry(PieceType.Z, "●"), Map.entry(PieceType.J, "◀"),
            Map.entry(PieceType.L, "▶"), Map.entry(PieceType.SLOW, "~"), Map.entry(PieceType.HEAVY_WEIGHT, "▼"),
            Map.entry(PieceType.SPECIAL_BLOCK, "✚"), Map.entry(PieceType.SPECIAL_I, "☆"));

    public static final String BLOCK_GLYPH = "O";

    /** 뿌요 4색 (빨강, 초록, 파랑, 노랑 순) */
    public static final List<Color> PUYO_COLORS = List.of(
            NORMAL_COLORS.get(PieceType.Z), NORMAL_COLORS.get(PieceType.S),
            NORMAL_COLORS.get(PieceType.J), NORMAL_COLORS.get(PieceType.O));
    public static final List<Color> PUYO_COLOR_BLIND_COLORS = List.of(
            COLOR_BLIND_COLORS.get(PieceType.Z), COLOR_BLIND_COLORS.get(PieceType.S),
            COLOR_BLIND_COLORS.get(PieceType.J), COLOR_BLIND_COLORS.get(PieceType.O));
    public static final List<String> PUYO_COLOR_BLIND_GLYPHS = List.of("A", "B", "C", "D");

    private static final double DARK_LUMINANCE = 0.25;
    private static final double NORMAL_GLYPH_OPACITY = 0.35;
    private static final double DARK_NORMAL_GLYPH_OPACITY = 0.5;
    private static final double COLOR_BLIND_GLYPH_OPACITY = 0.9;
    private static final double GLYPH_SIZE_RATIO = 0.5;
    private static final double ITEM_GLYPH_SIZE_RATIO = 0.56;
    private static final double ITEM_INSET_RATIO = 0.18;
    private static final double HATCH_STEP_RATIO = 0.18;
    private static final String SYMBOL_FONT_FAMILY = "Monospaced";

    private final boolean colorBlindMode;

    public BlockRenderer(boolean colorBlindMode) {
        this.colorBlindMode = colorBlindMode;
    }

    /** 이 모드에서 블록 종류의 색 */
    public Color colorOf(PieceType type) {
        return (colorBlindMode ? COLOR_BLIND_COLORS : NORMAL_COLORS).get(type);
    }

    /** 보드 바탕(밝은 종이)과 칸 격자를 그린다 */
    public void drawBoardBackground(GraphicsContext graphics, double width, double height, double cell) {
        graphics.setFill(BOARD_BACKGROUND);
        graphics.fillRect(0, 0, width, height);
        graphics.setStroke(GRID_LINE);
        graphics.setLineWidth(1);
        for (double x = cell; x < width - 0.5; x += cell) {
            double line = Math.round(x) + 0.5;
            graphics.strokeLine(line, 0, line, height);
        }
        for (double y = cell; y < height - 0.5; y += cell) {
            double line = Math.round(y) + 0.5;
            graphics.strokeLine(0, line, width, line);
        }
    }

    /**
     * 블록 한 칸을 그린다.
     *
     * @param itemSymbol 아이템 글자. 아이템이 아니면 null
     */
    public void drawBlock(GraphicsContext graphics, double x, double y, double cell, PieceType type,
            String itemSymbol) {
        Color color = colorOf(type);
        fillTile(graphics, x, y, cell, color);

        if (itemSymbol != null) {
            double inset = Math.round(cell * ITEM_INSET_RATIO);
            graphics.setFill(ITEM_BACKGROUND);
            graphics.fillRect(x + inset, y + inset, cell - 2 * inset, cell - 2 * inset);
            graphics.setStroke(INK);
            graphics.setLineWidth(1);
            graphics.strokeRect(x + inset + 0.5, y + inset + 0.5, cell - 2 * inset - 1, cell - 2 * inset - 1);
            drawGlyph(graphics, itemSymbol, ITEM_GLYPH, x, y, cell, ITEM_GLYPH_SIZE_RATIO, FontWeight.BOLD);
            return;
        }

        boolean dark = luminance(color) < DARK_LUMINANCE;
        String glyph = colorBlindMode ? COLOR_BLIND_GLYPHS.getOrDefault(type, BLOCK_GLYPH) : BLOCK_GLYPH;
        double opacity = colorBlindMode ? COLOR_BLIND_GLYPH_OPACITY
                : dark ? DARK_NORMAL_GLYPH_OPACITY : NORMAL_GLYPH_OPACITY;
        Color glyphColor = dark ? Color.color(1, 1, 1, opacity) : Color.color(INK.getRed(), INK.getGreen(),
                INK.getBlue(), opacity);
        drawGlyph(graphics, glyph, glyphColor, x, y, cell, GLYPH_SIZE_RATIO, FontWeight.BOLD);
    }

    /** 상대가 보낸 줄의 한 칸: 회색 면 + 빗금 + 먹선 */
    public void drawGarbage(GraphicsContext graphics, double x, double y, double cell) {
        graphics.setFill(GARBAGE_FILL);
        graphics.fillRect(x + 1, y + 1, cell - 2, cell - 2);
        graphics.save();
        graphics.beginPath();
        graphics.rect(x + 1, y + 1, cell - 2, cell - 2);
        graphics.clip();
        graphics.setStroke(GARBAGE_HATCH);
        graphics.setLineWidth(1);
        double step = Math.max(3, cell * HATCH_STEP_RATIO);
        for (double k = -cell; k < cell; k += step) {
            graphics.strokeLine(x + k, y + cell, x + k + cell, y);
        }
        graphics.restore();
        strokeTile(graphics, x, y, cell);
    }

    /** 블록이 떨어질 자리: 먹 점선 테두리 */
    public void drawGhost(GraphicsContext graphics, double x, double y, double cell) {
        double dash = Math.max(2, cell / 8);
        graphics.setStroke(GHOST_STROKE);
        graphics.setLineWidth(Math.max(1, cell / 20));
        graphics.setLineDashes(dash, dash * 0.7);
        graphics.strokeRect(x + 2.5, y + 2.5, cell - 5, cell - 5);
        graphics.setLineDashes((double[]) null);
    }

    /** 지워질 줄의 한 칸: 파란 면 + 흰 O (빈 칸도 같이 칠해 줄 전체가 보이게 한다) */
    public void drawClearing(GraphicsContext graphics, double x, double y, double cell) {
        graphics.setFill(CLEARING_FILL);
        graphics.fillRect(x, y, cell, cell);
        drawGlyph(graphics, BLOCK_GLYPH, CLEARING_GLYPH, x, y, cell, GLYPH_SIZE_RATIO, FontWeight.BOLD);
    }

    /**
     * 뿌요 한 알을 그린다.
     *
     * @param colorIndex 0~3 색 번호. 방해 뿌요면 -1
     * @param clearing   터지는 중인지
     */
    public void drawPuyo(GraphicsContext graphics, double x, double y, double cell, int colorIndex, boolean clearing) {
        boolean garbage = colorIndex < 0;
        Color fill = clearing ? CLEARING_FILL : garbage ? GARBAGE_FILL
                : (colorBlindMode ? PUYO_COLOR_BLIND_COLORS : PUYO_COLORS).get(colorIndex);
        double inset = Math.max(1.5, cell * 0.08);
        graphics.setFill(fill);
        graphics.fillOval(x + inset, y + inset, cell - 2 * inset, cell - 2 * inset);
        graphics.setStroke(INK);
        graphics.setLineWidth(Math.max(1, cell / 24));
        graphics.strokeOval(x + inset, y + inset, cell - 2 * inset, cell - 2 * inset);

        String glyph = garbage ? "#" : colorBlindMode ? PUYO_COLOR_BLIND_GLYPHS.get(colorIndex) : BLOCK_GLYPH;
        Color glyphColor = clearing || luminance(fill) < DARK_LUMINANCE ? Color.WHITE : INK;
        double opacity = clearing || colorBlindMode || garbage ? 1 : NORMAL_GLYPH_OPACITY;
        drawGlyph(graphics, glyph, Color.color(glyphColor.getRed(), glyphColor.getGreen(), glyphColor.getBlue(),
                opacity), x, y, cell, GLYPH_SIZE_RATIO * 0.9, FontWeight.BOLD);
    }

    private void fillTile(GraphicsContext graphics, double x, double y, double cell, Color color) {
        graphics.setFill(color);
        graphics.fillRect(x + 1, y + 1, cell - 2, cell - 2);
        strokeTile(graphics, x, y, cell);
    }

    private void strokeTile(GraphicsContext graphics, double x, double y, double cell) {
        double lineWidth = Math.max(1, cell / 20);
        graphics.setStroke(INK);
        graphics.setLineWidth(lineWidth);
        graphics.setLineCap(StrokeLineCap.SQUARE);
        graphics.strokeRect(x + 1 + lineWidth / 2, y + 1 + lineWidth / 2, cell - 2 - lineWidth, cell - 2 - lineWidth);
    }

    private static void drawGlyph(GraphicsContext graphics, String glyph, Color color, double x, double y,
            double cell, double sizeRatio, FontWeight weight) {
        graphics.setFill(color);
        graphics.setFont(glyphFont(glyph, weight, cell * sizeRatio));
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setTextBaseline(VPos.CENTER);
        graphics.fillText(glyph, x + cell / 2, y + cell / 2 + 1);
    }

    /** 영문·숫자는 테마 고정폭 글꼴, 기호(▣ ★ ▲ 등)는 빠진 글자를 다른 글꼴에서 찾아 주는 Monospaced 글꼴 */
    static Font glyphFont(String glyph, FontWeight weight, double size) {
        boolean ascii = glyph.chars().allMatch(ch -> ch < 128);
        return ascii ? Theme.monoFont(weight, size) : Font.font(SYMBOL_FONT_FAMILY, weight, size);
    }

    private static double luminance(Color color) {
        return 0.2126 * linear(color.getRed()) + 0.7152 * linear(color.getGreen()) + 0.0722 * linear(color.getBlue());
    }

    private static double linear(double channel) {
        return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    private static Map<PieceType, Color> createColors(String i, String o, String t, String s, String z, String j,
            String l, String slow, String heavy, String specialBlock, String specialI) {
        Map<PieceType, Color> colors = new EnumMap<>(PieceType.class);
        colors.put(PieceType.I, Color.web(i));
        colors.put(PieceType.O, Color.web(o));
        colors.put(PieceType.T, Color.web(t));
        colors.put(PieceType.S, Color.web(s));
        colors.put(PieceType.Z, Color.web(z));
        colors.put(PieceType.J, Color.web(j));
        colors.put(PieceType.L, Color.web(l));
        colors.put(PieceType.SLOW, Color.web(slow));
        colors.put(PieceType.HEAVY_WEIGHT, Color.web(heavy));
        colors.put(PieceType.SPECIAL_BLOCK, Color.web(specialBlock));
        colors.put(PieceType.SPECIAL_I, Color.web(specialI));
        return Map.copyOf(colors);
    }
}
// AI-assisted code end
