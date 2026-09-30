package tetris.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 보드 위에서 떨어지는 블록. 불변 객체라서 이동, 회전 시 새 객체를 반환한다.
 * 
 * @param type     블록 종류
 * @param rotation 회전 상태 (0~3, 0은 스폰 모양)
 * @param row      shape 배열 (0,0)의 보드 행 좌표
 * @param column   shape 배열 (0,0)의 보드 열 좌표
 */
public record Tetromino(TetrominoType type, int rotation, int row, int column) {

    private static final int SPAWN_ROTATION = 0;
    private static final int SPAWN_ROW = 0;
    private static final int ROTATION_COUNT = 4;

    // type이 null인지, rotation이 0~3 범위인지 검사한다.
    public Tetromino {
        if (type == null) {
            throw new NullPointerException("Tetromino Type is null");
        }

        if (rotation >= ROTATION_COUNT || rotation < 0) {
            throw new IllegalArgumentException("rotation must be 0~3: " + rotation);
        }
    }

    // 보드 맨 위 가운데에 스폰 모양(rotation 0)으로 블록을 생성하는 함수
    public static Tetromino spawn(TetrominoType type, int boardWidth) {
        int column = (boardWidth - type.getBoxSize()) / 2;
        return new Tetromino(type, SPAWN_ROTATION, SPAWN_ROW, column);
    }

    // (dRow, dCol)만큼 이동한 새 블록을 반환하는 함수
    public Tetromino move(int dRow, int dCol) {
        return new Tetromino(type, rotation, dRow + row, dCol + column);
    }

    // 시계방향으로 회전된 새 블록 객체를 반환하는 함수
    public Tetromino rotateClockwise() {
        return new Tetromino(type, (rotation + 1) % ROTATION_COUNT, row, column);
    }

    // 반시계방향으로 회전된 새 블록 객체를 반환하는 함수
    public Tetromino rotateCounterClockwise() {
        return new Tetromino(type, (rotation + ROTATION_COUNT - 1) % ROTATION_COUNT, row, column);
    }

    // 상대 좌표들을 보드 기준 절대 좌표 List로 반환하는 함수
    public List<Position> getAbsolutePositions() {
        List<Position> relativePositions = type.getCellsAt(rotation);
        List<Position> absolutePositions = new ArrayList<>();
        for (Position relativePos : relativePositions) {
            Position absolutePos = new Position(relativePos.row() + row, relativePos.column() + column);
            absolutePositions.add(absolutePos);
        }
        return List.copyOf(absolutePositions);
    }
}