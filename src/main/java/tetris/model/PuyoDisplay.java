package tetris.model;

import java.util.List;
import java.util.Set;

/** 뿌요 화면과 P2P 상태 전송에 필요한 추가 정보. */
public record PuyoDisplay(List<PairColors> nextPairs, Set<Position> clearing, int chain, int pendingGarbage) {
    public static final int COLOR_COUNT = 4;
    public static final int MAX_PENDING_GARBAGE = 60;

    public record PairColors(int pivot, int child) {
        public PairColors {
            if (pivot < 1 || pivot > COLOR_COUNT || child < 1 || child > COLOR_COUNT) {
                throw new IllegalArgumentException("잘못된 뿌요 색상입니다.");
            }
        }
    }

    public PuyoDisplay {
        nextPairs = List.copyOf(nextPairs);
        clearing = Set.copyOf(clearing);
        if (nextPairs.size() > 3 || chain < 0 || chain > 18
                || pendingGarbage < 0 || pendingGarbage > MAX_PENDING_GARBAGE) {
            throw new IllegalArgumentException("뿌요 미리보기 정보가 범위를 벗어났습니다.");
        }
        for (Position position : clearing) {
            if (position.row() < 0 || position.row() >= GameKind.PUYO.height()
                    || position.column() < 0 || position.column() >= GameKind.PUYO.width()) {
                throw new IllegalArgumentException("삭제할 뿌요 좌표가 보드 범위를 벗어났습니다.");
            }
        }
    }
}
