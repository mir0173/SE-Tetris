package tetris.view;

import java.util.Map;
import java.util.function.Consumer;

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
import tetris.model.Position;
import tetris.model.TetrominoType;
import tetris.settings.GameSettings;
public class JavaFxGameView implements GameView {

    private static final double VIEW_WIDTH = 500;
    private static final double VIEW_HEIGHT = 600;
    private static final double PREVIEW_SIZE = 100;
    private static final double SIDEBAR_WIDTH = 160;
    private static final double CONTENT_SPACING = 20;
    private static final double SIDEBAR_SPACING = 12;
    private static final double ROOT_PADDING = 20;
    private static final int SPAWN_ROTATION = 0;

    private static final Color EMPTY_COLOR = Color.BLACK;
    private static final String BORDER_SYMBOL = "X";
    private static final String BLOCK_SYMBOL = "O";
    private static final Font BLOCK_FONT = Font.font("Monospaced", FontWeight.BOLD, 20);

    private static final Map<TetrominoType, Color> NORMAL_COLORS = Map.of(
        TetrominoType.I, Color.web("#22D3EE"),
        TetrominoType.O, Color.web("#FACC15"),
        TetrominoType.T, Color.web("#A855F7"),
        TetrominoType.S, Color.web("#22C55E"),
        TetrominoType.Z, Color.web("#EF4444"),
        TetrominoType.J, Color.web("#3B82F6"),
        TetrominoType.L, Color.web("#F97316")
    );

    private static final Map<TetrominoType, Color> COLOR_BLIND_COLORS = Map.of(
        TetrominoType.I, Color.web("#56B4E9"),
        TetrominoType.O, Color.web("#F0E442"),
        TetrominoType.T, Color.web("#CC79A7"),
        TetrominoType.S, Color.web("#009E73"),
        TetrominoType.Z, Color.web("#D55E00"),
        TetrominoType.J, Color.web("#0072B2"),
        TetrominoType.L, Color.web("#E69F00")
    );

    private final double cellWidth;
    private final double cellHeight;
    private final double textBaselineOffset;

    private final Stage stage;
    private final Map<GameCommand, String> keyBindings;
    private final boolean colorBlindMode;

    private final Canvas boardCanvas = new Canvas();
    private final Canvas nextCanvas = new Canvas(PREVIEW_SIZE, PREVIEW_SIZE);
    private final Canvas holdCanvas = new Canvas(PREVIEW_SIZE, PREVIEW_SIZE);

    private final Label scoreLabel = new Label("점수 : 0");
    private final Label intervalLabel = new Label("낙하 간격 : 1000ms");
    private final Label pauseLabel = new Label();

    private Consumer<GameCommand> commandHandler;
    private boolean pauseKeyHeld;

    public JavaFxGameView(Stage stage, GameSettings settings) {
        this.stage = stage;
        this.keyBindings = Map.copyOf(settings.getKeyBindings());
        this.colorBlindMode = settings.isColorBlindMode();
        Text measure = new Text(BLOCK_SYMBOL);
        measure.setFont(BLOCK_FONT);
        Text visibleMeasure = new Text(BORDER_SYMBOL + BLOCK_SYMBOL);
        visibleMeasure.setFont(BLOCK_FONT);
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
        drawBoard(snapshot.getCells());

        TetrominoType nextPiece = snapshot.getNextPieces().isEmpty() ? null : snapshot.getNextPieces().get(0);
        
        drawPreview(nextCanvas, nextPiece);
        drawPreview(holdCanvas, snapshot.getHeldPiece());

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
        scoreLabel.setFont(Font.font(20));

        pauseLabel.setText("일시정지\n" + keyBindings.get(GameCommand.PAUSE) + ": 재개\n" + keyBindings.get(GameCommand.QUIT_GAME) + ": 메뉴");
        pauseLabel.setAlignment(Pos.CENTER);
        pauseLabel.setTextFill(Color.WHITE);
        pauseLabel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);" + "-fx-padding: 20;" + "-fx-font-size: 16;");
        pauseLabel.setVisible(false);
        pauseLabel.setMouseTransparent(true);

        StackPane boardArea = new StackPane(boardCanvas, pauseLabel);
        VBox sidebar = new VBox(
            SIDEBAR_SPACING,
            scoreLabel,
            intervalLabel,
            new Label("다음 블록"),
            nextCanvas,
            new Label("홀드"),
            holdCanvas,
            createKeyHelp());

        sidebar.setPrefWidth(SIDEBAR_WIDTH);
        sidebar.setMinWidth(SIDEBAR_WIDTH);

        HBox root = new HBox(CONTENT_SPACING, boardArea, sidebar);

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(ROOT_PADDING));
        root.setFocusTraversable(true);

        Scene scene = new Scene(root, VIEW_WIDTH, VIEW_HEIGHT);
        scene.setOnKeyPressed(event -> handleKey(event.getCode()));

        scene.setOnKeyReleased(event -> {
            if (mapKey(event.getCode()) == GameCommand.PAUSE) {
                pauseKeyHeld = false;
            }
        });

        root.focusedProperty().addListener((observable, wasFocused, focused) -> {
            if (!focused) {
                pauseKeyHeld = false;
            }
        });

        stage.setTitle("SE Tetris");
        stage.setScene(scene);
    }

    private Label createKeyHelp() {
        Label help = new Label(keyBindings.get(GameCommand.HOLD) + ": 홀드\n" + keyBindings.get(GameCommand.PAUSE) + ": 일시정지 / 재개\n" + keyBindings.get(GameCommand.QUIT_GAME) + ": 메뉴");
        help.setWrapText(true);
        return help;
    }

    private void drawBoard(int[][] cells) {
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
                TetrominoType type = findType(cells[row][col]);
                drawCell(graphics, (col + 1) * cellWidth, (row + 1) * cellHeight, type);
            }
        }
    }

    private void drawPreview(Canvas canvas, TetrominoType type) {
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

        for (Position position : positions) {
            double x = offsetX + (position.column() - minColumn) * cellWidth;
            double y = offsetY + (position.row() - minRow) * cellHeight;

            drawCell(graphics, x, y, type);
        }
    }

    private void drawCell(GraphicsContext graphics, double x, double y, TetrominoType type) {
        if (type == null) {
            return;
        }

        drawSymbol(graphics, BLOCK_SYMBOL, getPieceColor(type), x, y);
    }

    private void drawSymbol(GraphicsContext graphics, String symbol, Color color, double x, double y) {
        graphics.setFill(color);
        graphics.setFont(BLOCK_FONT);
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setTextBaseline(VPos.BASELINE);
        graphics.fillText(symbol, x + cellWidth / 2, y + textBaselineOffset);
    }

    private Color getPieceColor(TetrominoType type) {
        return colorBlindMode ? COLOR_BLIND_COLORS.get(type) : NORMAL_COLORS.get(type);
    }

    private TetrominoType findType(int cellId) {
        if (cellId == Board.EMPTY_CELL) {
            return null;
        }

        for (TetrominoType type : TetrominoType.values()) {
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
}