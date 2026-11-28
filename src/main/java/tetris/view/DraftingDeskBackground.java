package tetris.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;

// AI-assisted code start
/**
 * 화면 뒤에 흐리게 깔리는 "제도 책상" 장식(자, 연필, 컴퍼스, 삼각자, 각도기, 지우개).
 * 게임 내용 뒤에 있으므로 빈 공간에서만 보이고, 움직이지 않으므로 크기가 바뀔 때만 다시 그린다.
 * 마우스 입력은 받지 않는다.
 */
public final class DraftingDeskBackground extends Pane {

    /** 도구 배치. 화면마다 다른 배치를 써서 같은 그림이 반복되지 않게 한다 */
    public enum Layout {
        /** 왼쪽 세로 자, 오른쪽 위 연필, 왼쪽 아래 각도기, 오른쪽 아래 삼각자 */
        EDGES,
        /** 왼쪽 컴퍼스와 그리던 원호, 왼쪽 위 지우개, 아래 가로 자, 오른쪽 세로 연필 */
        COMPASS
    }

    public static final String ID = "drafting-desk";
    static final double OPACITY = 0.22;

    private static final double DESATURATE = -0.6;
    private static final Color TOOL_INK = Color.web("#14243d", 0.75);
    private static final Color ACRYLIC = Color.web("#cde1f6", 0.92);
    private static final Color PEN_BLUE = Color.web("#1f5fd6", 0.8);

    private final Canvas canvas = new Canvas();
    private final Layout layout;
    private final boolean mirrored;

    /**
     * @param layout   도구 배치
     * @param mirrored true면 좌우를 뒤집어 그린다
     */
    public DraftingDeskBackground(Layout layout, boolean mirrored) {
        this.layout = layout;
        this.mirrored = mirrored;
        setId(ID);
        setMouseTransparent(true);
        setOpacity(OPACITY);
        setEffect(new ColorAdjust(0, DESATURATE, 0, 0));
        getChildren().add(canvas);
        widthProperty().addListener((observable, before, after) -> redraw());
        heightProperty().addListener((observable, before, after) -> redraw());
    }

    @Override
    protected void layoutChildren() {
        // 캔버스는 이 Pane 크기를 그대로 쓴다 (redraw에서 맞춤)
    }

    private void redraw() {
        double width = getWidth();
        double height = getHeight();
        canvas.setWidth(width);
        canvas.setHeight(height);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, width, height);
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.save();
        if (mirrored) {
            graphics.translate(width, 0);
            graphics.scale(-1, 1);
        }
        if (layout == Layout.EDGES) {
            place(graphics, 18, height * 0.16, 90, () -> ruler(graphics, height * 0.56));
            place(graphics, width - 64, 66, -45, () -> pencil(graphics, 170));
            place(graphics, 44, height - 36, 0, () -> protractor(graphics, 58));
            place(graphics, width - 4, height - 36, -90, () -> setSquare(graphics, 120));
        } else {
            place(graphics, 30, height * 0.28, 6, () -> compass(graphics, 96));
            place(graphics, 34, 34, 20, () -> eraser(graphics));
            place(graphics, width * 0.2, height - 62, -2.3, () -> ruler(graphics, width * 0.75));
            place(graphics, width - 22, height * 0.62, -97, () -> pencil(graphics, 180));
        }
        graphics.restore();
    }

    private static void place(GraphicsContext graphics, double x, double y, double degrees, Runnable draw) {
        graphics.save();
        graphics.translate(x, y);
        graphics.rotate(degrees);
        graphics.setStroke(TOOL_INK);
        graphics.setLineWidth(1.2);
        draw.run();
        graphics.restore();
    }

    private static void ruler(GraphicsContext graphics, double length) {
        double width = 24;
        graphics.setFill(ACRYLIC);
        graphics.fillRect(0, 0, length, width);
        graphics.strokeRect(0.5, 0.5, length - 1, width - 1);
        graphics.setLineWidth(1);
        int index = 0;
        for (double x = 6; x < length - 4; x += 5, index++) {
            double tick = index % 10 == 0 ? width * 0.55 : index % 5 == 0 ? width * 0.38 : width * 0.22;
            graphics.strokeLine(Math.round(x) + 0.5, 0, Math.round(x) + 0.5, tick);
        }
    }

    private static void pencil(GraphicsContext graphics, double length) {
        double radius = 6;
        double tip = 24;
        double ferrule = 26;
        graphics.setFill(Color.web("#ecd2a6"));
        graphics.fillPolygon(new double[] {0, tip, tip}, new double[] {0, -radius, radius}, 3);
        graphics.setFill(Color.web("#14243d"));
        double lead = radius * 7 / tip;
        graphics.fillPolygon(new double[] {0, 7, 7}, new double[] {0, -lead, lead}, 3);
        graphics.setFill(Color.web("#f2c94c"));
        graphics.fillRect(tip, -radius, length - tip - ferrule, 2 * radius);
        graphics.setFill(Color.web("#b9c2cc"));
        graphics.fillRect(length - ferrule, -radius, 14, 2 * radius);
        graphics.setFill(Color.web("#f29aa5"));
        graphics.fillRect(length - 12, -radius, 12, 2 * radius);
        graphics.strokePolygon(new double[] {0, tip, length, length, tip},
                new double[] {0, -radius, -radius, radius, radius}, 5);
        graphics.setLineWidth(0.8);
        graphics.strokeLine(tip, -radius / 3, length - ferrule, -radius / 3);
        graphics.strokeLine(tip, radius / 3, length - ferrule, radius / 3);
        for (double x : new double[] {tip, length - ferrule, length - ferrule + 5, length - ferrule + 9, length - 12}) {
            graphics.strokeLine(x, -radius, x, radius);
        }
    }

    private static void compass(GraphicsContext graphics, double size) {
        double spread = size * 0.36;
        graphics.save();
        graphics.setStroke(PEN_BLUE);
        graphics.strokeArc(-spread - 2 * spread, size - 2 * spread, 4 * spread, 4 * spread, -57, 74, ArcType.OPEN);
        graphics.restore();
        leg(graphics, -spread, size - 6);
        leg(graphics, spread, size - 14);
        graphics.setLineWidth(1.6);
        graphics.strokeLine(-spread, size - 6, -spread, size);
        graphics.save();
        graphics.setStroke(Color.web("#e0b93a"));
        graphics.setLineWidth(4);
        graphics.strokeLine(spread, size - 14, spread, size - 3);
        graphics.restore();
        graphics.setLineWidth(1.2);
        graphics.setFill(Color.web("#14243d"));
        graphics.fillPolygon(new double[] {spread - 2, spread + 2, spread}, new double[] {size - 3, size - 3, size}, 3);
        graphics.setFill(Color.web("#cfd8e3"));
        graphics.fillRect(-3, -20, 6, 13);
        graphics.strokeRect(-3, -20, 6, 13);
        graphics.setFill(Color.web("#e3e9f0"));
        graphics.fillOval(-7, -7, 14, 14);
        graphics.strokeOval(-7, -7, 14, 14);
        graphics.setFill(Color.web("#14243d"));
        graphics.fillOval(-1.8, -1.8, 3.6, 3.6);
    }

    private static void leg(GraphicsContext graphics, double endX, double endY) {
        double[] xs = {-3, 3, endX + 1.2, endX - 1.2};
        double[] ys = {0, 0, endY, endY};
        graphics.setFill(Color.web("#aab6c4"));
        graphics.fillPolygon(xs, ys, 4);
        graphics.strokePolygon(xs, ys, 4);
    }

    private static void setSquare(GraphicsContext graphics, double size) {
        double height = size;
        double base = size * 0.72;
        double inner = size * 0.17;
        graphics.beginPath();
        graphics.moveTo(0, 0);
        graphics.lineTo(0, -height);
        graphics.lineTo(base, 0);
        graphics.closePath();
        graphics.moveTo(inner, -inner);
        graphics.lineTo(base - inner * 2.3, -inner);
        graphics.lineTo(inner, -height + inner * 2.6);
        graphics.closePath();
        graphics.setFillRule(FillRule.EVEN_ODD);
        graphics.setFill(Color.web("#1f5fd6", 0.10));
        graphics.fill();
        graphics.stroke();
        graphics.setLineWidth(1);
        int index = 0;
        for (double x = 6; x < base - 8; x += 5, index++) {
            graphics.strokeLine(x, 0, x, index % 5 == 0 ? -7 : -4);
        }
    }

    private static void protractor(GraphicsContext graphics, double radius) {
        graphics.setFill(ACRYLIC);
        graphics.fillArc(-radius, -radius, 2 * radius, 2 * radius, 0, 180, ArcType.CHORD);
        graphics.strokeArc(-radius, -radius, 2 * radius, 2 * radius, 0, 180, ArcType.CHORD);
        double inner = radius * 0.55;
        graphics.strokeArc(-inner, -inner, 2 * inner, 2 * inner, 0, 180, ArcType.OPEN);
        graphics.setLineWidth(1);
        for (int degree = 0; degree <= 180; degree += 5) {
            double angle = Math.toRadians(180 + degree);
            double tick = degree % 30 == 0 ? 10 : degree % 10 == 0 ? 7 : 4;
            graphics.strokeLine(Math.cos(angle) * radius, Math.sin(angle) * radius,
                    Math.cos(angle) * (radius - tick), Math.sin(angle) * (radius - tick));
        }
        graphics.strokeLine(-5, 0, 5, 0);
        graphics.strokeLine(0, -5, 0, 0);
    }

    private static void eraser(GraphicsContext graphics) {
        graphics.setFill(Color.WHITE);
        graphics.fillRect(-20, -9, 40, 18);
        graphics.setFill(Color.web("#9fbde6"));
        graphics.fillRect(-20, -9, 15, 18);
        graphics.strokeRect(-20, -9, 40, 18);
        graphics.strokeLine(-5, -9, -5, 9);
    }
}
// AI-assisted code end
