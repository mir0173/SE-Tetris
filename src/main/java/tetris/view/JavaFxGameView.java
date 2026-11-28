package tetris.view;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import tetris.model.Board;
import tetris.model.GameCommand;
import tetris.model.GameSnapshot;
import tetris.model.GameStatus;
import tetris.model.Item;
import tetris.model.ItemType;
import tetris.model.PieceType;
import tetris.model.Position;
import tetris.model.PuyoDisplay;
import tetris.model.PuyoDisplay.PairColors;
import tetris.settings.GameSettings;

/**
 * 게임 화면. 보드 + 정보 칸(NEXT, HOLD, SCORE, SPEED, 넘어온 줄)을 그린다.
 * 1인 게임은 창 전체(Scene)를 만들고, 대전 화면은 {@link #createEmbedded}로 부품만 받아 쓴다.
 */
public class JavaFxGameView implements GameView {

    // AI-assisted code start (redesign) 블루프린트 테마 배치 값. 테스트가 배율 적용을 확인하도록 패키지 공개
    static final double VIEW_WIDTH = 540;
    static final double VIEW_HEIGHT = 600;
    static final double CELL_SIZE = 25;
    static final double PREVIEW_WIDTH = 96;
    static final double PREVIEW_HEIGHT = 64;
    static final double GARBAGE_PREVIEW_SIZE = 80;
    static final double SCORE_FONT_SIZE = 20;

    private static final int BOARD_HEIGHT_CELLS = 20; // 보드 높이는 테트리스 20줄 기준으로 고정 (뿌요는 칸이 커짐)
    private static final double SIDEBAR_WIDTH = 112;
    private static final double CONTENT_SPACING = 14;
    private static final double SIDEBAR_SPACING = 8;
    private static final double ROOT_PADDING = 16;
    private static final double PANEL_TITLE_FONT_SIZE = 10;
    private static final double VALUE_FONT_SIZE = 13;
    private static final double NOTE_FONT_SIZE = 11;
    private static final double PAUSE_FONT_SIZE = 15;
    private static final double PAUSE_BLUR_RADIUS = 4;
    private static final double PREVIEW_CELL_RATIO = 0.6;
    private static final int SPAWN_ROTATION = 0;
    private static final int GARBAGE_PREVIEW_ROWS = 10; // 넘어온 줄은 최대 10줄까지 대기한다
    private static final String PANEL_STYLE_CLASS = "game-panel";
    private static final String PANEL_TITLE_STYLE_CLASS = "game-panel-title";
    private static final String VALUE_STYLE_CLASS = "game-value";
    private static final String NOTE_STYLE_CLASS = "game-note";
    // AI-assisted code end

    private final double scale;
    private final Stage stage;
    private final Map<GameCommand, String> keyBindings;
    private final BlockRenderer renderer; // AI-assisted (redesign)

    private final Canvas boardCanvas = new Canvas();
    private final Canvas nextCanvas;
    private final Canvas holdCanvas;
    private final Canvas garbageCanvas; // AI-assisted (T17)

    private final Label chainLabel = new Label();
    private final Label scoreLabel = new Label("000000");
    private final Label intervalLabel = new Label("1000ms");
    private final Label pauseLabel = new Label();
    private final Label garbageCountLabel = new Label("넘어온 줄: 0"); // AI-assisted (T17)
    private final Region pauseShade = new Region(); // AI-assisted (redesign) 일시정지 때 보드를 덮는 반투명 종이
    private final KeyHintBar keyHints = new KeyHintBar(); // AI-assisted (redesign) 1인 게임 화면 아래 키 안내

    private VBox holdPanel;
    private VBox chainPanel;
    private Consumer<GameCommand> commandHandler;
    private boolean pauseKeyHeld;
    private boolean puyoKeysApplied;
    private Parent content;
    private Label keyHelpLabel;

    public JavaFxGameView(Stage stage, GameSettings settings) {
        this(stage, settings, true);
    }

    private JavaFxGameView(Stage stage, GameSettings settings, boolean attachToStage) {
        this.stage = stage;
        this.keyBindings = Map.copyOf(settings.getKeyBindings());
        this.scale = settings.getWindowSize().getScale();
        this.renderer = new BlockRenderer(settings.isColorBlindMode());
        this.nextCanvas = new Canvas(PREVIEW_WIDTH * scale, PREVIEW_HEIGHT * scale);
        this.holdCanvas = new Canvas(PREVIEW_WIDTH * scale, PREVIEW_HEIGHT * scale);
        this.garbageCanvas = new Canvas(GARBAGE_PREVIEW_SIZE * scale, GARBAGE_PREVIEW_SIZE * scale); // AI-assisted
        initializeView(attachToStage);
    }

    /** 기존 문자 렌더러를 창 전체가 아닌 대전 화면의 한 패널로 생성한다. */
    public static JavaFxGameView createEmbedded(GameSettings settings) {
        return new JavaFxGameView(null, settings, false);
    }

    public Parent getContent() {
        return content;
    }

    public void setKeyHelpText(String text) {
        keyHelpLabel.setText(text);
    }

    // AI-assisted code start
    /** 키 안내의 보임 여부와 배치 점유 여부를 함께 바꾼다. */
    public void setKeyHelpVisible(boolean visible) {
        keyHelpLabel.setVisible(visible);
        keyHelpLabel.setManaged(visible);
    }
    // AI-assisted code end

    @Override
    public void setCommandHandler(Consumer<GameCommand> commandHandler) {
        this.commandHandler = commandHandler;
    }

    @Override
    public void render(GameSnapshot snapshot) {
        PuyoDisplay puyo = snapshot.getPuyo();
        drawBoard(snapshot.getCells(), snapshot.getClearingRows(), snapshot.getGhostPieceCells(), puyo);

        if (puyo == null) {
            List<int[]> pendingRows = snapshot.getPendingGarbageRows();
            drawGarbagePreview(pendingRows);
            garbageCountLabel.setText("넘어온 줄: " + pendingRows.size());
            PieceType nextPiece = snapshot.getNextPieces().isEmpty() ? null : snapshot.getNextPieces().getFirst();
            drawPreview(nextCanvas, nextPiece, snapshot.getNextPieceItem());
            drawPreview(holdCanvas, snapshot.getHeldPiece(), snapshot.getHeldPieceItem());
        } else {
            renderPuyoSidebar(puyo);
        }

        // AI-assisted code start (redesign)
        scoreLabel.setText(String.format("%06d", snapshot.getScore()));
        intervalLabel.setText(snapshot.getDropInterval() + "ms");
        boolean paused = snapshot.getStatus() == GameStatus.PAUSED;
        pauseLabel.setVisible(paused);
        boardCanvas.setEffect(paused ? new GaussianBlur(PAUSE_BLUR_RADIUS * scale) : null);
        // AI-assisted code end
    }

    private void renderPuyoSidebar(PuyoDisplay puyo) {
        holdPanel.setVisible(false);
        garbageCanvas.setVisible(false);
        chainLabel.setText("연쇄: " + puyo.chain());
        chainPanel.setVisible(true);
        garbageCountLabel.setText("방해 블록: " + puyo.pendingGarbage());
        if (!puyoKeysApplied) {
            String holdHelp = keyBindings.get(GameCommand.HOLD) + ": 홀드";
            keyHelpLabel.setText(keyHelpLabel.getText().replace(holdHelp + "\n", ""));
            showKeyHints(false); // AI-assisted (redesign) 뿌요에는 홀드가 없다
            puyoKeysApplied = true;
        }

        // AI-assisted code start (redesign)
        GraphicsContext graphics = nextCanvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, nextCanvas.getWidth(), nextCanvas.getHeight());
        int count = puyo.nextPairs().size();
        double cell = Math.min(nextCanvas.getHeight() / 2, nextCanvas.getWidth() / Math.max(1, count) * 0.8);
        for (int index = 0; index < count; index++) {
            PairColors pair = puyo.nextPairs().get(index);
            double x = (index + 0.5) * nextCanvas.getWidth() / count - cell / 2;
            double y = nextCanvas.getHeight() / 2;
            drawPuyoCell(graphics, pair.child(), false, x, y - cell, cell);
            drawPuyoCell(graphics, pair.pivot(), false, x, y, cell);
        }
        // AI-assisted code end
    }

    private void drawPuyoCell(GraphicsContext graphics, int value, boolean clearing, double x, double y, double cell) {
        if (value == Board.EMPTY_CELL) {
            return;
        }
        int colorIndex = value == Board.GARBAGE_CELL ? -1 : value - 1;
        renderer.drawPuyo(graphics, x, y, cell, colorIndex, clearing); // AI-assisted (redesign)
    }

    @Override
    public void show() {
        if (stage != null) {
            stage.show();
        }
        content.requestFocus();
    }

    @Override
    public void close() {
        if (stage != null) {
            stage.close();
        }
    }

    private void initializeView(boolean attachToStage) {
        boardCanvas.setId("game-board");
        nextCanvas.setId("next-preview");
        holdCanvas.setId("hold-preview");
        scoreLabel.setId("score-label");
        intervalLabel.setId("interval-label");
        pauseLabel.setId("pause-label");

        // AI-assisted code start (redesign) 정보 칸을 제목 + 값으로 된 패널로 만든다
        styleText(scoreLabel, VALUE_STYLE_CLASS, Theme.monoFont(FontWeight.BOLD, SCORE_FONT_SIZE * scale));
        styleText(intervalLabel, VALUE_STYLE_CLASS, Theme.monoFont(FontWeight.BOLD, VALUE_FONT_SIZE * scale));
        styleText(chainLabel, VALUE_STYLE_CLASS, Theme.uiFont(FontWeight.BOLD, VALUE_FONT_SIZE * scale));
        styleText(garbageCountLabel, NOTE_STYLE_CLASS, Theme.uiFont(NOTE_FONT_SIZE * scale));

        garbageCanvas.setId("garbage-preview");
        garbageCountLabel.setId("garbage-count-label");
        VBox nextPanel = createPanel("NEXT", nextCanvas);
        holdPanel = createPanel("HOLD", holdCanvas);
        chainPanel = createPanel("CHAIN", chainLabel);
        chainPanel.setId("puyo-chain"); // 뿌요에서만 보이는 연쇄 칸
        VBox scorePanel = createPanel("SCORE", scoreLabel);
        VBox speedPanel = createPanel("SPEED", intervalLabel);
        VBox garbagePanel = createPanel("ATTACK", garbageCanvas, garbageCountLabel);

        boolean showGarbage = !attachToStage; // 공격 줄은 대전 화면(embedded)에서만 받는다
        garbagePanel.setVisible(showGarbage);
        garbageCountLabel.setVisible(showGarbage);
        garbageCountLabel.setManaged(showGarbage);
        garbageCanvas.setVisible(showGarbage);
        garbageCanvas.managedProperty().bind(garbageCanvas.visibleProperty());
        chainPanel.setVisible(false);
        for (Node collapsible : List.of(holdPanel, chainPanel, garbagePanel)) {
            collapsible.managedProperty().bind(collapsible.visibleProperty());
        }

        pauseLabel.setText("PAUSED\n" + KeyNames.display(keyBindings.get(GameCommand.PAUSE)) + " 재개 · "
                + KeyNames.display(keyBindings.get(GameCommand.QUIT_GAME)) + " 메뉴로");
        pauseLabel.getStyleClass().add("pause-card");
        pauseLabel.setFont(Theme.uiFont(FontWeight.BOLD, PAUSE_FONT_SIZE * scale));
        pauseLabel.setAlignment(Pos.CENTER);
        pauseLabel.setVisible(false);
        pauseLabel.setMouseTransparent(true);
        pauseShade.getStyleClass().add("pause-shade");
        pauseShade.visibleProperty().bind(pauseLabel.visibleProperty());
        pauseShade.setMouseTransparent(true);

        StackPane boardArea = new StackPane(boardCanvas, pauseShade, pauseLabel);
        boardArea.getStyleClass().add("board-frame");
        boardArea.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        VBox sidebar = new VBox(
                SIDEBAR_SPACING * scale,
                nextPanel,
                holdPanel,
                chainPanel,
                scorePanel,
                speedPanel,
                garbagePanel,
                createKeyHelp());
        sidebar.setPrefWidth(SIDEBAR_WIDTH * scale);
        sidebar.setMinWidth(SIDEBAR_WIDTH * scale);

        HBox root = new HBox(CONTENT_SPACING * scale, boardArea, sidebar);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(ROOT_PADDING * scale));
        root.setFocusTraversable(true);
        // AI-assisted code end

        content = root;
        root.focusedProperty().addListener(
                (observable, wasFocused, focused) -> {
                    if (!focused) {
                        pauseKeyHeld = false;
                    }
                });

        // AI-assisted code start (redesign)
        // 키 처리기는 화면 맨 바깥 노드에 단다. 1인 게임은 본문을 배경·키 안내와 함께 감싸므로,
        // 본문에 포커스가 있을 때 올라오는 키와 창 전체로 오는 키를 한 곳에서 한 번만 처리한다
        Parent keyTarget = root;
        if (attachToStage) {
            // 1인 게임: 제도 책상 배경 + 게임 + 아래 키 안내
            setKeyHelpVisible(false);
            showKeyHints(true);
            BorderPane frame = new BorderPane(root);
            frame.setBottom(keyHints);
            StackPane window = new StackPane(
                    new DraftingDeskBackground(DraftingDeskBackground.Layout.EDGES, false), frame);
            keyTarget = window;
        }
        keyTarget.setOnKeyPressed(event -> handleKey(event.getCode()));
        keyTarget.setOnKeyReleased(event -> {
            if (mapKey(event.getCode()) == GameCommand.PAUSE) {
                pauseKeyHeld = false;
            }
        });
        // AI-assisted code end

        if (attachToStage) {
            Scene scene = new Scene(keyTarget, VIEW_WIDTH * scale, VIEW_HEIGHT * scale); // AI-assisted (redesign)
            Theme.apply(scene); // AI-assisted (redesign)
            stage.setTitle("SE Tetris");
            stage.setScene(scene);
            stage.sizeToScene();
        }
    }

    // AI-assisted code start (redesign)
    /** 1인 게임 화면 아래 키 안내. 뿌요는 홀드가 없으므로 withHold를 false로 부른다 */
    private void showKeyHints(boolean withHold) {
        keyHints.clear()
                .add("이동", keyBindings.get(GameCommand.MOVE_LEFT), keyBindings.get(GameCommand.MOVE_RIGHT))
                .add("회전", keyBindings.get(GameCommand.ROTATE_CLOCKWISE))
                .add("하드 드롭", keyBindings.get(GameCommand.HARD_DROP));
        if (withHold) {
            keyHints.add("홀드", keyBindings.get(GameCommand.HOLD));
        }
        keyHints.add("일시정지", keyBindings.get(GameCommand.PAUSE))
                .add("메뉴", keyBindings.get(GameCommand.QUIT_GAME));
    }

    /** 제목(NEXT, SCORE 등)과 내용을 묶은 흰 패널 */
    private VBox createPanel(String title, Node... children) {
        Label titleLabel = new Label(title);
        styleText(titleLabel, PANEL_TITLE_STYLE_CLASS, Theme.monoFont(FontWeight.BOLD, PANEL_TITLE_FONT_SIZE * scale));
        VBox panel = new VBox(3 * scale, titleLabel);
        panel.getChildren().addAll(children);
        panel.getStyleClass().add(PANEL_STYLE_CLASS);
        panel.setPadding(new Insets(6 * scale, 8 * scale, 7 * scale, 8 * scale));
        return panel;
    }

    private static void styleText(Label label, String styleClass, Font font) {
        label.getStyleClass().add(styleClass);
        label.setFont(font);
    }
    // AI-assisted code end

    private Label createKeyHelp() {
        Label help = new Label(
                keyBindings.get(GameCommand.HOLD) + ": 홀드\n" + keyBindings.get(GameCommand.PAUSE) + ": 일시정지 / 재개\n"
                        + keyBindings.get(GameCommand.QUIT_GAME) + ": 메뉴");
        styleText(help, NOTE_STYLE_CLASS, Theme.uiFont(NOTE_FONT_SIZE * scale)); // AI-assisted (redesign)
        help.setWrapText(true);
        help.setId("game-key-help");
        keyHelpLabel = help;
        return help;
    }

    // AI-assisted code start (redesign) 보드: 밝은 종이 + 격자 위에 타일 블록. 보드 높이는 늘 20줄 높이와 같다
    private void drawBoard(int[][] cells, List<Integer> clearingRows, Map<Position, Integer> ghostPieceCells,
            PuyoDisplay puyo) {
        int rows = cells.length;
        int cols = rows == 0 ? 0 : cells[0].length;
        double cell = rows == 0 ? CELL_SIZE * scale : BOARD_HEIGHT_CELLS * CELL_SIZE * scale / rows;

        boardCanvas.setWidth(cols * cell);
        boardCanvas.setHeight(rows * cell);

        GraphicsContext graphics = boardCanvas.getGraphicsContext2D();
        renderer.drawBoardBackground(graphics, boardCanvas.getWidth(), boardCanvas.getHeight(), cell);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double x = col * cell;
                double y = row * cell;
                int value = cells[row][col];

                if (puyo != null) {
                    drawPuyoCell(graphics, value, puyo.clearing().contains(new Position(row, col)), x, y, cell);
                    continue;
                }

                if (clearingRows.contains(row)) {
                    renderer.drawClearing(graphics, x, y, cell);
                } else if (value == Board.GARBAGE_CELL) {
                    renderer.drawGarbage(graphics, x, y, cell);
                } else if (value == Board.EMPTY_CELL) {
                    if (ghostPieceCells.containsKey(new Position(row, col))) {
                        renderer.drawGhost(graphics, x, y, cell);
                    }
                } else {
                    PieceType type = findType(Board.extractCellId(value));
                    renderer.drawBlock(graphics, x, y, cell, type, findItemSymbol(value));
                }
            }
        }
    }

    private void drawPreview(Canvas canvas, PieceType type, Item item) {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (type == null) {
            return;
        }

        var positions = type.getCellsAt(SPAWN_ROTATION);

        int minRow = positions.stream().mapToInt(Position::row).min().orElse(0);
        int maxRow = positions.stream().mapToInt(Position::row).max().orElse(0);
        int minColumn = positions.stream().mapToInt(Position::column).min().orElse(0);
        int maxColumn = positions.stream().mapToInt(Position::column).max().orElse(0);

        int pieceColumns = maxColumn - minColumn + 1;
        int pieceRows = maxRow - minRow + 1;
        double cell = Math.min(CELL_SIZE * PREVIEW_CELL_RATIO * scale,
                Math.min(canvas.getWidth() / pieceColumns, canvas.getHeight() / pieceRows));
        double offsetX = (canvas.getWidth() - pieceColumns * cell) / 2;
        double offsetY = (canvas.getHeight() - pieceRows * cell) / 2;

        for (int i = 0; i < positions.size(); i++) {
            double x = offsetX + (positions.get(i).column() - minColumn) * cell;
            double y = offsetY + (positions.get(i).row() - minRow) * cell;
            int cellValue = Board.calculateCellValue(type, item, i);
            renderer.drawBlock(graphics, x, y, cell, type, findItemSymbol(cellValue));
        }
    }
    // AI-assisted code end

    // AI-assisted code start
    /**
     * 받은 공격 줄을 아래쪽부터 그린다. 목록의 마지막 줄이 맨 아래로, 보드에 쌓일 모양과 같다.
     * 10줄보다 많으면 아래쪽 10줄만 그린다.
     */
    private void drawGarbagePreview(List<int[]> rows) {
        GraphicsContext graphics = garbageCanvas.getGraphicsContext2D();
        graphics.setFill(BlockRenderer.BOARD_BACKGROUND);
        graphics.fillRect(0, 0, garbageCanvas.getWidth(), garbageCanvas.getHeight());

        if (rows.isEmpty()) {
            return;
        }

        int shownCount = Math.min(rows.size(), GARBAGE_PREVIEW_ROWS);
        int firstShownIndex = rows.size() - shownCount;
        double miniCellWidth = garbageCanvas.getWidth() / rows.get(0).length;
        double miniCellHeight = garbageCanvas.getHeight() / GARBAGE_PREVIEW_ROWS;

        graphics.setFill(BlockRenderer.GARBAGE_HATCH);
        for (int index = 0; index < shownCount; index++) {
            int[] row = rows.get(firstShownIndex + index);
            int previewRow = GARBAGE_PREVIEW_ROWS - shownCount + index;

            for (int column = 0; column < row.length; column++) {
                if (row[column] != Board.EMPTY_CELL) {
                    // 1px씩 빼서 칸 사이에 틈을 두면 구멍 위치가 잘 보인다
                    graphics.fillRect(column * miniCellWidth, previewRow * miniCellHeight,
                            miniCellWidth - 1, miniCellHeight - 1);
                }
            }
        }
    }
    // AI-assisted code end

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

    /** 칸에 아이템이 있으면 아이템 글자, 없으면 null */
    private String findItemSymbol(int cellValue) {
        int itemCode = Board.extractItemCode(cellValue);
        if (itemCode != Board.NO_ITEM_CODE) {
            return ItemType.findSymbolByCode(itemCode);
        }
        return null;
    }
}
