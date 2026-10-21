package tetris.model;

import java.util.List;
import java.util.Random;
import static java.util.Objects.requireNonNull;

public class Game {

    private static final long INITIAL_SCORE = 0L;
    private static final long INITIAL_DROP_INTERVAL = 1_000L;
    private static final int LINES_PER_LEVEL = 5;
    private static final int LINES_PER_ITEM_SPAWN = 10;
    private static final int PIECES_PER_LEVEL = 10;
    private static final long DRILL_INTERVAL = 50L;
    private static final long DROP_INTERVAL_STEP = 100L;
    private static final long MIN_DROP_INTERVAL = 100L;
    private static final long LINE_CLEAR_BONUS = 100L;
    private static final long SLOW_DURATION = 10_000L;
    private static final long SLOW_MULTIPLIER = 2L;

    private final Board board;
    private final TetrominoGenerator generator;
    private final ItemGenerator itemGenerator;
    private final GameMode mode;
    private final Difficulty difficulty;
    private final ItemContext context = new GameItemContext();
    private GameStatus status;

    private Piece currentPiece; // 현재 드랍중인 블록
    private PieceType nextPieceType; // 다음에 드랍될 블록 타입
    private Item nextItem; // 다음에 생성될 아이템
    private PieceType heldPieceType; // 홀드된 블록 종류
    private Item heldItem; // 홀드된 블록의 아이템 종류
    private boolean holdUsed; // 이번 블록에서 홀드를 이미 썼는지
    private boolean itemReserved; // spawnNextPiece 에서 아이템을 스폰하도록 예약하는 필드
    private boolean drilling; // 아이템 HEAVY WEIGHT(무게추)이 고정되지 않고 아래 칸을 부수는 상태인가
    private boolean doubleScoreReserved; // 점수를 두배로 계산하는 것을 예약하는 변수
    private boolean specialBlockReserved; // 특별 블럭을 스폰하는 것을 예약하는 변수
    private int lastClearedLines; // 마지막 고정 때 지운 줄 수 (T6 점수 계산용)
    private int itemLineCounter; // 아이템 생성까지 지운 줄 수 카운터, 초과분은 이월됨.
    private int itemClearedLines; // 아이템 효과로 지운 줄 수
    private long lockedPieceCount; // 고정된 블록 수
    private long spawnedPieceCount; // 실제 생성된 블록 수
    private long gravityDropCells; // 자동 낙하 칸 수
    private long softDropCells; // 소프트 드롭 칸 수
    private long hardDropCells; // 하드 드롭 칸 수
    private long score = INITIAL_SCORE;
    private long dropInterval = INITIAL_DROP_INTERVAL;
    private long totalClearedLines;
    private long currentPieceScore; // 지금 블록이 스폰된 뒤 얻은 점수 합계
    private long slowTimeLeft; // 남은 속도 저하 시간

    public Game(Board board, TetrominoGenerator generator, Difficulty difficulty, GameMode mode) {
        this(board, generator, new ItemGenerator(new Random()), difficulty, mode);
    }

    public Game(Board board, TetrominoGenerator generator, ItemGenerator itemGenerator, Difficulty difficulty,
            GameMode mode) {
        requireNonNull(board, "board");
        requireNonNull(generator, "generator");
        requireNonNull(itemGenerator, "itemGenerator");
        requireNonNull(difficulty, "difficulty");
        requireNonNull(mode, "mode");

        this.board = board;
        this.generator = generator;
        this.itemGenerator = itemGenerator;
        this.difficulty = difficulty;
        this.mode = mode;
        this.status = GameStatus.READY;
    }

    /**
     * 초기 낙하 간격을 반환한다.
     * TetrisApplication에서 timer 생성시 사용한다.
     */
    public static long getInitialDropInterval() {
        return INITIAL_DROP_INTERVAL;
    }

    /**
     * 게임 시작 후 고정된 블록의 누적 개수를 반환한다.
     */
    public long getLockedPieceCount() {
        return lockedPieceCount;
    }

    /**
     * 중력(tick)으로 실제로 내려간 칸 수의 누적값을 반환한다.
     */
    public long getGravityDropCells() {
        return gravityDropCells;
    }

    /**
     * 소프트 드롭으로 실제로 내려간 칸 수의 누적값을 반환한다.
     */
    public long getSoftDropCells() {
        return softDropCells;
    }

    /**
     * 하드 드롭으로 실제로 내려간 칸 수의 누적값을 반환한다.
     * 이미 바닥에 붙은 상태에서 누르면 0칸으로 센다.
     */
    public long getHardDropCells() {
        return hardDropCells;
    }

    /**
     * 가장 최근에 블록이 고정될 때 지운 줄 수를 반환한다. 점수 계산에 사용된다.
     *
     * @return 지운 줄 수 (0~4)
     */
    public int getLastClearedLines() {
        return lastClearedLines;
    }

    /**
     * 게임을 시작 가능한 상태에서 실행 상태로 변경하고, 첫 블록을 내보낸다.
     */
    public void start() {
        if (status != GameStatus.READY) {
            return;
        }
        status = GameStatus.PLAYING;
        nextPieceType = generator.generateNext();
        spawnNextPiece();
    }

    /**
     * 사용자 명령을 현재 게임 상태에 적용한다
     * 일시정지 중에는 재개와 종료만 허용한다
     * 이동과 회전은 블록을 놓을 수 없는 위치이면 무시한다
     *
     */
    public void handleCommand(GameCommand command) {
        if (command == GameCommand.QUIT_GAME && (status == GameStatus.PLAYING || status == GameStatus.PAUSED)) {
            status = GameStatus.QUIT;
            return;
        }

        if (command == GameCommand.PAUSE) {
            if (status == GameStatus.PLAYING) {
                status = GameStatus.PAUSED;
            } else if (status == GameStatus.PAUSED) {
                status = GameStatus.PLAYING;
            }
            return;
        }

        if (status != GameStatus.PLAYING) {
            return;
        }

        if (drilling) {
            return;
        }

        long previousDropCells = getTotalDropCells();
        long previousLockedPieceCount = lockedPieceCount;
        long previousSpawnedPieceCount = spawnedPieceCount;

        switch (command) {
            case MOVE_LEFT -> tryReplace(currentPiece.move(0, -1));
            case MOVE_RIGHT -> tryReplace(currentPiece.move(0, 1));
            case ROTATE_CLOCKWISE -> {
                if (currentPiece.canRotate()) {
                    tryReplace(currentPiece.rotateClockwise());
                }
            }
            case ROTATE_COUNTERCLOCKWISE -> {
                if (currentPiece.canRotate()) {
                    tryReplace(currentPiece.rotateCounterClockwise());
                }
            }
            case SOFT_DROP -> {
                if (softDrop()) {
                    softDropCells++;
                }
            }
            case HARD_DROP -> hardDrop();
            case HOLD -> hold();
            case PAUSE, QUIT_GAME, NONE -> {
            }
        }

        updateProgress(previousDropCells, previousLockedPieceCount, previousSpawnedPieceCount);
    }

    /**
     * 자동 낙하를 한 번 진행하고 점수와 속도를 갱신한다
     * 
     */
    public void tick() {
        if (status != GameStatus.PLAYING) {
            return;
        }

        if (slowTimeLeft > 0) {
            slowTimeLeft -= selectTimeInterval();
            slowTimeLeft = Math.max(0, slowTimeLeft);
        }
        long previousDropCells = getTotalDropCells();
        long previousLockedPieceCount = lockedPieceCount;
        long previousSpawnedPieceCount = spawnedPieceCount;

        if (drilling) {
            drillDown();
        } else if (softDrop()) {
            gravityDropCells++;
        }
        updateProgress(previousDropCells, previousLockedPieceCount, previousSpawnedPieceCount);
    }

    public boolean isPaused() {
        return status == GameStatus.PAUSED;
    }

    // 사용자가 게임을 종료했으면 true (GAMEOVER는 포함되지 않음)
    public boolean isFinished() {
        return status == GameStatus.QUIT;
    }

    // 새 블록이 스폰 위치에 놓이지 못해 게임이 끝났으면 true
    public boolean isGameOver() {
        return status == GameStatus.GAMEOVER;
    }

    /**
     * 화면 표시용으로 현재 게임 상태의 사본을 만든다.
     * 셀에는 고정된 블록과 떨어지는 블록이 함께 담기며, 게임오버이거나 시작 전이면 고정된 블록만 담긴다.
     * 홀드된 블록 종류도 함께 담긴다.
     *
     * @return 현재 상태의 스냅샷
     */
    public GameSnapshot createSnapshot() {
        int[][] cells;
        if (isGameOver() || currentPiece == null) {
            cells = board.copyCells();
        } else {
            cells = board.copyCellsWith(currentPiece);
        }
        List<PieceType> list;
        if (nextPieceType != null) {
            list = List.of(nextPieceType);
        } else {
            list = List.of();
        }
        return new GameSnapshot(
                cells,
                list,
                heldPieceType,
                score,
                status,
                selectTimeInterval());
    }

    // 후보가 놓일 수 있으면 현재 블록을 교체하고 true, 아니면 그대로 두고 false
    private boolean tryReplace(Piece candidate) {
        if (!board.canPlace(candidate)) {
            return false;
        } else {
            currentPiece = candidate;
            return true;
        }
    }

    // 한 칸 내렸으면 true, 못 내려가서 고정했으면 false
    private boolean softDrop() {
        if (tryReplace(currentPiece.move(1, 0))) {
            return true;
        } else {
            land();
            return false;
        }
    }

    // 바닥까지 한번에 내린 뒤 바로 고정
    private void hardDrop() {
        while (tryReplace(currentPiece.move(1, 0))) {
            hardDropCells++;
        }
        land();
    }

    // 현재 블록을 고정하고, 줄을 지우고, 다음 블록을 내보냄
    private void lockAndSpawnNext() {
        itemClearedLines = 0;
        lockedPieceCount++;
        board.lock(currentPiece);
        if (currentPiece.item() != null && currentPiece.item().isEmbedded()) {
            int cellIndex = currentPiece.item().cellIndex();
            Position position = currentPiece.getAbsolutePositions().get(cellIndex);
            currentPiece.item().type().getItemEffect().handleLock(context, position);
        }

        lastClearedLines = board.clearFullLines() + itemClearedLines;
        if (currentPiece.item() != null) {
            currentPiece.item().type().getItemEffect().handleLinesCleared(context, lastClearedLines);
        }
        holdUsed = false;

        if (mode == GameMode.ITEM) {
            itemLineCounter += lastClearedLines;
            if (itemLineCounter >= LINES_PER_ITEM_SPAWN) {
                itemLineCounter -= LINES_PER_ITEM_SPAWN;
                itemReserved = true;
            }
        }
        notifyDestroyedItems();
        spawnNextPiece();
    }

    // nextPieceType를 꺼내 스폰하고, nextPieceType를 새로 채움
    private void spawnNextPiece() {
        spawn(nextPieceType, nextItem);
        if (!isGameOver()) {
            spawnedPieceCount++;
        }
        nextPieceType = generator.generateNext();
        nextItem = null;
        if (itemReserved) {
            nextItem = itemGenerator.generateNext();
            PieceType dedicatedPieceType = nextItem.type().getDedicatedPieceType();
            if (dedicatedPieceType != null) {
                nextPieceType = dedicatedPieceType;
            }
            itemReserved = false;
        } else if (specialBlockReserved) {
            nextPieceType = PieceType.SPECIAL_BLOCK;
            specialBlockReserved = false;
        }
    }

    // 주어진 종류를 스폰 위치에 놓음. 놓을 수 없으면 게임오버
    private void spawn(PieceType type, Item item) {
        Piece candidate = Piece.spawn(type, item, board.getWidth());
        if (!board.canPlace(candidate)) {
            status = GameStatus.GAMEOVER;
            return;
        }
        currentPiece = candidate;
    }

    // 현재 블록을 홀드하거나 홀드된 블록과 바꾼다. 블록 하나당 한번만 가능하다.
    private void hold() {
        if (holdUsed) {
            return;
        }

        PieceType currentType = currentPiece.type();
        Item currentItem = currentPiece.item();
        if (heldPieceType == null) {
            heldPieceType = currentType;
            heldItem = currentItem;
            spawnNextPiece();
        } else {
            PieceType spawnType = heldPieceType;
            Item spawnItem = heldItem;
            heldPieceType = currentType;
            heldItem = currentItem;
            spawn(spawnType, spawnItem);
        }
        currentPieceScore = 0;
        holdUsed = true;
    }

    private long getTotalDropCells() {
        return gravityDropCells + softDropCells + hardDropCells;
    }

    /**
     * 이번 명령 또는 자동 낙하의 결과만 점수에 반영한다
     * 줄 삭제 보너스는 블록이 새로 고정된 경우에만 반영한다
     *
     * @param previousDropCells         실행 전 누적 낙하 칸 수
     * @param previousLockedPieceCount  실행 전 누적 블록 고정 횟수
     * @param previousSpawnedPieceCount 실행 전 누적 블록 생성 수
     */
    private void updateProgress(long previousDropCells, long previousLockedPieceCount, long previousSpawnedPieceCount) {

        long previousScore = score;
        long previousClearedLines = totalClearedLines;
        long speedReduction = INITIAL_DROP_INTERVAL - dropInterval;
        long level = (speedReduction + DROP_INTERVAL_STEP - 1) / DROP_INTERVAL_STEP + 1;
        long droppedCells = getTotalDropCells() - previousDropCells;
        boolean pieceLocked = lockedPieceCount > previousLockedPieceCount;

        score += droppedCells * level;

        if (pieceLocked) {
            long clearedLines = lastClearedLines;

            score += LINE_CLEAR_BONUS * clearedLines * clearedLines * level;
            totalClearedLines += clearedLines;
        }

        currentPieceScore += score - previousScore;

        if (pieceLocked) {
            if (doubleScoreReserved) {
                score += currentPieceScore;
                doubleScoreReserved = false;
            }
            currentPieceScore = 0;
        }

        long lineIncreases = totalClearedLines / LINES_PER_LEVEL - previousClearedLines / LINES_PER_LEVEL;
        long pieceIncreases = spawnedPieceCount / PIECES_PER_LEVEL - previousSpawnedPieceCount / PIECES_PER_LEVEL;
        long intervalReduction = lineIncreases * difficulty.getLineDropIntervalStep()
                + pieceIncreases * DROP_INTERVAL_STEP;

        dropInterval = Math.max(MIN_DROP_INTERVAL, dropInterval - intervalReduction);
    }

    // 드릴 중 한 칸 아래를 부수며 내려가고, 바닥이면 고정한다.
    private void drillDown() {
        Piece candidate = currentPiece.move(1, 0);
        List<Position> positions = candidate.getAbsolutePositions();
        for (Position pos : positions) {
            if (!board.isInside(pos.row(), pos.column())) {
                drilling = false;
                lockAndSpawnNext();
                return;
            }
        }
        for (Position pos : positions) {
            if (!board.isEmpty(pos.row(), pos.column())) {
                board.clearCell(pos.row(), pos.column());
            }
        }
        notifyDestroyedItems();
        currentPiece = candidate;
    }

    // 블록이 더 내려갈 수 없을 때 아이템 효과에 첫 착지를 알리고, 드릴이 시작되지 않았으면 고정한다.
    private void land() {
        if (currentPiece.item() != null && !drilling) {
            currentPiece.item().type().getItemEffect().handleFirstLanding(context);
        }
        if (!drilling) {
            lockAndSpawnNext();
        }
    }

    // 부서진 아이템 효과를 발동시킨다.
    private void notifyDestroyedItems() {
        for (int itemCode : board.takeDestroyedItemCodes()) {
            ItemType type = ItemType.findTypeByCode(itemCode);
            if (type != null) {
                type.getItemEffect().handleDestroyed(context);
            }
        }
    }

    // 드릴, 속도 저하를 반영한 실제 낙하 간격을 반환한다.
    private long selectTimeInterval() {
        if (drilling) {
            return DRILL_INTERVAL;
        } else if (slowTimeLeft > 0) {
            return dropInterval * SLOW_MULTIPLIER;
        } else {
            return dropInterval;
        }
    }

    private class GameItemContext implements ItemContext {
        @Override
        public void clearLine(int rowIndex) {
            board.clearLine(rowIndex);
            itemClearedLines++;
        }

        @Override
        public void startDrilling() {
            drilling = true;
        }

        @Override
        public void doubleScore() {
            doubleScoreReserved = true;
        }

        @Override
        public void startSlowDown() {
            slowTimeLeft = SLOW_DURATION;
        }

        @Override
        public void reserveSpecialPiece() {
            specialBlockReserved = true;
        }
    }
}