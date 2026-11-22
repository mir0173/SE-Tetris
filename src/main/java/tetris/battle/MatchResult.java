package tetris.battle;

import static java.util.Objects.requireNonNull;

/** 대전 결과. 승자, 끝난 이유, 경기가 끝난 순간의 양쪽 점수를 담는다. */
public record MatchResult(Winner winner, EndReason reason, long firstScore, long secondScore) {
    /** 경기 종료시 승자, 또는 승부 결과를 나타낸다. */
    public enum Winner {
        PLAYER_ONE,
        PLAYER_TWO,
        DRAW
    }

    /** 매치 종료시 종료 이유를 나타낸다. */
    public enum EndReason {
        GAME_OVER,
        TIME_UP,
        STOPPED // 경기 중단(로컬) 또는 기권(P2P)
    }

    public MatchResult {
        requireNonNull(winner);
        requireNonNull(reason);
    }

    /**
     * 1P와 2P를 바꾼 결과를 돌려준다.
     * P2P에서 상대가 보낸 결과(상대 기준)를 내 기준으로 볼 때 사용한다. 끝난 이유는 그대로 둔다.
     */
    public MatchResult swapSides() {
        // 승자 뒤집기
        Winner swappedWinner = switch (winner()) {
            case PLAYER_ONE -> Winner.PLAYER_TWO;
            case PLAYER_TWO -> Winner.PLAYER_ONE;
            case DRAW -> Winner.DRAW;
        };

        // 점수 순서가 다른 새 MatchResult 반환 (firstScore <-> SecondScore)
        return new MatchResult(swappedWinner, reason(), secondScore(), firstScore());
    }
}
