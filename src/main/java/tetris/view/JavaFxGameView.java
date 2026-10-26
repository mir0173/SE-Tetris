package tetris.view;

import java.util.Map;
import java.util.function.Consumer;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBoundsType;
import javafx.stage.Stage;

import tetris.model.Board;
import tetris.model.GameCommand;
import tetris.model.GameSnapshot;
import tetris.model.GameStatus;
import tetris.model.Item;
import tetris.model.ItemType;
import tetris.model.Position;
import tetris.model.PieceType;
import tetris.settings.GameSettings;

public class JavaFxGameView implements GameView {

    private static final double VIEW_WIDTH = 500;
    private static final double VIEW_HEIGHT = 600;
    private static final double PREVIEW_SIZE = 100;
    private static final double SIDEBAR_WIDTH = 160;
    private static final double CONTENT_SPACING = 20;
    private static final double SIDEBAR_SPACING = 12;
    private static final double ROOT_PADDING = 20;
    private static final double BLOCK_FONT_SIZE = 20;
    private static final double SCORE_FONT_SIZE = 20;
    private static final double PAUSE_FONT_SIZE = 16;
    private static final double PAUSE_PADDING = 20;
    private static final int SPAWN_ROTATION = 0;

    private static final Color EMPTY_COLOR = Color.BLACK;
    private static final Color CLEARING_BACKGROUND_COLOR = Color.WHITE;
    private static final Color CLEARING_SYMBOL_COLOR = Color.BLACK;
    private static final Color GHOST_COLOR = Color.gray(0.7, 0.3);
    private static final String BORDER_SYMBOL = "X";
    private static final String BLOCK_SYMBOL = "O";

    private static final Map<PieceType, Color> NORMAL_COLORS = Map.ofEntries(
            Map.entry(PieceType.I, Color.web("#22D3EE")),
            Map.entry(PieceType.O, Color.web("#FACC15")),
            Map.entry(PieceType.T, Color.web("#A855F7")),
            Map.entry(PieceType.S, Color.web("#22C55E")),
            Map.entry(PieceType.Z, Color.web("#EF4444")),
            Map.entry(PieceType.J, Color.web("#3B82F6")),
            Map.entry(PieceType.L, Color.web("#F97316")),
            Map.entry(PieceType.SLOW, Color.web("#F472B6")),
            Map.entry(PieceType.HEAVY_WEIGHT, Color.web("#9CA3AF")),
            Map.entry(PieceType.SPECIAL_I, Color.web("#FFFFFF")),
            Map.entry(PieceType.SPECIAL_BLOCK, Color.web("#A3E635")));

    private static final Map<PieceType, Color> COLOR_BLIND_COLORS = Map.ofEntries(
            Map.entry(PieceType.I, Color.web("#56B4E9")),
            Map.entry(PieceType.O, Color.web("#F0E442")),
            Map.entry(PieceType.T, Color.web("#CC79A7")),
            Map.entry(PieceType.S, Color.web("#009E73")),
            Map.entry(PieceType.Z, Color.web("#D55E00")),
            Map.entry(PieceType.J, Color.web("#0072B2")),
            Map.entry(PieceType.L, Color.web("#E69F00")),
            Map.entry(PieceType.SLOW, Color.web("#F5C8E1")), // 색약 친화 팔레트 7색을 다 썼으므로
            Map.entry(PieceType.HEAVY_WEIGHT, Color.web("#8C8C8C")), // 밝기 찿이로 구분되도록 색 선택
            Map.entry(PieceType.SPECIAL_I, Color.web("#FFFFFF")), // 기호가 구분을 도와줌
            Map.entry(PieceType.SPECIAL_BLOCK, Color.web("#B8A6FF")));

    private final double scale;
    private final Font blockFont;
    private final double cellWidth;
    private final double cellHeight;
    private final double textBaselineOffset;

    private final Stage stage;
    private final Map<GameCommand, String> keyBindings;
    private final boolean colorBlindMode;

    private final Canvas boardCanvas = new Canvas();
    private final Canvas nextCanvas;
    private final Canvas holdCanvas;

    private final Label scoreLabel = new Label("점수 : 0");
    private final Label intervalLabel = new Label("낙하 간격 : 1000ms");
    private final Label pauseLabel = new Label();

    private Consumer<GameCommand> commandHandler;
    private boolean pauseKeyHeld;

    public JavaFxGameView(Stage stage, GameSettings settings) {
        this.stage = stage;
        this.keyBindings = Map.copyOf(settings.getKeyBindings());
        this.colorBlindMode = settings.isColorBlindMode();
        this.scale = settings.getWindowSize().getScale();
        this.blockFont = Font.font("Monospaced", FontWeight.BOLD, BLOCK_FONT_SIZE * scale);
        this.nextCanvas = new Canvas(PREVIEW_SIZE * scale, PREVIEW_SIZE * scale);
        this.holdCanvas = new Canvas(PREVIEW_SIZE * scale, PREVIEW_SIZE * scale);

        Text measure = new Text(BLOCK_SYMBOL);
        measure.setFont(blockFont);
        Text visibleMeasure = new Text(BORDER_SYMBOL + BLOCK_SYMBOL);
        visibleMeasure.setFont(blockFont);
        visibleMeasure.setBoundsType(TextBoundsType.VISUAL);

        Bounds logicalBounds = measure.getLayoutBounds();
        Bounds visualBounds = visibleMeasure.getLayoutBounds();
        double transferredPadding = Math.max(0, logicalBounds.getHeight() - visualBounds.getHeight()) / 2;
        this.cellWidth = logicalBounds.getWidth() + transferredPadding;
        this.cellHeight = logicalBounds.getHeight() - transferredPadding;
        this.textBaselineOffset = (cellHeight - visualBounds.getHeight()) / 2 - visualBounds.getMinY();
        initializeView();
    }

    @Override
    public void setCommandHandler(Consumer<GameCommand> commandHandler) {
        this.commandHandler = commandHandler;
    }

    @Override
    public void render(GameSnapshot snapshot) {
        drawBoard(snapshot.getCells(), snapshot.getClearingRows(), snapshot.getGhostPieceCells());

        PieceType nextPiece = snapshot.getNextPieces().isEmpty() ? null : snapshot.getNextPieces().get(0);
        drawPreview(nextCanvas, nextPiece, snapshot.getNextPieceItem());
        drawPreview(holdCanvas, snapshot.getHeldPiece(), snapshot.getHeldPieceItem());

        scoreLabel.setText("점수: " + snapshot.getScore());
        intervalLabel.setText("낙하 간격: " + snapshot.getDropInterval() + "ms");
        pauseLabel.setVisible(snapshot.getStatus() == GameStatus.PAUSED);
    }

    @Override
    public void show() {
        stage.show();
        stage.getScene().getRoot().requestFocus();
    }

    @Override
    public void close() {
        stage.close();
    }

    private void initializeView() {
        boardCanvas.setId("game-board");
        nextCanvas.setId("next-preview");
        holdCanvas.setId("hold-preview");
        scoreLabel.setId("score-label");
        intervalLabel.setId("interval-label");
        pauseLabel.setId("pause-label");
        scoreLabel.setFont(Font.font(SCORE_FONT_SIZE * scale));
        intervalLabel.setFont(Font.font(Font.getDefault().getSize() * scale));

        pauseLabel.setText("일시정지\n" + keyBindings.get(GameCommand.PAUSE) + ": 재개\n"
                + keyBindings.get(GameCommand.QUIT_GAME) + ": 메뉴");
        pauseLabel.setAlignment(Pos.CENTER);
        pauseLabel.setTextFill(Color.WHITE);
        pauseLabel.setFont(Font.font(PAUSE_FONT_SIZE * scale));
        pauseLabel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.85);" + "-fx-padding: " + (PAUSE_PADDING * scale) + ";");
        pauseLabel.setVisible(false);
        pauseLabel.setMouseTransparent(true);

        StackPane boardArea = new StackPane(boardCanvas, pauseLabel);
        VBox sidebar = new VBox(
                SIDEBAR_SPACING * scale,
                scoreLabel,
                intervalLabel,
                createInformationLabel("다음 블록"),
                nextCanvas,
                createInformationLabel("홀드"),
                holdCanvas,
                createKeyHelp());

        sidebar.setPrefWidth(SIDEBAR_WIDTH * scale);
        sidebar.setMinWidth(SIDEBAR_WIDTH * scale);

        HBox root = new HBox(CONTENT_SPACING * scale, boardArea, sidebar);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(ROOT_PADDING * scale));
        root.setFocusTraversable(true);

        Scene scene = new Scene(root, VIEW_WIDTH * scale, VIEW_HEIGHT * scale);
        scene.setOnKeyPressed(event -> handleKey(event.getCode()));
        scene.setOnKeyReleased(event -> {
            if (mapKey(event.getCode()) == GameCommand.PAUSE) {
                pauseKeyHeld = false;
            }
        });

        root.focusedProperty().addListener(
                (observable, wasFocused, focused) -> {
                    if (!focused) {
                        pauseKeyHeld = false;
                    }
                });

        stage.setTitle("SE Tetris");
        stage.setScene(scene);
        stage.sizeToScene();
    }

    private Label createKeyHelp() {
        Label help = createInformationLabel(
                keyBindings.get(GameCommand.HOLD) + ": 홀드\n" + keyBindings.get(GameCommand.PAUSE) + ": 일시정지 / 재개\n"
                        + keyBindings.get(GameCommand.QUIT_GAME) + ": 메뉴");
        help.setWrapText(true);
        return help;
    }

    private Label createInformationLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(Font.getDefault().getSize() * scale));
        return label;
    }

    private void drawBoard(int[][] cells, List<Integer> clearingRows, Map<Position, Integer> ghostPieceCells) {
        int rows = cells.length;
        int cols = rows == 0 ? 0 : cells[0].length;

        boardCanvas.setWidth((cols + 2) * cellWidth);
        boardCanvas.setHeight((rows + 2) * cellHeight);

        GraphicsContext graphics = boardCanvas.getGraphicsContext2D();
        graphics.setFill(EMPTY_COLOR);
        graphics.fillRect(0, 0, boardCanvas.getWidth(), boardCanvas.getHeight());

        for (int col = 0; col < cols + 2; col++) {
            drawSymbol(graphics, BORDER_SYMBOL, Color.WHITE, col * cellWidth, 0);
            drawSymbol(graphics, BORDER_SYMBOL, Color.WHITE, col * cellWidth, (rows + 1) * cellHeight);
        }

        for (int row = 0; row < rows; row++) {
            drawSymbol(graphics, BORDER_SYMBOL, Color.WHITE, 0, (row + 1) * cellHeight);
            drawSymbol(graphics, BORDER_SYMBOL, Color.WHITE, (cols + 1) * cellWidth, (row + 1) * cellHeight);

            for (int col = 0; col < cols; col++) {
                String symbol = findSymbol(cells[row][col]);
                PieceType type = findType(Board.extractCellId(cells[row][col]));
                double x = (col + 1) * cellWidth;
                double y = (row + 1) * cellHeight;
                Position pos = new Position(row, col);

                if (clearingRows.contains(row)) {
                    drawClearingCell(graphics, x, y, symbol, type);
                } else if (cells[row][col] == Board.EMPTY_CELL
                        && ghostPieceCells.containsKey(pos)) {
                    drawSymbol(graphics, findSymbol(ghostPieceCells.get(pos)), GHOST_COLOR, x, y);
                } else {
                    drawCell(graphics, x, y, symbol, type);
                }

            }
        }
    }

    private void drawPreview(Canvas canvas, PieceType type, Item item) {
        GraphicsContext graphics = canvas.getGraphicsContext2D();

        graphics.setFill(EMPTY_COLOR);
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (type == null) {
            return;
        }

        var positions = type.getCellsAt(SPAWN_ROTATION);

        int minRow = positions.stream().mapToInt(Position::row).min().orElse(0);
        int maxRow = positions.stream().mapToInt(Position::row).max().orElse(0);
        int minColumn = positions.stream().mapToInt(Position::column).min().orElse(0);
        int maxColumn = positions.stream().mapToInt(Position::column).max().orElse(0);

        double pieceWidth = (maxColumn - minColumn + 1) * cellWidth;
        double pieceHeight = (maxRow - minRow + 1) * cellHeight;
        double offsetX = (canvas.getWidth() - pieceWidth) / 2;
        double offsetY = (canvas.getHeight() - pieceHeight) / 2;

        for (int i = 0; i < positions.size(); i++) {
            double x = offsetX + (positions.get(i).column() - minColumn) * cellWidth;
            double y = offsetY + (positions.get(i).row() - minRow) * cellHeight;
            int cellValue = Board.calculateCellValue(type, item, i);
            String symbol = findSymbol(cellValue);
            drawCell(graphics, x, y, symbol, type);
        }
    }

    private void drawCell(GraphicsContext graphics, double x, double y, String symbol, PieceType type) {
        if (type == null) {
            return;
        }
        drawSymbol(graphics, symbol, getPieceColor(type), x, y);
    }

    private void drawClearingCell(GraphicsContext graphics, double x, double y, String symbol, PieceType type) {
        graphics.setFill(CLEARING_BACKGROUND_COLOR);
        graphics.fillRect(x, y, cellWidth, cellHeight);
        if (type != null) {
            drawSymbol(graphics, symbol, CLEARING_SYMBOL_COLOR, x, y);
        }
    }

    private void drawSymbol(GraphicsContext graphics, String symbol, Color color, double x, double y) {
        graphics.setFill(color);
        graphics.setFont(blockFont);
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setTextBaseline(VPos.BASELINE);
        graphics.fillText(symbol, x + cellWidth / 2, y + textBaselineOffset);
    }

    private Color getPieceColor(PieceType type) {
        return colorBlindMode ? COLOR_BLIND_COLORS.get(type) : NORMAL_COLORS.get(type);
    }

    private PieceType findType(int cellId) {
        if (cellId == Board.EMPTY_CELL) {
            return null;
        }

        for (PieceType type : PieceType.values()) {
            if (type.getCellId() == cellId) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown cell ID: " + cellId);
    }

    private void handleKey(KeyCode keyCode) {
        GameCommand command = mapKey(keyCode);

        if (command == GameCommand.NONE || commandHandler == null) {
            return;
        }

        if (command == GameCommand.PAUSE) {
            if (pauseKeyHeld) {
                return;
            }
            pauseKeyHeld = true;
        }

        commandHandler.accept(command);
    }

    private GameCommand mapKey(KeyCode keyCode) {
        for (Map.Entry<GameCommand, String> binding : keyBindings.entrySet()) {
            if (binding.getValue().equals(keyCode.name())) {
                return binding.getKey();
            }
        }

        return GameCommand.NONE;
    }

    private String findSymbol(int cellValue) {
        int itemCode = Board.extractItemCode(cellValue);
        if (itemCode != Board.NO_ITEM_CODE) {
            String symbol = ItemType.findSymbolByCode(itemCode);
            if (symbol != null) {
                return symbol;
            }
        }

        return BLOCK_SYMBOL;
    }
}