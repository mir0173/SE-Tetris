package tetris.puyo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tetris.model.Board;
import tetris.model.GameKind;
import tetris.model.Position;

final class PuyoBoard {
    static final int WIDTH = GameKind.PUYO.width();
    static final int HEIGHT = GameKind.PUYO.height();
    private static final int MIN_GROUP_SIZE = 4;
    private static final List<Position> NEIGHBORS = List.of(
            new Position(-1, 0), new Position(1, 0), new Position(0, -1), new Position(0, 1));

    private final int[][] cells;

    PuyoBoard(int[][] cells) {
        this.cells = new int[HEIGHT][];
        if (cells.length != HEIGHT) {
            throw new IllegalArgumentException("뿌요 보드 높이가 다릅니다.");
        }
        for (int row = 0; row < HEIGHT; row++) {
            if (cells[row].length != WIDTH) {
                throw new IllegalArgumentException("뿌요 보드 폭이 다릅니다.");
            }
            this.cells[row] = cells[row].clone();
        }
    }

    boolean blocked(int row, int column) {
        return column < 0 || column >= WIDTH || row >= HEIGHT
                || (row >= 0 && cells[row][column] != 0);
    }

    void set(int row, int column, int color) {
        cells[row][column] = color;
    }

    int[][] copyCells() {
        int[][] copy = new int[HEIGHT][];
        for (int row = 0; row < HEIGHT; row++) {
            copy[row] = cells[row].clone();
        }
        return copy;
    }

    /** 각 열 안에서 아래에서부터 빈칸을 채운다. */
    void settle() {
        for (int col = 0; col < WIDTH; col++) {
            int writeRow = HEIGHT - 1;
            for (int row = HEIGHT - 1; row >= 0; row--) {
                if (cells[row][col] != 0) {
                    cells[writeRow--][col] = cells[row][col];
                }
            }
            for (int row = writeRow; row >= 0; row--) {
                cells[row][col] = 0;
            }
        }
    }

    /** 아래가 비어 있는 블록을 각각 한 칸씩 내린다. */
    boolean advanceGravityOneRow() {
        boolean moved = false;
        for (int row = HEIGHT - 2; row >= 0; row--) {
            for (int col = 0; col < WIDTH; col++) {
                if (cells[row][col] != 0 && cells[row + 1][col] == 0) {
                    cells[row + 1][col] = cells[row][col];
                    cells[row][col] = 0;
                    moved = true;
                }
            }
        }
        return moved;
    }

    Clear findClear() {
        Set<Position> visited = new HashSet<>();
        Set<Position> clearing = new HashSet<>();
        Set<Integer> colors = new HashSet<>();
        List<Integer> groups = new ArrayList<>();
        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < WIDTH; col++) {
                int color = cells[row][col];
                Position start = new Position(row, col);
                if (color == 0 || color == Board.GARBAGE_CELL || !visited.add(start)) {
                    continue;
                }
                List<Position> group = findGroup(start, color, visited);
                if (group.size() < MIN_GROUP_SIZE) {
                    continue;
                }
                groups.add(group.size());
                colors.add(color);
                clearing.addAll(group);
                for (Position member : group) {
                    for (Position offset : NEIGHBORS) {
                        Position neighbor = adjacent(member, offset);
                        if (hasColor(neighbor, Board.GARBAGE_CELL)) {
                            clearing.add(neighbor);
                        }
                    }
                }
            }
        }
        return new Clear(clearing, colors.size(), groups);
    }

    private List<Position> findGroup(Position start, int color, Set<Position> visited) {
        ArrayDeque<Position> remaining = new ArrayDeque<>();
        List<Position> group = new ArrayList<>();
        remaining.add(start);
        while (!remaining.isEmpty()) {
            Position position = remaining.removeFirst();
            group.add(position);
            for (Position offset : NEIGHBORS) {
                Position neighbor = adjacent(position, offset);
                if (hasColor(neighbor, color) && visited.add(neighbor)) {
                    remaining.addLast(neighbor);
                }
            }
        }
        return group;
    }

    private static Position adjacent(Position position, Position offset) {
        return new Position(position.row() + offset.row(), position.column() + offset.column());
    }

    private boolean hasColor(Position position, int color) {
        return position.row() >= 0 && position.row() < HEIGHT && position.column() >= 0
                && position.column() < WIDTH && cells[position.row()][position.column()] == color;
    }

    record Clear(Set<Position> cells, int colorCount, List<Integer> groups) { }
}
