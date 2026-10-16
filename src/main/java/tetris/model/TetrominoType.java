package tetris.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 테트리스에서 사용하는 7가지 테트로미노와 아이템 블록의 종류와 회전 정보를 제공한다.
 * 각 모양은 정사각형 상자 안의 상대 좌표로 표현되며, row는 위에서 아래로,
 * column은 왼쪽에서 오른쪽으로 증가한다.
 */
public enum TetrominoType {
    // AI-assisted code start
    I(1, true, true, new int[][] {
            { 0, 0, 0, 0 },
            { 1, 1, 1, 1 },
            { 0, 0, 0, 0 },
            { 0, 0, 0, 0 }
    }),
    O(2, true, true, new int[][] {
            { 1, 1 },
            { 1, 1 }
    }),
    T(3, true, true, new int[][] {
            { 1, 1, 1 },
            { 0, 1, 0 },
            { 0, 0, 0 }
    }),
    S(4, true, true, new int[][] {
            { 0, 1, 1 },
            { 1, 1, 0 },
            { 0, 0, 0 }
    }),
    Z(5, true, true, new int[][] {
            { 1, 1, 0 },
            { 0, 1, 1 },
            { 0, 0, 0 }
    }),
    J(6, true, true, new int[][] {
            { 1, 0, 0 },
            { 1, 1, 1 },
            { 0, 0, 0 }
    }),
    L(7, true, true, new int[][] {
            { 0, 0, 1 },
            { 1, 1, 1 },
            { 0, 0, 0 }
    }),
    // AI-assisted code end
    SLOW(8, false,true, new int[][] {
            {1}
    }),
    HEAVY_WEIGHT(9, false, false, new int[][] {
            { 0, 0, 0, 0 },
            { 0, 1, 1, 0 },
            { 1, 1, 1, 1 },
            { 0, 0, 0, 0 }
    }),
    SPECIAL_BLOCK(12,false,true, new int[][]{
            { 1 , 1 },
            { 0 , 0 }

    }),
    SPECIAL_I(11, false, true, new int[][] {
            { 0, 0, 0, 0 },
            { 1, 1, 1, 1 },
            { 0, 0, 0, 0 },
            { 0, 0, 0, 0 }
    });    

    /** 블록의 회전 상태 수. */
    private static final int ROTATION_COUNT = 4;
    /** 모양 배열에서 채워진 칸을 나타내는 값. */
    private static final int FILLED = 1;
    /** 보드에 고정할 때 사용하는 블록 식별자. */
    private final int cellId;
    /** 블록 모양을 포함하는 정사각형 상자의 한 변 크기. */
    private final int boxSize;
    /** 블록이 기본 블록인지 아이템인지 확인하는 이진 변수 */
    private final boolean basic;
    /** 블록이 회전 가능한 블록인지 확인하는 이진 변수 */
    private final boolean rotatable;
    /** 회전 상태별 블록의 상대 좌표 목록. */
    private final List<List<Position>> rotations;

    /**
     * 블록 타입과 스폰 모양을 초기화하고 4가지 회전 상태를 계산한다.
     *
     * @param cellId 보드 셀에 저장할 블록 식별자
     * @param shape 스폰 상태의 블록 모양 배열
     */
    TetrominoType(int cellId, boolean basic, boolean rotatable, int[][] shape) {
        this.cellId = cellId;
        this.boxSize = shape.length;
        this.basic = basic;
        this.rotatable = rotatable;
        this.rotations = createRotations(boxSize, createPositions(shape));
    }

    /**
     * 보드 셀에 저장할 블록 식별자를 반환한다.
     *
     * @return 블록 식별자
     */
    public int getCellId() {
        return cellId;
    }

    /**
     * 블록 모양을 포함하는 정사각형 상자의 크기를 반환한다.
     *
     * @return 상자의 한 변 크기
     */
    public int getBoxSize() {
        return boxSize;
    }
    /**
    * 일반 블록인지 아이템인지 여부를 반환한다.
    * 
    * @return 일반 블록 여부 boolean값 
    */
    public boolean isBasic() {
        return basic;
    }
    /**
     * 회전 가능한 블럭인지 여부를 반환한다.
     * 
     * @return 회전 가능 여부 boolean 값
     */
    public boolean canRotate() {
        return rotatable;
    }

    /**
     * 각 rotation 순서에 따른 Position 정보를 제공한다.
        * rotation = 0 일시 스폰 모양이고, 1씩 증가할 때 마다 시계방향 90° 회전한 모양.
        * 반환되는 좌표는 블록 위치를 기준으로 한 상대 좌표다.
        *
     * @param rotation 조회할 회전 상태 (0부터 3까지)
        * @return 해당 회전 상태의 상대 좌표 목록
     * @throws IllegalArgumentException rotation이 0보다 작거나 3보다 큰 경우
     */
    public List<Position> getCellsAt(int rotation) {
        if (rotation < 0 || rotation >= ROTATION_COUNT) {
            throw new IllegalArgumentException("rotation must be 0~3: " + rotation);
        }
        return rotations.get(rotation);
    }

    /**
     * 모양 배열을 채워진 셀의 상대 좌표 목록으로 변환한다.
     *
     * @param shape 변환할 블록 모양 배열
     * @return 채워진 셀의 상대 좌표 목록
     */
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

    /**
     * 스폰 모양에서 시계 방향으로 90도씩 회전한 상태를 계산한다.
     *
     * @param boxSize 블록 모양을 포함하는 상자의 크기
     * @param positions 스폰 상태의 상대 좌표 목록
     * @return 회전 상태별 상대 좌표 목록
     */
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