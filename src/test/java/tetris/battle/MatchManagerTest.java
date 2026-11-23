package tetris.battle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import tetris.model.GameSnapshot;
import tetris.model.GameStatus;

/**
 * 공격 전달, 경기 종료, 시간 판정과 일시정지 동기화 규칙을 검증한다.
 */
class MatchManagerTest {

    // AI-assisted code start
    // 가짜 플레이어와 공격 행 검증에 사용하는 공통 값
    private static final int BOARD_WIDTH = 10;
    private static final int EXPECTED_ONE_RECEIVE = 1;
    private static final int EXPECTED_ONE_STOP = 1;
    private static final int EXPECTED_NO_RECEIVE = 0;
    private static final int EXPECTED_ONE_RESULT = 1;
    private static final int EXPECTED_ONE_SYNC_CALL = 1;
    private static final int MIN_ATTACK_ROW_COUNT = 2;
    private static final int FIRST_ROW_VALUE = 1;
    private static final int SECOND_ROW_VALUE = 2;
    private static final int THIRD_ROW_VALUE = 3;
    private static final int FOURTH_ROW_VALUE = 4;
    private static final int BOUNDARY_ONE_ROW_VALUE = 1;
    private static final int BOUNDARY_TWO_ROW_A_VALUE = 2;
    private static final int BOUNDARY_TWO_ROW_B_VALUE = 3;
    private static final int DISCARDED_ROW_VALUE = 10;
    private static final int NEXT_ATTACK_ROW_A_VALUE = 20;
    private static final int NEXT_ATTACK_ROW_B_VALUE = 30;
    private static final int EXPECTED_NO_CALLS = 0;
    private static final long FIRST_GAME_OVER_SCORE = 300L;
    private static final long SECOND_GAME_OVER_SCORE = 500L;
    private static final long SCORE_BEFORE_STOP = 700L;
    private static final long SCORE_AFTER_STOP = 900L;
    private static final long FIRST_FORFEIT_SCORE = 1234L;
    private static final long SECOND_FORFEIT_SCORE = 5678L;

    // AI-assisted code start
    @Test // 1P가 기권하면 2P가 승리하고 기권 사유와 점수가 기록되는지 확인하는 테스트
    void firstPlayerForfeitMakesSecondPlayerWin() {
        // given: 점수가 다른 양쪽 플레이어와 기권 결과 기록자를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        first.score = FIRST_FORFEIT_SCORE;
        second.score = SECOND_FORFEIT_SCORE;
        List<MatchResult> results = new ArrayList<>();
        MatchManager manager = new MatchManager(first, second, results::add);

        // when: 1P의 기권을 심판에 알린다.
        manager.forfeit(first);

        // then: 2P 승리와 STOPPED 사유가 한 번 기록되고 양쪽이 정지된다.
        assertEquals(EXPECTED_ONE_RESULT, results.size());
        MatchResult result = results.get(0);
        assertEquals(MatchResult.Winner.PLAYER_TWO, result.winner());
        assertEquals(MatchResult.EndReason.STOPPED, result.reason());
        assertEquals(FIRST_FORFEIT_SCORE, result.firstScore());
        assertEquals(SECOND_FORFEIT_SCORE, result.secondScore());
        assertEquals(EXPECTED_ONE_STOP, first.stopCalls);
        assertEquals(EXPECTED_ONE_STOP, second.stopCalls);
        System.out.println("[REQ3-종료] 1P 기권 → 2P 승리, 이유 STOPPED PASS");
    }

    @Test // 2P가 기권하면 1P가 승리하는지 확인하는 테스트
    void secondPlayerForfeitMakesFirstPlayerWin() {
        // given: 기권 결과를 기록하는 심판과 양쪽 플레이어를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        List<MatchResult> results = new ArrayList<>();
        MatchManager manager = new MatchManager(first, second, results::add);

        // when: 2P의 기권을 심판에 알린다.
        manager.forfeit(second);

        // then: 1P 승리와 STOPPED 사유가 한 번 기록된다.
        assertEquals(EXPECTED_ONE_RESULT, results.size());
        MatchResult result = results.get(0);
        assertEquals(MatchResult.Winner.PLAYER_ONE, result.winner());
        assertEquals(MatchResult.EndReason.STOPPED, result.reason());
    }

    @Test // 알 수 없는 플레이어나 null이 기권할 때 거부되는지 확인하는 테스트
    void rejectForfeitOfUnknownPlayer() {
        // given: 심판과 경기 참여자가 아닌 가짜 플레이어를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        MatchManager manager = new MatchManager(first, second, result -> {
        });

        // when / then: 외부 플레이어와 null 기권 모두 예외를 발생시킨다.
        assertThrows(IllegalArgumentException.class, () -> manager.forfeit(new FakePlayer()));
        assertThrows(IllegalArgumentException.class, () -> manager.forfeit(null));
    }

    @Test // 경기 종료 후 기권을 해도 기존 결과만 유지되는지 확인하는 테스트
    void ignoreForfeitAfterMatchEnded() {
        // given: 1P 게임오버로 이미 끝난 경기와 결과 기록자를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        first.status = GameStatus.GAMEOVER;
        List<MatchResult> results = new ArrayList<>();
        MatchManager manager = new MatchManager(first, second, results::add);
        manager.start();

        // when: 1P 게임오버로 먼저 경기 종료한 뒤 1P 기권을 다시 알린다.
        first.onUpdate.run();
        manager.forfeit(first);

        // then: 기존 게임오버 결과만 정확히 한 번 남는다.
        assertEquals(EXPECTED_ONE_RESULT, results.size());
        assertEquals(MatchResult.EndReason.GAME_OVER, results.get(0).reason());
    }

    @Test // 결과를 판정하지 않는 심판이 기권을 무시하는지 확인하는 테스트
    void ignoreForfeitWhenNotDecidingResult() {
        // given: 결과를 판정하지 않는 심판과 양쪽 플레이어를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        List<MatchResult> results = new ArrayList<>();
        MatchManager manager = new MatchManager(first, second, results::add, false);

        // when: 2P의 기권을 심판에 알린다.
        manager.forfeit(second);

        // then: 결과가 없고 양쪽 모두 정지되지 않는다.
        assertEquals(EXPECTED_NO_CALLS, results.size());
        assertEquals(EXPECTED_NO_CALLS, first.stopCalls);
        assertEquals(EXPECTED_NO_CALLS, second.stopCalls);
    }

    @Test // 연결 끊김 무효로 양쪽만 정지하고 결과는 만들지 않는지 확인하는 테스트
    void cancelStopsBothWithoutResult() {
        // given: 경기를 기록하는 심판과 양쪽 플레이어를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        List<MatchResult> results = new ArrayList<>();
        MatchManager manager = new MatchManager(first, second, results::add);
        manager.start();

        // when: 경기를 무효로 취소하고 후속 갱신과 시간 종료를 시도한다.
        manager.cancel();
        second.status = GameStatus.GAMEOVER;
        second.onUpdate.run();
        manager.finishByTime();

        // then: 양쪽은 한 번씩 정지되고 결과는 전달되지 않는다.
        assertEquals(EXPECTED_ONE_STOP, first.stopCalls);
        assertEquals(EXPECTED_ONE_STOP, second.stopCalls);
        assertEquals(EXPECTED_NO_CALLS, results.size());
        System.out.println("[REQ3-종료] 연결 끊김 무효 → 양쪽 정지, 결과 없음 PASS");
    }

    @Test // 결과를 판정하지 않는 심판에서도 무효 취소가 양쪽을 정지하는지 확인하는 테스트
    void cancelWorksWhenNotDecidingResult() {
        // given: 결과를 판정하지 않는 심판과 양쪽 플레이어를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        List<MatchResult> results = new ArrayList<>();
        MatchManager manager = new MatchManager(first, second, results::add, false);
        manager.start();

        // when: 경기를 무효로 취소한다.
        manager.cancel();

        // then: 양쪽은 한 번씩 정지되고 결과는 전달되지 않는다.
        assertEquals(EXPECTED_ONE_STOP, first.stopCalls);
        assertEquals(EXPECTED_ONE_STOP, second.stopCalls);
        assertEquals(EXPECTED_NO_CALLS, results.size());
    }

    @Test // 이미 종료된 경기에서 무효 취소가 다시 정지하지 않는지 확인하는 테스트
    void ignoreCancelAfterMatchEnded() {
        // given: 1P 게임오버로 이미 끝난 경기와 결과 기록자를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        first.status = GameStatus.GAMEOVER;
        List<MatchResult> results = new ArrayList<>();
        MatchManager manager = new MatchManager(first, second, results::add);
        manager.start();

        // when: 1P 게임오버로 먼저 종료한 뒤 경기를 무효로 취소한다.
        first.onUpdate.run();
        manager.cancel();

        // then: 양쪽 정지 횟수는 1로 유지되고 기존 결과만 한 번 남는다.
        assertEquals(EXPECTED_ONE_STOP, first.stopCalls);
        assertEquals(EXPECTED_ONE_STOP, second.stopCalls);
        assertEquals(EXPECTED_ONE_RESULT, results.size());
    }
    // AI-assisted code end

    @Test // 결과를 판정하지 않는 심판이 갱신 공격 행을 상대에게 전달하는지 확인하는 테스트
    void relayAttacksWhenNotDecidingResult() {
        // given: 결과를 판정하지 않는 심판과 2줄 공격을 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        int[] firstRow = garbageRow(FIRST_ROW_VALUE);
        int[] secondRow = garbageRow(SECOND_ROW_VALUE);
        first.rowsToSend.add(firstRow);
        first.rowsToSend.add(secondRow);
        MatchManager manager = new MatchManager(first, second, result -> {
        }, false);

        // when: 심판을 시작하고 1P의 갱신을 발생시킨다.
        manager.start();
        first.onUpdate.run();

        // then: 2줄 공격은 2P에게 한 번 전달된다.
        assertEquals(EXPECTED_ONE_RECEIVE, second.receiveCalls);
        assertEquals(MIN_ATTACK_ROW_COUNT, second.receivedRows.size());
        assertArrayEquals(firstRow, second.receivedRows.get(0));
        assertArrayEquals(secondRow, second.receivedRows.get(1));
    }

    @Test // 결과를 판정하지 않는 심판이 게임오버를 판정하거나 정지하지 않는지 확인하는 테스트
    void doNotDecideResultWhenNotDeciding() {
        // given: 2P만 게임오버이고 결과를 판정하지 않는 심판을 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        second.status = GameStatus.GAMEOVER;
        List<MatchResult> results = new ArrayList<>();
        MatchManager manager = new MatchManager(first, second, results::add, false);

        // when: 심판을 시작하고 1P의 갱신을 발생시킨다.
        manager.start();
        first.onUpdate.run();

        // then: 결과도 전달되지 않고 양쪽 모두 정지되지 않는다.
        assertEquals(EXPECTED_NO_CALLS, results.size());
        assertEquals(EXPECTED_NO_CALLS, first.stopCalls);
        assertEquals(EXPECTED_NO_CALLS, second.stopCalls);
    }

    @Test // 결과를 판정하지 않는 심판이 시간 종료를 무시하는지 확인하는 테스트
    void ignoreFinishByTimeWhenNotDeciding() {
        // given: 결과를 판정하지 않는 심판과 결과 기록자를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        List<MatchResult> results = new ArrayList<>();
        MatchManager manager = new MatchManager(first, second, results::add, false);

        // when: 제한 시간이 끝났다고 알린다.
        manager.finishByTime();

        // then: 시간 종료로 결과가 전달되지 않는다.
        assertEquals(EXPECTED_NO_CALLS, results.size());
    }

    @Test // 결과를 판정하지 않는 심판이 일시정지를 동기화하지 않는지 확인하는 테스트
    void doNotSyncPauseWhenNotDeciding() {
        // given: 1P가 일시정지 상태이고 결과를 판정하지 않는 심판을 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        first.status = GameStatus.PAUSED;
        MatchManager manager = new MatchManager(first, second, result -> {
        }, false);

        // when: 심판을 시작하고 1P의 갱신을 발생시킨다.
        manager.start();
        first.onUpdate.run();

        // then: 2P는 일시정지되지 않는다.
        assertEquals(EXPECTED_NO_CALLS, second.pauseCalls);
    }

    @Test   // 양쪽이 만든 공격 행이 2줄일 때 상대에게만 전달되는지 확인하는 테스트
    void relayAttackRowsFromFirstToSecondAndFromSecondToFirst() {
        // given: 양쪽 플레이어가 각각 상대에게 보낼 공격 행을 두 개 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        int[] firstRowA = garbageRow(FIRST_ROW_VALUE);
        int[] firstRowB = garbageRow(SECOND_ROW_VALUE);
        int[] secondRowA = garbageRow(THIRD_ROW_VALUE);
        int[] secondRowB = garbageRow(FOURTH_ROW_VALUE);
        first.rowsToSend.add(firstRowA);
        first.rowsToSend.add(firstRowB);
        second.rowsToSend.add(secondRowA);
        second.rowsToSend.add(secondRowB);
        MatchManager manager = new MatchManager(first, second, result -> {
        });

        // when: 심판을 시작하고 1P의 갱신을 발생시킨다.
        manager.start(); // 양쪽 플레이어에 심판의 갱신 핸들러를 등록한다.
        first.onUpdate.run(); // 컨트롤러가 갱신을 알린 상황을 흉내 내며 양쪽을 함께 검사한다.

        // then: 각 공격 행은 자기 자신이 아니라 상대에게만 정확히 한 번씩 전달된다.
        assertEquals(EXPECTED_ONE_RECEIVE, second.receiveCalls);
        assertEquals(MIN_ATTACK_ROW_COUNT, second.receivedRows.size());
        assertArrayEquals(firstRowA, second.receivedRows.get(0));
        assertArrayEquals(firstRowB, second.receivedRows.get(1));
        assertEquals(EXPECTED_ONE_RECEIVE, first.receiveCalls);
        assertEquals(MIN_ATTACK_ROW_COUNT, first.receivedRows.size());
        assertArrayEquals(secondRowA, first.receivedRows.get(0));
        assertArrayEquals(secondRowB, first.receivedRows.get(1));
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-공격] 양쪽 공격 행이 상대에게만 2줄씩 각 1회 전달 PASS");
    }
    // AI-assisted code end

    // AI-assisted code start
    @Test // 1줄과 2줄 삭제가 공격 전달 경계를 나누는지 확인하는 테스트
    void relayOnlyWhenTwoOrMoreLinesCleared() {
        // given: 1P가 각각 1줄과 2줄을 보낼 수 있는 가짜 플레이어를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        MatchManager manager = new MatchManager(first, second, result -> {
        });
        manager.start();

        // when: 1P가 1줄을 보내고 갱신한다.
        first.rowsToSend.add(garbageRow(BOUNDARY_ONE_ROW_VALUE));
        first.onUpdate.run();

        // then: 1줄은 공격으로 전달되지 않는다.
        assertEquals(EXPECTED_NO_RECEIVE, second.receiveCalls);
        assertTrue(second.receivedRows.isEmpty());

        // given: 이어서 1P가 서로 다른 2줄을 준비한다.
        int[] rowA = garbageRow(BOUNDARY_TWO_ROW_A_VALUE);
        int[] rowB = garbageRow(BOUNDARY_TWO_ROW_B_VALUE);
        first.rowsToSend.add(rowA);
        first.rowsToSend.add(rowB);

        // when: 1P가 다시 갱신한다.
        first.onUpdate.run();

        // then: 2줄만 정확히 한 번 전달된다.
        assertEquals(EXPECTED_ONE_RECEIVE, second.receiveCalls);
        assertEquals(MIN_ATTACK_ROW_COUNT, second.receivedRows.size());
        assertArrayEquals(rowA, second.receivedRows.get(0));
        assertArrayEquals(rowB, second.receivedRows.get(1));
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-공격] 1줄 삭제 → 공격 없음, 2줄 삭제 → 2줄 전달 PASS");
    }
    // AI-assisted code end

    // AI-assisted code start
    @Test // 단일 줄 공격이 다음 공격과 혼합되지 않고 폐기되는지 확인하는 테스트
    void discardSingleLineSoItDoesNotMixIntoNextAttack() {
        // given: 1P가 값 A인 단일 줄을 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        int[] rowA = garbageRow(DISCARDED_ROW_VALUE);
        first.rowsToSend.add(rowA);
        MatchManager manager = new MatchManager(first, second, result -> {
        });
        manager.start();

        // when: 단일 줄로 갱신한다.
        first.onUpdate.run();

        // then: 단일 줄은 폐기되어 공격이 전달되지 않는다.
        assertEquals(EXPECTED_NO_RECEIVE, second.receiveCalls);

        // given: 다음 갱신에서 값 B와 C인 2줄을 준비한다.
        int[] rowB = garbageRow(NEXT_ATTACK_ROW_A_VALUE);
        int[] rowC = garbageRow(NEXT_ATTACK_ROW_B_VALUE);
        first.rowsToSend.add(rowB);
        first.rowsToSend.add(rowC);

        // when: 다시 갱신한다.
        first.onUpdate.run();

        // then: 다음 공격에는 B와 C만 순서대로 전달된다.
        assertEquals(EXPECTED_ONE_RECEIVE, second.receiveCalls);
        assertEquals(MIN_ATTACK_ROW_COUNT, second.receivedRows.size());
        assertArrayEquals(rowB, second.receivedRows.get(0));
        assertArrayEquals(rowC, second.receivedRows.get(1));
        for (int[] receivedRow : second.receivedRows) {
            assertTrue(receivedRow[0] != DISCARDED_ROW_VALUE);
        }
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-공격] 단일 줄은 폐기되고 다음 2줄만 전달 PASS");
    }
    // AI-assisted code end

    @Test   // 보낼 공격이 없을 때 상대에게 아무것도 넘기지 않는지 확인하는 테스트
    void doNotRelayWhenThereIsNothingToSend() {
        // given: 양쪽 모두 보낼 공격 행이 없는 상태를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        MatchManager manager = new MatchManager(first, second, result -> {
        });

        manager.start(); // 양쪽 플레이어에 심판의 갱신 핸들러를 등록한다.
        first.onUpdate.run(); // 한 번의 갱신으로 양쪽 플레이어의 공격 목록을 확인한다.

        // then: 빈 목록 전달도 놓치지 않도록 수신 호출 횟수 자체가 0인지 확인한다.
        assertEquals(EXPECTED_NO_RECEIVE, first.receiveCalls);
        assertEquals(EXPECTED_NO_RECEIVE, second.receiveCalls);
    }

    // AI-assisted code start
    @Test   // 2P가 지면 1P가 이기고, 결과가 딱 한번 알려지고, 양쪽 게임이 동기화되어 멈추는지 확인하는 테스트
    void resolveSecondPlayerGameOver() {
        // given: 2P만 게임오버 상태인 경기를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        first.score = FIRST_GAME_OVER_SCORE;
        second.score = SECOND_GAME_OVER_SCORE;
        second.status = GameStatus.GAMEOVER;    // 2P 진 상황으로 세팅
        List<MatchResult> results = new ArrayList<>(); // 결과 값과 전달 횟수를 함께 확인하도록 모든 결과를 모은다.
        MatchManager manager = new MatchManager(first, second, results::add);

        // when: 심판을 시작하고 1P의 갱신을 발생시킨다.
        manager.start(); // 양쪽 플레이어에 심판의 갱신 핸들러를 등록한다.
        first.onUpdate.run(); // 1P의 갱신을 계기로 양쪽 게임오버 상태를 확인한다.

        assertEquals(EXPECTED_ONE_RESULT, results.size());
        MatchResult result = results.get(0);
        assertEquals(MatchResult.Winner.PLAYER_ONE, result.winner());
        assertEquals(MatchResult.EndReason.GAME_OVER, result.reason());
        assertEquals(FIRST_GAME_OVER_SCORE, result.firstScore());
        assertEquals(SECOND_GAME_OVER_SCORE, result.secondScore());
        assertEquals(EXPECTED_ONE_STOP, first.stopCalls);
        assertEquals(EXPECTED_ONE_STOP, second.stopCalls);
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-종료] 2P 게임오버 → 1P 승리, 결과 1회 전달, 양쪽 게임 정지 PASS");
    }
    // AI-assisted code end

    // AI-assisted code start
    @Test // 위의 테스트에서 1P <-> 2P 된 테스트
    void resolveFirstPlayerGameOver() {
        // given: 1P만 게임오버 상태인 경기를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        first.score = FIRST_GAME_OVER_SCORE;
        second.score = SECOND_GAME_OVER_SCORE;
        first.status = GameStatus.GAMEOVER;
        List<MatchResult> results = new ArrayList<>();
        MatchManager manager = new MatchManager(first, second, results::add);

        // when: 심판을 시작하고 2P의 갱신을 발생시킨다.
        manager.start(); // 양쪽 플레이어에 심판의 갱신 핸들러를 등록한다.
        second.onUpdate.run(); // 2P의 갱신을 계기로 양쪽 게임오버 상태를 확인한다.

        // then: 패배한 1P와 승리한 2P 모두 stop 호출을 한 번씩 받는다.
        assertEquals(EXPECTED_ONE_RESULT, results.size());
        MatchResult result = results.get(0);
        assertEquals(MatchResult.Winner.PLAYER_TWO, result.winner());
        assertEquals(MatchResult.EndReason.GAME_OVER, result.reason());
        assertEquals(FIRST_GAME_OVER_SCORE, result.firstScore());
        assertEquals(SECOND_GAME_OVER_SCORE, result.secondScore());
        assertEquals(EXPECTED_ONE_STOP, first.stopCalls);
        assertEquals(EXPECTED_ONE_STOP, second.stopCalls);
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-종료] 1P 게임오버 → 2P 승리, 결과 1회 전달, 양쪽 게임 정지 PASS");
    }
    // AI-assisted code end

    // AI-assisted code start
    @ParameterizedTest
    @CsvSource({"true", "false"})
    void drawWhenBothPlayersAreGameOver(boolean firstTriggers) { // 양쪽이 동시에 게임오버일 때 어느 갱신으로든 무승부가 되는지 확인하는 테스트
        // given: 두 플레이어가 모두 게임오버 상태이고 결과를 모으는 심판을 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        first.status = GameStatus.GAMEOVER;
        second.status = GameStatus.GAMEOVER;
        List<MatchResult> results = new ArrayList<>();
        MatchManager manager = new MatchManager(first, second, results::add);
        manager.start();

        // when: 파라미터에 따라 1P 또는 2P의 갱신을 발생시킨다.
        if (firstTriggers) {
            first.onUpdate.run();
        } else {
            second.onUpdate.run();
        }

        // then: 양쪽 동시 게임오버는 무승부로 한 번 전달되고 양쪽이 정지된다.
        assertEquals(EXPECTED_ONE_RESULT, results.size());
        MatchResult result = results.get(0);
        assertEquals(MatchResult.Winner.DRAW, result.winner());
        assertEquals(MatchResult.EndReason.GAME_OVER, result.reason());
        assertEquals(EXPECTED_ONE_STOP, first.stopCalls);
        assertEquals(EXPECTED_ONE_STOP, second.stopCalls);
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-종료] 양쪽 동시 게임오버 → 무승부, 결과 1회 전달 PASS");
    }
    // AI-assisted code end

    // AI-assisted code start
    // 제한 시간 모드에서 점수가 높은 쪽이 이기고, 같으면 무승부가 되는지 확인하는 테스트
    // ParameterizedTest + CsvSource로 서로 다른 파라미터의 테스트 3번 실행
    @ParameterizedTest 
    @CsvSource({"20, 10, PLAYER_ONE", "10, 20, PLAYER_TWO", "10, 10, DRAW"})
    void finishByTimeChoosesResult(long firstScore, long secondScore, MatchResult.Winner expected) {
        // given: 파라미터로 두 플레이어의 점수와 기대 결과를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        first.score = firstScore;
        second.score = secondScore;
        List<MatchResult> results = new ArrayList<>();
        // 결과 값과 전달 횟수를 함께 확인하도록 모든 결과를 모은다.
        // 결과 값과 전달 횟수를 함께 확인하도록 모든 결과를 모은다.
        MatchManager manager = new MatchManager(first, second, results::add);

        // when: 제한 시간이 끝났다고 알린다.
        manager.finishByTime(); // 제한 시간이 끝난 상황에서 점수로 결과를 결정한다.

        // then: 파라미터에 맞는 결과가 한 번 전달된다.
        assertEquals(EXPECTED_ONE_RESULT, results.size());
        MatchResult result = results.get(0);
        assertEquals(expected, result.winner());
        assertEquals(MatchResult.EndReason.TIME_UP, result.reason());
        assertEquals(firstScore, result.firstScore());
        assertEquals(secondScore, result.secondScore());
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-시간] 점수 " + firstScore + ":" + secondScore + " → " + expected + " PASS");
    }
    // AI-assisted code end

    @Test // 한 플레이어가 일시정지하면 다른 플레이어도 함께 일시정지되는지 확인하는 테스트
    void synchronizePause() {
        // given: 두 플레이어가 진행 중이고 1P만 일시정지한 상태를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        MatchManager manager = new MatchManager(first, second, result -> {
        });

        manager.start(); // 양쪽 플레이어에 심판의 갱신 핸들러를 등록한다.
        first.status = GameStatus.PAUSED;
        first.onUpdate.run(); // 1P가 일시정지했다고 심판에게 알린다.

        // then: 심판이 2P에도 일시정지를 전파한다.
        assertEquals(GameStatus.PAUSED, first.status);
        assertEquals(GameStatus.PAUSED, second.status);
        assertEquals(EXPECTED_ONE_SYNC_CALL, first.pauseCalls);
        assertEquals(EXPECTED_ONE_SYNC_CALL, second.pauseCalls);
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-일시정지] 한 플레이어 일시정지 → 양쪽 PAUSED, 각 1회 호출 PASS");
    }

    @Test // 일시정지 중 한 플레이어가 재개하면 다른 플레이어도 함께 재개되는지 확인하는 테스트
    void synchronizeResume() {
        // given: 먼저 1P의 일시정지로 양쪽을 멈춘다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        MatchManager manager = new MatchManager(first, second, result -> {
        });

        manager.start(); // 양쪽 플레이어에 심판의 갱신 핸들러를 등록한다.
        first.status = GameStatus.PAUSED;
        first.onUpdate.run(); // 1P가 일시정지했다고 심판에게 알린다.
        first.status = GameStatus.PLAYING;
        first.onUpdate.run(); // 1P가 재개했다고 심판에게 알린다.

        // then: 심판이 2P에도 재개를 전파한다.
        assertEquals(GameStatus.PLAYING, first.status);
        assertEquals(GameStatus.PLAYING, second.status);
        assertEquals(EXPECTED_ONE_SYNC_CALL, first.resumeCalls);
        assertEquals(EXPECTED_ONE_SYNC_CALL, second.resumeCalls);
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-재개] 한 플레이어 재개 → 양쪽 PLAYING, 각 1회 호출 PASS");
    }

    @Test // 줄 삭제 중인 상대도 함께 멈추고 재개 시 연출 상태로 복귀하는지 확인하는 테스트
    void pauseClearingOpponentAndResumeToClearing() {
        // given: 1P는 일시정지하고 2P는 줄 삭제 중인 경기를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        MatchManager manager = new MatchManager(first, second, result -> {
        });
        manager.start(); // 양쪽 플레이어에 심판의 갱신 핸들러를 등록한다.
        first.status = GameStatus.PAUSED;
        second.status = GameStatus.CLEARING;

        // when: 1P의 갱신을 심판에게 알린다.
        first.onUpdate.run();

        // then: 줄 삭제 중이던 2P도 일시정지된다.
        assertEquals(GameStatus.PAUSED, first.status);
        assertEquals(GameStatus.PAUSED, second.status);

        // when: 1P가 재개 상태가 되어 갱신을 알리면 심판은 양쪽 재개를 동기화한다.
        first.status = GameStatus.PLAYING;
        first.onUpdate.run();

        // then: 1P는 PLAYING으로 복귀하고 2P는 이전 CLEARING 상태로 복귀한다.
        assertEquals(GameStatus.PLAYING, first.status);
        assertEquals(GameStatus.CLEARING, second.status);
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-일시정지] 줄 삭제 중인 상대도 함께 멈추고 재개 시 연출 상태로 복귀 PASS");
    }

    @Test // 한 플레이어가 CLEARING 상태로 재개될 때 다른 플레이어도 함께 재개되는지 확인하는 테스트
    void resumeBothWhenPlayerResumesIntoClearing() {
        // given: 양쪽을 일시정지하고, 1P의 일시정지 전 상태를 CLEARING으로 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        first.status = GameStatus.CLEARING;
        second.status = GameStatus.PAUSED;
        MatchManager manager = new MatchManager(first, second, result -> {
        });
        manager.start();
        first.onUpdate.run();
        assertEquals(GameStatus.PAUSED, first.status);
        assertEquals(GameStatus.PAUSED, second.status);

        // when: 1P가 줄 삭제 연출 상태 CLEARING으로 재개되어 갱신을 알린다.
        first.status = GameStatus.CLEARING;
        first.onUpdate.run();

        // then: 연출 상태가 아닌 줄 삭제 상태도 재개 신호로 보고 2P를 재개한다.
        assertEquals(GameStatus.CLEARING, first.status);
        assertEquals(GameStatus.PLAYING, second.status);
        assertEquals(EXPECTED_ONE_SYNC_CALL, first.resumeCalls);
        assertEquals(EXPECTED_ONE_SYNC_CALL, second.resumeCalls);
    }

    @Test // 일반 갱신과 상태 동기화 및 게임오버 처리에서 재귀 호출이 무한히 이어지지 않는지 확인하는 테스트
    void doNotOverflowStackDuringReentrantUpdates() {
        // given: 재진입 가능한 두 가짜 플레이어와 심판을 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        MatchManager manager = new MatchManager(first, second, result -> {
        });
        manager.start(); // 양쪽 플레이어에 심판의 갱신 핸들러를 등록한다.

        // when: 일반 갱신, 일시정지, 재개와 게임오버 갱신을 연속으로 발생시킨다.
        assertDoesNotThrow(() -> {
            first.onUpdate.run();
            first.status = GameStatus.PAUSED;
            first.onUpdate.run();
            first.status = GameStatus.PLAYING;
            first.onUpdate.run();
            second.status = GameStatus.GAMEOVER;
            first.onUpdate.run();
        });
        // then: 재진입이 반복되어도 StackOverflowError 없이 처리가 끝난다.
    }

    // AI-assisted code start
    @Test // 경기가 끝난 뒤에는 추가 갱신이나 시간 종료가 결과와 공격 전달을 다시 발생시키지 않는지 확인하는 테스트
    void ignoreEverythingAfterMatchEnded() {
        // given: 1P 게임오버로 경기를 먼저 종료한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        List<MatchResult> results = new ArrayList<>(); // 결과 값과 전달 횟수를 함께 확인하도록 모든 결과를 모은다.
        MatchManager manager = new MatchManager(first, second, results::add);
        manager.start(); // 양쪽 플레이어에 심판의 갱신 핸들러를 등록한다.
        first.status = GameStatus.GAMEOVER;
        first.onUpdate.run(); // 1P의 갱신을 계기로 경기 종료를 알린다.
        int receiveCalls = second.receiveCalls;
        first.rowsToSend.add(garbageRow(3));

        // when: 종료 뒤 공격 행을 추가하고 갱신과 시간 종료를 다시 시도한다.
        second.onUpdate.run(); // 종료된 컨트롤러에서 들어오는 후속 갱신을 흉내 낸다.
        manager.finishByTime(); // 이미 끝난 경기의 중복 결과 처리를 시도한다.

        // then: 수신 호출과 결과 전달 모두 추가로 발생하지 않는다.
        assertEquals(receiveCalls, second.receiveCalls);
        assertEquals(EXPECTED_ONE_RESULT, results.size());
        MatchResult result = results.get(0);
        assertEquals(MatchResult.Winner.PLAYER_TWO, result.winner());
        assertEquals(MatchResult.EndReason.GAME_OVER, result.reason());
    }
    // AI-assisted code end

    // AI-assisted code start
    @Test // 경기가 종료되어 정지될 때 정지 전 점수가 결과에 보존되는지 확인하는 테스트
    void reportScoresReadBeforeStopping() {
        // given: 양쪽 점수를 준비하고 정지 시 점수가 변경되도록 가짜 플레이어를 설정한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        first.score = SCORE_BEFORE_STOP;
        second.score = SECOND_GAME_OVER_SCORE;
        first.status = GameStatus.GAMEOVER;
        first.changeScoreOnStop = true;
        second.changeScoreOnStop = true;
        List<MatchResult> results = new ArrayList<>();
        MatchManager manager = new MatchManager(first, second, results::add);

        // when: 심판을 시작하고 1P의 게임오버 갱신을 발생시킨다.
        manager.start();
        first.onUpdate.run();

        // then: 두 결과 점수는 stop 호출로 변경되기 전 값이다.
        assertEquals(EXPECTED_ONE_RESULT, results.size());
        MatchResult result = results.get(0);
        assertEquals(SCORE_BEFORE_STOP, result.firstScore());
        assertEquals(SECOND_GAME_OVER_SCORE, result.secondScore());
        assertEquals(SCORE_AFTER_STOP, first.score);
        assertEquals(SCORE_AFTER_STOP, second.score);
    }
    // AI-assisted code end

    // 비교 가능한 공격 행 하나를 만든다.
    private int[] garbageRow(int value) {
        int[] row = new int[BOARD_WIDTH];
        row[0] = value;
        return row;
    }

    // LocalPlayer가 GameController.refresh()로 MatchManager에 재진입하는 동작을 흉내 낸다.
    private static class FakePlayer implements Player {
        private GameStatus status = GameStatus.PLAYING; // 현재 가짜 게임의 상태를 기록한다.
        private long score; // 시간 판정에 사용할 점수를 기록한다.
        private final List<int[]> rowsToSend = new ArrayList<>(); // 상대에게 보낼 공격 행을 기록한다.
        private final List<int[]> receivedRows = new ArrayList<>(); // 상대에게서 받은 공격 행을 기록한다.
        private int receiveCalls; // 빈 목록 전달도 놓치지 않도록 수신 메서드 호출 횟수를 기록한다.
        private Runnable onUpdate = () -> {
        }; // 심판이 다시 호출할 갱신 동작을 기록한다.
        private int stopCalls; // 이미 끝난 플레이어에 대한 호출까지 포함해 모든 stop 호출을 기록한다.
        private int pauseCalls; // 일시정지 동기화 호출 횟수를 기록한다.
        private int resumeCalls; // 재개 동기화 호출 횟수를 기록한다.
        private GameStatus statusBeforePause = GameStatus.PLAYING;
        private boolean changeScoreOnStop;

        @Override
        public GameSnapshot createSnapshot() {
            return new GameSnapshot(new int[0][0], List.of(), null, score, status, 0L,
                    null, null, List.of(), Map.of(), List.of());
        }

        @Override
        public List<int[]> takeAttackRows() {
            List<int[]> rows = new ArrayList<>();
            for (int[] row : rowsToSend) {
                rows.add(row.clone());
            }
            rowsToSend.clear();
            return rows;
        }

        @Override
        public void receiveGarbageRows(List<int[]> rows) {
            receiveCalls++;
            for (int[] row : rows) {
                receivedRows.add(row.clone());
            }
        }

        @Override
        public boolean isGameOver() {
            return status == GameStatus.GAMEOVER;
        }

        @Override
        public long getScore() {
            return score;
        }

        @Override
        public GameStatus getStatus() {
            return status;
        }

        @Override
        public void pause() {
            pauseCalls++;
            if (status == GameStatus.PLAYING || status == GameStatus.CLEARING) {
                statusBeforePause = status;
                status = GameStatus.PAUSED;
            }
            if (!isFinished()) {
                onUpdate.run();
            }
        }

        @Override
        public void resume() {
            resumeCalls++;
            if (status == GameStatus.PAUSED) {
                status = statusBeforePause;
            } else if (status == GameStatus.CLEARING) {
                status = statusBeforePause;
            }
            if (!isFinished()) {
                onUpdate.run();
            }
        }

        @Override
        public void stop() {
            stopCalls++;
            if (changeScoreOnStop) {
                score = SCORE_AFTER_STOP;
            }
            if (!isFinished()) {
                status = GameStatus.QUIT;
                onUpdate.run();
            }
        }

        // 게임오버 또는 종료 상태인지 판단한다.
        private boolean isFinished() {
            return status == GameStatus.GAMEOVER || status == GameStatus.QUIT;
        }

        @Override
        public void setOnUpdate(Runnable onUpdate) {
            this.onUpdate = onUpdate;
        }
    }
    // AI-assisted code end
}
