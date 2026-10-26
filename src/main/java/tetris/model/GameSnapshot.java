package tetris.model;

import java.util.List;

public class GameSnapshot {

    private final int[][] cells;
    private final List<PieceType> nextPieces;
    private final PieceType heldPiece; // 홀드가 없으면 null
    private final long score;
    private final GameStatus status;
    private final long dropInterval;
    private final Item nextPieceItem; // 아이템이 없으면 null
    private final Item heldPieceItem; // 아이템이 없으면 null
    private final List<Integer> clearingRows; // 삭제 대기 중인 줄 번호, 없으면 빈 목록
    private final List<Position> ghostPiecePositions;

    public GameSnapshot(
            int[][] cells,
            List<PieceType> nextPieces,
            PieceType heldPiece,
            long score,
            GameStatus status,
            long dropInterval,
            Item nextPieceItem,
            Item heldPieceItem,
            List<Integer> clearingRows,
            List<Position> ghostPiecePositions) {
        this.cells = copyCells(cells);
        this.nextPieces = List.copyOf(nextPieces);
        this.heldPiece = heldPiece;
        this.score = score;
        this.status = status;
        this.dropInterval = dropInterval;
        this.nextPieceItem = nextPieceItem;
        this.heldPieceItem = heldPieceItem;
        this.clearingRows = List.copyOf(clearingRows);
        this.ghostPiecePositions = List.copyOf(ghostPiecePositions);
    }

    public int[][] getCells() {
        return copyCells(cells);
    }

    public List<PieceType> getNextPieces() {
        return nextPieces;
    }

    /**
     * 아이템이 없을 시 null 반환 가능
     */
    public Item getNextPieceItem() {
        return nextPieceItem;
    }

    /**
     * hold된 블록이 없으면 null 반환 가능
     */
    public PieceType getHeldPiece() {
        return heldPiece;
    }

    /**
     * hold된 블록에 아이템이 없으면 null 반환 가능
     */
    public Item getHeldPieceItem() {
        return heldPieceItem;
    }

    /**
     * 삭제 대기 중인 줄 번호, 없으면 빈 목록 반환
     */
    public List<Integer> getClearingRows() {
        return clearingRows;
    }

    public long getScore() {
        return score;
    }

    public GameStatus getStatus() {
        return status;
    }

    public long getDropInterval() {
        return dropInterval;
    }

    public List<Position> getGhostPiecePositions() {
        return ghostPiecePositions;
    }

    private int[][] copyCells(int[][] source) {
        int[][] copy = new int[source.length][];

        for (int row = 0; row < source.length; row++) {
            copy[row] = source[row].clone();
        }

        return copy;
    }
}