package tetris.puyo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.Set;

import tetris.model.Board;
import tetris.model.ControlledGame;
import tetris.model.GameCommand;
import tetris.model.GameKind;
import tetris.model.GameSnapshot;
import tetris.model.GameStatus;
import tetris.model.Position;
import tetris.model.PuyoDisplay;
import tetris.model.PuyoDisplay.PairColors;

/** 색 블록 쌍의 이동, 연쇄, 점수 및 방해 블록을 처리한다. */
public final class PuyoGame implements ControlledGame {
    private static final long DROP_INTERVAL = 800L;
    private static final long LOCK_DELAY = 533L; // 60fps에서 32프레임에 해당하는 시간
    private static final long HIGHLIGHT_DELAY = 480L;
    private static final long FALL_STEP_DELAY = 75L;
    private static final long SETTLE_DELAY = 150L;
    private static final int NEXT_PAIR_COUNT = 3;
    private static final int POINTS_PER_GARBAGE = 70;
    private static final int[] CHAIN_BONUS = {
        0, 0, 8, 16, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, 480, 512
    };
    private static final int[] COLOR_BONUS = {0, 0, 3, 6, 12};
    private static final int[] GROUP_BONUS = {0, 0, 0, 0, 0, 2, 3, 4, 5, 6, 7};
    private static final List<Position> CHILD_OFFSETS = List.of(
            new Position(-1, 0), new Position(0, 1), new Position(1, 0), new Position(0, -1));
    private static final List<Position> ROTATION_OFFSETS = List.of(
            new Position(0, 0), new Position(0, 1), new Position(0, -1),
            new Position(-1, 0), new Position(1, 0));

    private final PuyoBoard board;
    private final Random pairRandom;
    private final Random garbageRandom;
    private final Deque<PairColors> next = new ArrayDeque<>();

    private GameStatus status = GameStatus.READY;
    private GameStatus beforePause;
    private Pair active;
    private Set<Position> clearing = Set.of();
    private ClearPhase clearPhase;
    private long lockSequence;
    private long lockId;
    private long score;
    private int chain;
    private int dropBonus;
    private int attackRemainder;
    private int chainAttack;
    private int outgoing;
    private int pendingGarbage;

    public PuyoGame(long seed) {
        this(seed, new int[PuyoBoard.HEIGHT][PuyoBoard.WIDTH]);
    }

    PuyoGame(long seed, int[][] cells) {
        this.board = new PuyoBoard(cells);
        this.pairRandom = new Random(seed);
        this.garbageRandom = new Random(seed ^ 0x9E3779B97F4A7C15L);
    }

    @Override public GameKind kind() { return GameKind.PUYO; }
    @Override public long lockDelayMillis() { return LOCK_DELAY; }
    @Override public GameStatus getStatus() { return status; }
    @Override public long getScore() { return score + dropBonus; }
    @Override public long getPendingLockDelayId() { return lockId; }
    @Override public boolean isLockDelayRunning() { return status == GameStatus.PLAYING && isGrounded(); }

    @Override
    public long clearStepDelayMillis(long defaultDelayMillis) {
        if (status != GameStatus.CLEARING) {
            return defaultDelayMillis;
        }
        return switch (clearPhase) {
            case HIGHLIGHT -> HIGHLIGHT_DELAY;
            case FALL -> FALL_STEP_DELAY;
            case SETTLE -> SETTLE_DELAY;
        };
    }
    @Override public int getPendingGarbageRowCount() { return pendingGarbage; }
    @Override public boolean isPaused() { return status == GameStatus.PAUSED; }
    @Override public boolean isGameOver() { return status == GameStatus.GAMEOVER; }
    @Override public boolean isQuit() { return status == GameStatus.QUIT; }

    @Override
    public void start() {
        if (status == GameStatus.READY) {
            spawn();
        }
    }

    @Override
    public void handleCommand(GameCommand command) {
        if (command == GameCommand.QUIT_GAME) {
            stop();
        } else if (command == GameCommand.PAUSE) {
            if (isPaused()) {
                resume();
            } else {
                pause();
            }
        } else if (status == GameStatus.PLAYING) {
            switch (command) {
                case MOVE_LEFT -> move(0, -1);
                case MOVE_RIGHT -> move(0, 1);
                case ROTATE_CLOCKWISE -> rotate(1);
                case ROTATE_COUNTERCLOCKWISE -> rotate(-1);
                case SOFT_DROP -> {
                    if (move(1, 0)) {
                        dropBonus += 2;
                    }
                    if (isGrounded()) {
                        lock();
                    }
                }
                case HARD_DROP -> {
                    while (move(1, 0)) {
                        dropBonus += 2;
                    }
                    lock();
                }
                default -> { }
            }
            updateLockDelay();
        }
    }

    @Override
    public void tick() {
        if (status == GameStatus.PLAYING) {
            move(1, 0);
            updateLockDelay();
        }
    }

    @Override
    public void pause() {
        if (status == GameStatus.PLAYING || status == GameStatus.CLEARING) {
            beforePause = status;
            status = GameStatus.PAUSED;
        }
    }

    @Override
    public void resume() {
        if (isPaused()) {
            status = beforePause;
        }
    }

    @Override
    public void stop() {
        if (!isQuit() && !isGameOver()) {
            lockId = 0;
            status = GameStatus.QUIT;
        }
    }

    @Override
    public boolean finishLockDelay(long id) {
        if (id == 0 || lockId != id || !isLockDelayRunning()) {
            return false;
        }
        lock();
        return true;
    }

    @Override
    public void finishLineClear() {
        if (status != GameStatus.CLEARING) {
            return;
        }
        switch (clearPhase) {
            case HIGHLIGHT -> {
                for (Position cell : clearing) {
                    board.set(cell.row(), cell.column(), 0);
                }
                clearing = Set.of();
                clearPhase = ClearPhase.FALL;
            }
            case FALL -> {
                if (!board.advanceGravityOneRow()) {
                    clearPhase = ClearPhase.SETTLE;
                }
            }
            case SETTLE -> checkChain();
        }
    }

    @Override
    public GameSnapshot createSnapshot() {
        int[][] cells = board.copyCells();
        if (active != null) {
            Position child = active.child();
            if (active.row() >= 0) {
                cells[active.row()][active.column()] = active.colors().pivot();
            }
            if (child.row() >= 0) {
                cells[child.row()][child.column()] = active.colors().child();
            }
        }
        return GameSnapshot.puyo(cells, getScore(), status, DROP_INTERVAL,
                new PuyoDisplay(List.copyOf(next), clearing, chain, pendingGarbage));
    }

    @Override
    public List<int[]> takeAttackRows() {
        List<int[]> rows = new ArrayList<>();
        int units = outgoing;
        outgoing = 0;
        while (units > 0) {
            int[] row = new int[PuyoBoard.WIDTH];
            int count = Math.min(units, row.length);
            Arrays.fill(row, 0, count, Board.GARBAGE_CELL);
            rows.add(row);
            units -= count;
        }
        return rows;
    }

    @Override
    public void receiveGarbageRows(List<int[]> rows) {
        if (isQuit() || isGameOver()) {
            return;
        }
        for (int[] row : rows) {
            for (int value : row) {
                if (value == Board.GARBAGE_CELL && pendingGarbage < PuyoDisplay.MAX_PENDING_GARBAGE) {
                    pendingGarbage++;
                }
            }
        }
    }

    private void spawn() {
        status = GameStatus.PLAYING;
        fillNext(NEXT_PAIR_COUNT + 1);
        active = new Pair(0, 2, 0, next.removeFirst());
        chain = 0;
        if (collides(active)) {
            active = null;
            status = GameStatus.GAMEOVER;
        }
        updateLockDelay();
    }

    private void fillNext(int size) {
        while (next.size() < size) {
            next.addLast(new PairColors(randomColor(), randomColor()));
        }
    }

    private int randomColor() {
        return 1 + pairRandom.nextInt(PuyoDisplay.COLOR_COUNT);
    }

    private boolean move(int rowDelta, int columnDelta) {
        return replace(new Pair(active.row() + rowDelta, active.column() + columnDelta,
                active.rotation(), active.colors()));
    }

    private void rotate(int direction) {
        int rotation = Math.floorMod(active.rotation() + direction, CHILD_OFFSETS.size());
        for (Position offset : ROTATION_OFFSETS) {
            if (replace(new Pair(active.row() + offset.row(), active.column() + offset.column(),
                    rotation, active.colors()))) {
                return;
            }
        }
    }

    private boolean replace(Pair candidate) {
        if (collides(candidate)) {
            return false;
        }
        active = candidate;
        return true;
    }

    private boolean collides(Pair pair) {
        Position child = pair.child();
        return pair.row() < 0 || child.row() < -1 || board.blocked(pair.row(), pair.column())
                || board.blocked(child.row(), child.column());
    }

    private boolean isGrounded() {
        return active != null && collides(new Pair(active.row() + 1, active.column(),
                active.rotation(), active.colors()));
    }

    private void updateLockDelay() {
        // 같은 쌍이 다시 공중에 떠도 ID를 유지해 이미 쓴 착지 시간이 초기화되지 않게 한다.
        if (lockId == 0 && isGrounded()) {
            lockId = ++lockSequence;
        }
    }

    private void lock() {
        Position child = active.child();
        board.set(active.row(), active.column(), active.colors().pivot());
        lockId = 0;
        if (child.row() < 0) {
            active = null;
            status = GameStatus.GAMEOVER;
            return;
        }
        board.set(child.row(), child.column(), active.colors().child());
        active = null;
        board.settle();
        checkChain();
    }

    private void checkChain() {
        PuyoBoard.Clear clear = board.findClear();
        if (clear.cells().isEmpty()) {
            score += dropBonus;
            dropBonus = 0;
            finishTurn();
            return;
        }

        chain++;
        clearing = clear.cells();
        clearPhase = ClearPhase.HIGHLIGHT;
        int erased = clear.groups().stream().mapToInt(Integer::intValue).sum();
        int groupBonus = clear.groups().stream()
                .mapToInt(size -> size < GROUP_BONUS.length ? GROUP_BONUS[size] : 10).sum();
        int bonus = CHAIN_BONUS[Math.min(chain, CHAIN_BONUS.length - 1)]
                + COLOR_BONUS[clear.colorCount()] + groupBonus;
        long gained = 10L * erased * Math.max(1, bonus) + dropBonus;
        score += gained;
        dropBonus = 0;
        long attackScore = attackRemainder + gained;
        chainAttack += (int) (attackScore / POINTS_PER_GARBAGE);
        attackRemainder = (int) (attackScore % POINTS_PER_GARBAGE);
        status = GameStatus.CLEARING;
    }

    private void finishTurn() {
        int cancelled = Math.min(chainAttack, pendingGarbage);
        outgoing = Math.min(PuyoDisplay.MAX_PENDING_GARBAGE, outgoing + chainAttack - cancelled);
        pendingGarbage -= cancelled;
        chainAttack = 0;
        if (placeGarbage()) {
            spawn();
        }
    }

    private boolean placeGarbage() {
        List<Integer> columns = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
        if (pendingGarbage % PuyoBoard.WIDTH != 0) {
            Collections.shuffle(columns, garbageRandom);
        }
        for (int index = 0; index < pendingGarbage; index++) {
            int col = columns.get(index % PuyoBoard.WIDTH);
            if (board.blocked(0, col)) {
                pendingGarbage = 0;
                status = GameStatus.GAMEOVER;
                return false;
            }
            int row = 0;
            while (!board.blocked(row + 1, col)) {
                row++;
            }
            board.set(row, col, Board.GARBAGE_CELL);
        }
        pendingGarbage = 0;
        return true;
    }

    private enum ClearPhase { HIGHLIGHT, FALL, SETTLE }

    private record Pair(int row, int column, int rotation, PairColors colors) {
        Position child() {
            Position offset = CHILD_OFFSETS.get(rotation);
            return new Position(row + offset.row(), column + offset.column());
        }
    }
}
