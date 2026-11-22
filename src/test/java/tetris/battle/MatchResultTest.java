package tetris.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MatchResultTest {

    // AI-assisted code start
    private static final long FIRST_SCORE = 100L;
    private static final long SECOND_SCORE = 50L;
    private static final long SWAPPED_FIRST_SCORE = 50L;
    private static final long SWAPPED_SECOND_SCORE = 100L;

    @ParameterizedTest // MatchResult를 좌우로 바꾸면 승자와 점수가 뒤집히고 종료 이유는 유지되는지 확인하는 테스트
    @EnumSource(MatchResult.EndReason.class)
    void swapSidesFlipsWinnerAndScores(MatchResult.EndReason reason) {
        // given: 1P 승리와 2P 승리 결과를 준비한다.
        MatchResult firstPlayerResult = new MatchResult(
                MatchResult.Winner.PLAYER_ONE, reason, FIRST_SCORE, SECOND_SCORE);
        MatchResult secondPlayerResult = new MatchResult(
                MatchResult.Winner.PLAYER_TWO, reason, FIRST_SCORE, SECOND_SCORE);

        // when: 양쪽 결과를 모두 뒤집는다.
        MatchResult swappedFirstPlayerResult = firstPlayerResult.swapSides();
        MatchResult swappedSecondPlayerResult = secondPlayerResult.swapSides();

        // then: 승자와 점수는 뒤집히고 종료 이유는 유지된다.
        assertEquals(MatchResult.Winner.PLAYER_TWO, swappedFirstPlayerResult.winner());
        assertEquals(reason, swappedFirstPlayerResult.reason());
        assertEquals(SWAPPED_FIRST_SCORE, swappedFirstPlayerResult.firstScore());
        assertEquals(SWAPPED_SECOND_SCORE, swappedFirstPlayerResult.secondScore());

        assertEquals(MatchResult.Winner.PLAYER_ONE, swappedSecondPlayerResult.winner());
        assertEquals(reason, swappedSecondPlayerResult.reason());
        assertEquals(SWAPPED_FIRST_SCORE, swappedSecondPlayerResult.firstScore());
        assertEquals(SWAPPED_SECOND_SCORE, swappedSecondPlayerResult.secondScore());
    }

    @Test // MatchResult의 무승부를 좌우로 바꾸면 무승부를 유지하고 점수만 뒤집는지 확인하는 테스트
    void swapSidesKeepsDraw() {
        // given: 무승부 결과와 양쪽 점수를 준비한다.
        MatchResult result = new MatchResult(
                MatchResult.Winner.DRAW, MatchResult.EndReason.GAME_OVER,
                FIRST_SCORE, SECOND_SCORE);

        // when: 양쪽을 뒤집는다.
        MatchResult swapped = result.swapSides();

        // then: 무승부는 유지되고 점수는 뒤집힌다.
        assertEquals(MatchResult.Winner.DRAW, swapped.winner());
        assertEquals(MatchResult.EndReason.GAME_OVER, swapped.reason());
        assertEquals(SWAPPED_FIRST_SCORE, swapped.firstScore());
        assertEquals(SWAPPED_SECOND_SCORE, swapped.secondScore());
    }

    @Test // MatchResult를 두 번 좌우로 바꾸면 원래 결과와 같아지는지 확인하는 테스트
    void swapSidesTwiceReturnsOriginal() {
        // given: 승자와 종료 이유 및 점수를 가진 결과를 준비한다.
        MatchResult result = new MatchResult(
                MatchResult.Winner.PLAYER_ONE, MatchResult.EndReason.STOPPED,
                FIRST_SCORE, SECOND_SCORE);

        // when: 양쪽을 두 번 뒤집는다.
        MatchResult swappedTwice = result.swapSides().swapSides();

        // then: 두 번 뒤집은 결과는 원래 결과와 같다.
        assertEquals(result, swappedTwice);
    }

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
