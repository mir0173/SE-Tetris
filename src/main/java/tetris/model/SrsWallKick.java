package tetris.model;

import java.util.List;

/** SRS 보정 좌표를 제공 */
final class SrsWallKick {
    // 0, R, 2, L 순서로 (0, 0)은 Game에서 먼저 검사
    private static final List<List<Position>> STANDARD_CLOCKWISE = List.of(
            List.of(offset(-1, 0), offset(-1, 1), offset(0, -2), offset(-1, -2)),
            List.of(offset(1, 0), offset(1, -1), offset(0, 2), offset(1, 2)),
            List.of(offset(1, 0), offset(1, 1), offset(0, -2), offset(1, -2)),
            List.of(offset(-1, 0), offset(-1, -1), offset(0, 2), offset(-1, 2))
    );
    private static final List<List<Position>> STANDARD_COUNTERCLOCKWISE = List.of(
            List.of(offset(1, 0), offset(1, 1), offset(0, -2), offset(1, -2)),
            List.of(offset(1, 0), offset(1, -1), offset(0, 2), offset(1, 2)),
            List.of(offset(-1, 0), offset(-1, 1), offset(0, -2), offset(-1, -2)),
            List.of(offset(-1, 0), offset(-1, -1), offset(0, 2), offset(-1, 2))
    );
    private static final List<List<Position>> I_CLOCKWISE = List.of(
            List.of(offset(-2, 0), offset(1, 0), offset(-2, -1), offset(1, 2)),
            List.of(offset(-1, 0), offset(2, 0), offset(-1, 2), offset(2, -1)),
            List.of(offset(2, 0), offset(-1, 0), offset(2, 1), offset(-1, -2)),
            List.of(offset(1, 0), offset(-2, 0), offset(1, -2), offset(-2, 1))
    );
    private static final List<List<Position>> I_COUNTERCLOCKWISE = List.of(
            List.of(offset(-1, 0), offset(2, 0), offset(-1, 2), offset(2, -1)),
            List.of(offset(2, 0), offset(-1, 0), offset(2, 1), offset(-1, -2)),
            List.of(offset(1, 0), offset(-2, 0), offset(1, -2), offset(-2, 1)),
            List.of(offset(-2, 0), offset(1, 0), offset(-2, -1), offset(1, 2))
    );

    private SrsWallKick() {
    }

    static List<Position> offsetsFor(PieceType type, int rotation, boolean clockwise) {
        return switch (type) {
            case I, SPECIAL_I -> (clockwise ? I_CLOCKWISE : I_COUNTERCLOCKWISE).get(rotation);
            case J, L, S, T, Z -> (clockwise ? STANDARD_CLOCKWISE : STANDARD_COUNTERCLOCKWISE).get(rotation);
            default -> List.of();
        };
    }

    private static Position offset(int x, int y) {
        return new Position(-y, x);
    }
}
