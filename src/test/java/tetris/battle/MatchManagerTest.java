package tetris.battle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

    @Test   // 한쪽이 만든 공격 줄이 상대에게만 전달되는지 확인하는 테스트
    void relayAttackRowsFromFirstToSecondAndFromSecondToFirst() {
        // given: 양쪽 플레이어가 각각 상대에게 보낼 공격 행을 하나씩 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        int[] firstRow = garbageRow(1);
        int[] secondRow = garbageRow(2);
        first.rowsToSend.add(firstRow);
        second.rowsToSend.add(secondRow);
        MatchManager manager = new MatchManager(first, second, result -> {
        });

        // when: 심판을 시작하고 1P의 갱신을 발생시킨다.
        manager.start(); // 양쪽 플레이어에 심판의 갱신 핸들러를 등록한다.
        first.onUpdate.run(); // 컨트롤러가 갱신을 알린 상황을 흉내 내며 양쪽을 함께 검사한다.

        // then: 각 공격 행은 자기 자신이 아니라 상대에게만 한 번씩 전달된다.
        assertEquals(EXPECTED_ONE_RECEIVE, second.receiveCalls);
        assertArrayEquals(firstRow, second.receivedRows.get(0));
        assertEquals(EXPECTED_ONE_RECEIVE, first.receiveCalls);
        assertArrayEquals(secondRow, first.receivedRows.get(0));
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-공격] 양쪽 공격 행이 상대에게만 각 1회 전달 PASS");
    }

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

    @Test   // 2P가 지면 1P가 이기고, 결과가 딱 한번 알려지고, 양쪽 게임이 동기화되어 멈추는지 확인하는 테스트
    void resolveSecondPlayerGameOver() {
        // given: 2P만 게임오버 상태인 경기를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        second.status = GameStatus.GAMEOVER;    // 2P 진 상황으로 세팅
        List<MatchResult> results = new ArrayList<>(); // 결과 값과 전달 횟수를 함께 확인하도록 모든 결과를 모은다.
        // 결과 값과 전달 횟수를 함께 확인하도록 모든 결과를 모은다.
        // 결과 값과 전달 횟수를 함께 확인하도록 모든 결과를 모은다.
        MatchManager manager = new MatchManager(first, second, results::add);

        // when: 심판을 시작하고 1P의 갱신을 발생시킨다.
        manager.start(); // 양쪽 플레이어에 심판의 갱신 핸들러를 등록한다.
        first.onUpdate.run(); // 1P의 갱신을 계기로 양쪽 게임오버 상태를 확인한다.

        assertEquals(List.of(MatchResult.PLAYER_ONE_WIN), results);
        assertEquals(EXPECTED_ONE_STOP, first.stopCalls);
        assertEquals(EXPECTED_ONE_STOP, second.stopCalls);
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-종료] 2P 게임오버 → 1P 승리, 결과 1회 전달, 양쪽 게임 정지 PASS");
    }

    @Test // 위의 테스트에서 1P <-> 2P 된 테스트
    void resolveFirstPlayerGameOver() {
        // given: 1P만 게임오버 상태인 경기를 준비한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        first.status = GameStatus.GAMEOVER;
        List<MatchResult> results = new ArrayList<>();
        // 결과 값과 전달 횟수를 함께 확인하도록 모든 결과를 모은다.
        // 결과 값과 전달 횟수를 함께 확인하도록 모든 결과를 모은다.
        MatchManager manager = new MatchManager(first, second, results::add);

        // when: 심판을 시작하고 2P의 갱신을 발생시킨다.
        manager.start(); // 양쪽 플레이어에 심판의 갱신 핸들러를 등록한다.
        second.onUpdate.run(); // 2P의 갱신을 계기로 양쪽 게임오버 상태를 확인한다.

        // then: 패배한 1P와 승리한 2P 모두 stop 호출을 한 번씩 받는다.
        assertEquals(List.of(MatchResult.PLAYER_TWO_WIN), results);
        assertEquals(EXPECTED_ONE_STOP, first.stopCalls);
        assertEquals(EXPECTED_ONE_STOP, second.stopCalls);
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-종료] 1P 게임오버 → 2P 승리, 결과 1회 전달, 양쪽 게임 정지 PASS");
    }

    // 제한 시간 모드에서 점수가 높은 쪽이 이기고, 같으면 무승부가 되는지 확인하는 테스트
    // ParameterizedTest + CsvSource로 서로 다른 파라미터의 테스트 3번 실행
    @ParameterizedTest 
    @CsvSource({"20, 10, PLAYER_ONE_WIN", "10, 20, PLAYER_TWO_WIN", "10, 10, DRAW"})
    void finishByTimeChoosesResult(long firstScore, long secondScore, MatchResult expected) {
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
        assertEquals(List.of(expected), results);
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-시간] 점수 " + firstScore + ":" + secondScore + " → " + expected + " PASS");
    }

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

    @Test // 상대가 줄 삭제 중이어도 일시정지한 플레이어를 재개시키지 않는지 확인하는 테스트
    void keepPausedWhileOpponentIsClearing() {
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
        second.onUpdate.run(); // 2P가 줄 삭제 중에 한번 더 갱신을 알린다.

        // then: 줄 삭제가 끝난 뒤 재개하는 동작은 T18에서 처리할 알려진 제한이므로,
        //       현재는 1P가 계속 일시정지 상태인지 확인한다.
        assertEquals(GameStatus.PAUSED, first.status);
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

    @Test // 경기가 끝난 뒤에는 추가 갱신이나 시간 종료가 결과와 공격 전달을 다시 발생시키지 않는지 확인하는 테스트
    void ignoreEverythingAfterMatchEnded() {
        // given: 1P 게임오버로 경기를 먼저 종료한다.
        FakePlayer first = new FakePlayer();
        FakePlayer second = new FakePlayer();
        List<MatchResult> results = new ArrayList<>(); // 결과 값과 전달 횟수를 함께 확인하도록 모든 결과를 모은다.
        // 결과 값과 전달 횟수를 함께 확인하도록 모든 결과를 모은다.
        // 결과 값과 전달 횟수를 함께 확인하도록 모든 결과를 모은다.
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
    }

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
        public void pause() {
            pauseCalls++;
            if (status == GameStatus.PLAYING) {
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
                status = GameStatus.PLAYING;
            }
            if (!isFinished()) {
                onUpdate.run();
            }
        }

        @Override
        public void stop() {
            stopCalls++;
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
