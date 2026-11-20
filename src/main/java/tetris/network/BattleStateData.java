package tetris.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import tetris.model.Board;
import tetris.model.GameSnapshot;
import tetris.model.GameStatus;
import tetris.model.Item;
import tetris.model.ItemType;
import tetris.model.PieceType;
import tetris.model.Position;

/** 전송 전용 상태*/
public record BattleStateData(int[][] cells, List<PieceType> nextPieces, PieceType heldPiece,
        long score, GameStatus status, long dropInterval, Item nextItem, Item heldItem,
        List<Integer> clearingRows, List<GhostCell> ghostCells, List<int[]> pendingRows) {
    private static final int MAX_WIDTH = 30;
    private static final int MAX_HEIGHT = 40;

    public record GhostCell(int row, int column, int value) { }

    public BattleStateData {
        Objects.requireNonNull(cells, "cells");
        Objects.requireNonNull(status, "status");
        if (cells.length == 0 || cells.length > MAX_HEIGHT || cells[0] == null
                || cells[0].length == 0 || cells[0].length > MAX_WIDTH || score < 0 || dropInterval < 0) {
            throw new IllegalArgumentException("잘못된 게임 상태입니다.");
        }
        int width = cells[0].length;
        cells = copyRows(List.of(cells), width).toArray(int[][]::new);
        nextPieces = List.copyOf(nextPieces);
        if (nextPieces.size() > 10) {
            throw new IllegalArgumentException("정보가 너무 많습니다.");
        }
        clearingRows = List.copyOf(clearingRows);
        ghostCells = List.copyOf(ghostCells);
        if (ghostCells.size() > MAX_WIDTH * MAX_HEIGHT) {
            throw new IllegalArgumentException("고스트 정보가 너무 많습니다.");
        }
        for (int row : clearingRows) {
            if (row < 0 || row >= cells.length) {
                throw new IllegalArgumentException("삭제 줄 좌표가 잘못되었습니다.");
            }
        }
        for (GhostCell ghost : ghostCells) {
            if (ghost.row() < 0 || ghost.row() >= cells.length || ghost.column() < 0 || ghost.column() >= width) {
                throw new IllegalArgumentException("고스트 좌표가 잘못되었습니다.");
            }
            validateCell(ghost.value());
        }
        if (pendingRows.size() > MAX_HEIGHT) {
            throw new IllegalArgumentException("대기 줄 정보가 너무 많습니다.");
        }
        pendingRows = copyRows(pendingRows, width);
    }

    public static BattleStateData fromSnapshot(GameSnapshot snapshot) {
        List<GhostCell> ghosts = snapshot.getGhostPieceCells().entrySet().stream()
                .map(entry -> new GhostCell(entry.getKey().row(), entry.getKey().column(), entry.getValue()))
                .toList();
        return new BattleStateData(snapshot.getCells(), snapshot.getNextPieces(), snapshot.getHeldPiece(),
                snapshot.getScore(), snapshot.getStatus(), snapshot.getDropInterval(), snapshot.getNextPieceItem(),
                snapshot.getHeldPieceItem(), snapshot.getClearingRows(), ghosts, snapshot.getPendingGarbageRows());
    }

    public GameSnapshot toSnapshot() {
        HashMap<Position, Integer> ghosts = new HashMap<>();
        for (GhostCell ghost : ghostCells) {
            ghosts.put(new Position(ghost.row(), ghost.column()), ghost.value());
        }
        return new GameSnapshot(cells, nextPieces, heldPiece, score, status, dropInterval, nextItem, heldItem,
                clearingRows, ghosts, pendingRows);
    }

    @Override
    public int[][] cells() {
        return copyRows(List.of(cells), cells[0].length).toArray(int[][]::new);
    }

    @Override
    public List<int[]> pendingRows() {
        return copyRows(pendingRows, cells[0].length);
    }

    private static List<int[]> copyRows(List<int[]> rows, int width) {
        List<int[]> copy = new ArrayList<>();
        for (int[] row : rows) {
            if (row == null || row.length != width) {
                throw new IllegalArgumentException("보드 줄의 길이가 다릅니다.");
            }
            for (int value : row) {
                validateCell(value);
            }
            copy.add(row.clone());
        }
        return List.copyOf(copy);
    }

    private static void validateCell(int value) {
        if (value == Board.EMPTY_CELL || value == Board.GARBAGE_CELL) {
            return;
        }
        if (value < 0 || (Board.extractItemCode(value) != 0
                && ItemType.findTypeByCode(Board.extractItemCode(value)) == null)) {
            throw new IllegalArgumentException("알 수 없는 셀 값입니다.");
        }
        for (PieceType type : PieceType.values()) {
            if (type.getCellId() == Board.extractCellId(value)) {
                return;
            }
        }
        throw new IllegalArgumentException("알 수 없는 블록 값입니다.");
    }
}
