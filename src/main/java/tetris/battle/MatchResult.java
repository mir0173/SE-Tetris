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
        STOPPED
    }

    public MatchResult {
        requireNonNull(winner);
        requireNonNull(reason);
    }
}
