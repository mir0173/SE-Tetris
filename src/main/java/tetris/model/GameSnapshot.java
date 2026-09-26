package tetris.model;
import java.util.List;

public class GameSnapshot {

    private final int[][] cells;
    private final List<TetrominoType> nextPieces;
    private final long score;
    private final GameStatus status;
    private final long dropInterval;

    public GameSnapshot(int[][] cells, List<TetrominoType> nextPieces, long score, GameStatus status, long dropInterval) {
        this.cells = copyCells(cells);
        this.nextPieces = List.copyOf(nextPieces);
        this.score = score;
        this.status = status;
        this.dropInterval = dropInterval;
    }

    public int[][] getCells() {
        return copyCells(cells);
    }

    public List<TetrominoType> getNextPieces() {
        return nextPieces;
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

    private int[][] copyCells(int[][] source) {
        int[][] copy = new int[source.length][];

        for (int row = 0; row < source.length; row++) {
            copy[row] = source[row].clone();
        }

        return copy;
    }
}