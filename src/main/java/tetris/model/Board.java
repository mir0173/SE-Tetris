package tetris.model;

import java.util.List;

/**
 * 떨어지는 블록이 고정된 게임 보드를 관리한다.
 * 좌표는 위에서 아래로 증가하는 row와 왼쪽에서 오른쪽으로 증가하는 column을 사용한다.
 */
public class Board {

    /** 빈 칸을 나타내는 셀 값. */
    public static final int EMPTY_CELL = 0;

    /** 보드의 가로 칸 수. */
    private final int width;
    /** 보드의 세로 칸 수. */
    private final int height;
    /** 행과 열 순서로 저장된 고정 블록 셀. 빈 칸은 EMPTY_CELL, 채워진 칸은 TetrominoType의 cellId */
    private final int[][] cells;

    /**
     * 지정한 크기의 빈 보드를 생성한다.
     *
     * @param width 보드의 가로 칸 수
     * @param height 보드의 세로 칸 수
     * @throws IllegalArgumentException width 또는 height가 0 이하인 경우
     */
    public Board(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("보드의 width, height가 0 이하로 설정");
        }
        this.width = width;
        this.height = height;
        this.cells = new int[height][width];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * 지정한 칸의 셀 값을 반환한다.
     *
     * @param row 조회할 행
     * @param column 조회할 열
     * @return 해당 칸의 셀 값
     * @throws ArrayIndexOutOfBoundsException 좌표가 보드 범위를 벗어난 경우
     */
    public int getCell(int row, int column) {
        return cells[row][column];
    }

    /**
     * 지정한 칸이 비어 있는지 확인한다.
     *
     * @param row 확인할 행
     * @param column 확인할 열
     * @return 해당 칸이 비어 있으면 true
     * @throws ArrayIndexOutOfBoundsException 좌표가 보드 범위를 벗어난 경우
     */
    public boolean isEmpty(int row, int column) {
        return cells[row][column] == EMPTY_CELL;
    }

    /**
     * 보드 셀의 깊은 복사본을 반환한다.
     * 반환된 배열을 수정해도 보드 내부 상태는 바뀌지 않는다.
     *
     * @return 보드 셀의 복사본
     */
    public int[][] copyCells() {
        int[][] copy = new int[height][width];

        for (int row = 0; row < height; row++) {
            System.arraycopy(cells[row], 0, copy[row], 0, width);
        }

        return copy;
    }

    /**
     * 좌표가 보드 내부에 포함되는지 확인한다.
     *
     * @param row 확인할 행
     * @param column 확인할 열
     * @return 좌표가 보드 범위 안에 있으면 true
     */
    public boolean isInside(int row, int column) {
        boolean isRowInside = ((row < height) && (row >= 0));
        boolean isColumnInside = ((column < width) && (column >= 0));
        return isRowInside && isColumnInside;
    }

    /**
     * 떨어지는 블록의 모든 칸을 현재 보드에 배치할 수 있는지 확인한다.
     * 블록의 모든 칸이 보드 안에 있고 빈 칸이어야 한다.
     *
     * @param block 배치 가능 여부를 확인할 떨어지는 블록
     * @return 블록을 배치할 수 있으면 true
     */
    public boolean canPlace(Tetromino block) {
        List<Position> positions = block.getAbsolutePositions();
        for (Position pos : positions) {
            if (!isInside(pos.row(), pos.column()) ||
                    !isEmpty(pos.row(),pos.column())) {
                return false;
            }
        }
        return true;
    }
}