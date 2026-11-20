package tetris.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MatchResultTest {

    // AI-assisted code start
    @Test // MatchResult가 승자, 종료 이유와 양쪽 점수를 모두 보존하는지 확인하는 테스트
    void keepsAllResultValues() {
        // given: 네 개의 결과 값을 준비한다.
        MatchResult.Winner winner = MatchResult.Winner.PLAYER_ONE;
        MatchResult.EndReason reason = MatchResult.EndReason.TIME_UP;
        long firstScore = 120L;
        long secondScore = 80L;

        // when: MatchResult를 생성한다.
        MatchResult result = new MatchResult(winner, reason, firstScore, secondScore);

        // then: 모든 값이 생성 시 입력한 값 그대로 보존된다.
        assertEquals(winner, result.winner());
        assertEquals(reason, result.reason());
        assertEquals(firstScore, result.firstScore());
        assertEquals(secondScore, result.secondScore());
    }

    @Test // MatchResult의 승자가 null이면 예외가 발생하는지 확인하는 테스트
    void rejectsNullWinner() {
        // given: 승자 없이 생성할 입력을 준비한다.
        MatchResult.EndReason reason = MatchResult.EndReason.STOPPED;

        // when / then: null 승자를 생성하면 NullPointerException이 발생한다.
        assertThrows(NullPointerException.class,
                () -> new MatchResult(null, reason, 0L, 0L));
    }

    @Test // MatchResult의 종료 이유가 null이면 예외가 발생하는지 확인하는 테스트
    void rejectsNullReason() {
        // given: 종료 이유 없이 생성할 입력을 준비한다.
        MatchResult.Winner winner = MatchResult.Winner.DRAW;

        // when / then: null 종료 이유로 생성하면 NullPointerException이 발생한다.
        assertThrows(NullPointerException.class,
                () -> new MatchResult(winner, null, 0L, 0L));
    }
    // AI-assisted code end
}
