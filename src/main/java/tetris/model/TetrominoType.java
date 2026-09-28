package tetris.model;

import java.util.ArrayList;
import java.util.List;

public enum TetrominoType {
    // AI-assisted code start
    I(1, new int[][] {
            { 0, 0, 0, 0 },
            { 1, 1, 1, 1 },
            { 0, 0, 0, 0 },
            { 0, 0, 0, 0 }
    }),
    O(2, new int[][] {
            { 1, 1 },
            { 1, 1 }
    }),
    T(3, new int[][] {
            { 1, 1, 1 },
            { 0, 1, 0 },
            { 0, 0, 0 }
    }),
    S(4, new int[][] {
            { 0, 1, 1 },
            { 1, 1, 0 },
            { 0, 0, 0 }
    }),
    Z(5, new int[][] {
            { 1, 1, 0 },
            { 0, 1, 1 },
            { 0, 0, 0 }
    }),
    J(6, new int[][] {
            { 1, 0, 0 },
            { 1, 1, 1 },
            { 0, 0, 0 }
    }),
    L(7, new int[][] {
            { 0, 0, 1 },
            { 1, 1, 1 },
            { 0, 0, 0 }
    });
    // AI-assisted code end

    private static final int ROTATION_COUNT = 4;
    private static final int FILLED = 1;
    private final int cellId;
    private final int boxSize;
    private final List<List<Position>> rotations;

    TetrominoType(int cellId, int[][] shape) {
        this.cellId = cellId;
        boxSize = shape.length;
        rotations = createRotations(boxSize, createPositions(shape));
    }

    public int getCellId() {
        return cellId;
    }

    public int getBoxSize() {
        return boxSize;
    }

    /**
     * 각 rotation 순서에 따른 Position 정보를 제공
     * rotation = 0 일시 스폰 모양이고, 1씩 증가할 때 마다 시계방향 90° 회전한 모양
     * rotation이 범위를 벗어날 시 예외 처리
     */
    public List<Position> getCellsAt(int rotation) {
        if (rotation < 0 || rotation >= ROTATION_COUNT) {
            throw new IllegalArgumentException("rotation must be 0~3: " + rotation);
        }
        return rotations.get(rotation);
    }

    // enum shape 배열을 각 cell의 상대 좌표 List로 변환
    private static List<Position> createPositions(int[][] shape) {
        List<Position> positions = new ArrayList<>();
        for (int i = 0; i < shape.length; i++) {
            for (int j = 0; j < shape.length; j++) {
                if (shape[i][j] == FILLED) {
                    positions.add(new Position(i, j));
                }
            }
        }
        return positions;
    }

    // 스폰 모양에서 시계 방향으로 90°씩 회전한 4가지 상대 좌표 List 계산 -> 2차원 List로 반환
    private static List<List<Position>> createRotations(int boxSize, List<Position> positions) {
        List<List<Position>> rotations = new ArrayList<>();
        List<Position> prevState, currentState;
        rotations.add(List.copyOf(positions));
        prevState = positions;
        for (int i = 0; i < ROTATION_COUNT - 1; i++) {
            currentState = new ArrayList<>();
            for (Position pos : prevState) {
                Position newPos = new Position(pos.column(), boxSize - 1 - pos.row());
                currentState.add(newPos);
            }
            rotations.add(List.copyOf(currentState));
            prevState = currentState;
        }
        return List.copyOf(rotations);
    }
}