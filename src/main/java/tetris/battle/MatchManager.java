package tetris.battle;

import tetris.model.GameKind;
import tetris.model.GameStatus;
import tetris.controller.GameTimer;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import static java.util.Objects.requireNonNull;

/**
 * 대전의 심판
 * 두 플레이어의 갱신 알림을 받아 공격 줄을 전달하고, 일시정지를 맞추고, 결과(승자, 끝난 이유, 점수)를 알린다.
 */
public class MatchManager {
    private static final int MIN_ATTACK_LINES = 2;
    private static final long TIME_LIMIT_TICK_MILLIS = 1_000L; // 제한 시간을 1초 단위로 샌다.

    private final Player firstPlayer;
    private final Player secondPlayer;
    private final Consumer<MatchResult> onMatchEnd;
    private final boolean decidesResult; // false일 시 공격 전달만, 승패는 판정하지 않는다. (P2P CLIENT)

    private GameTimer timeLimitTimer; // 제한시간용 타이머, 시간제한 모드가 아니면 null
    private int remainingSeconds; // 남은 시간
    private IntConsumer onRemainingSeconds = seconds -> {}; // 남은 시간이 바뀔 때 마다 알릴 동작
    private boolean finished;
    private boolean paused;
    private boolean syncing;

    /**
     * 심판을 만든다.
     *
     * @param firstPlayer   1P (P2P에서는 나)
     * @param secondPlayer  2P (P2P에서는 상대)
     * @param onMatchEnd    경기가 끝났을 때 결과를 받을 동작. 결과를 정하는 심판에서만 불린다
     * @param decidesResult true면 승패를 판정해 결과를 알리고, false면 공격 전달만 한다 (P2P CLIENT)
     */
    public MatchManager(Player firstPlayer, Player secondPlayer, Consumer<MatchResult> onMatchEnd,
            boolean decidesResult) {
        requireNonNull(firstPlayer);
        requireNonNull(secondPlayer);
        requireNonNull(onMatchEnd);
        if (firstPlayer.gameKind() != secondPlayer.gameKind()) {
            throw new IllegalArgumentException("같은 게임끼리만 대전할 수 있습니다.");
        }

        this.firstPlayer = firstPlayer;
        this.secondPlayer = secondPlayer;
        this.onMatchEnd = onMatchEnd;
        this.decidesResult = decidesResult;
    }

    /**
     * 결과를 판정하는 심판을 만든다. (decidesResult가 true인 생성자와 같다)
     *
     * @param firstPlayer  1P
     * @param secondPlayer 2P
     * @param onMatchEnd   경기가 끝났을 때 결과를 받을 동작
     */
    public MatchManager(Player firstPlayer, Player secondPlayer, Consumer<MatchResult> onMatchEnd) {
        this(firstPlayer, secondPlayer, onMatchEnd, true);
    }

    /**
     * 두 플레이어에 갱신 알림을 등록해 경기를 지켜보기 시작한다. 게임 시작은 바깥에서 한다.
     */
    public void start() {
        firstPlayer.setOnUpdate(() -> handleUpdate());
        secondPlayer.setOnUpdate(() -> handleUpdate());
    }

    /**
     * 제한 시간이 끝났을 때 부른다. 점수가 높은 쪽이 이기고 같으면 무승부, 이미 끝났으면 무시한다.
     */
    public void finishByTime() {
        if (finished || !decidesResult) {
            return;
        }
        MatchResult.Winner winner;
        if (firstPlayer.getScore() > secondPlayer.getScore()) {
            winner = MatchResult.Winner.PLAYER_ONE;
        } else if (firstPlayer.getScore() < secondPlayer.getScore()) {
            winner = MatchResult.Winner.PLAYER_TWO;
        } else {
            winner = MatchResult.Winner.DRAW;
        }
        finish(winner, MatchResult.EndReason.TIME_UP);
    }

    /**
     * 플레이어가 경기를 나갔을 때(기권) 부른다.
     * 나간 쪽이 지고 이유는 STOPPED. 결과를 정하지 않는 심판이거나 이미 끝났다면 이 메서드를 무시한다.
     * 
     * @param leaver 경기를 나간 플레이어(1P 또는 2P)
     * @throws IllegalArgumentException 두 플레이어중 하나가 아닐 때
     */
    public void forfeit(Player leaver) {
        if (leaver != firstPlayer && leaver != secondPlayer) {
            throw new IllegalArgumentException("leaver must be one of the two players");
        }

        if (finished || !decidesResult) {
            return;
        }

        MatchResult.Winner winner;
        if (leaver == firstPlayer) {
            winner = MatchResult.Winner.PLAYER_TWO;
        } else {
            winner = MatchResult.Winner.PLAYER_ONE;
        }
        finish(winner, MatchResult.EndReason.STOPPED);
    }

    /**
     * 경기를 무효로 끝낸다.
     * 예 : 메시지 없이 연결 끊김
     * 두 게임을 멈추지만 결과는 알리지 않는다.
     */
    public void cancel() {
        if (finished) {
            return;
        }

        finished = true;
        if (timeLimitTimer != null) {
            timeLimitTimer.stop();
        }
        firstPlayer.stop();
        secondPlayer.stop();
    }

    /**
     * 제한 시간 모드에서 시간을 재기 시작한다. 1초마다 남은 시간을 알리고, 0이 되면 finishByTime()으로 결과를 정한다.
     * 일시정지 중에는 시간이 줄지 않는다.
     * P2P CLIENT에서는 남은 시간 표시만 하고 경기는 HOST가 끝낸다.
     * 
     * @param timer               1초마다 시간을 잴 타이머
     * @param seconds             제한시간(초), 1 이상
     * @param onRemainingSeconds 남은 시간이 바뀔 때마다 받을 동작. 시작할 때 한 번 바로 불림.
     * 
     * @throws NullPointerException     timer나 onRemainingSeconds가 null일 때
     * @throws IllegalArgumentException seconds가 1보다 작을 때
     * @throws IllegalStateException    이미 제한시간을 시작했을 때
     */
    public void startTimeLimit(GameTimer timer, int seconds, IntConsumer onRemainingSeconds) {
        // 예외 처리
        requireNonNull(timer);
        requireNonNull(onRemainingSeconds);
        if (seconds < 1) {
            throw new IllegalArgumentException("seconds is lower than 1");
        }
        if (timeLimitTimer != null) {
            throw new IllegalStateException("timeLimitTimer is already started");
        }

        // 게임 종료시 반환
        if (finished) {
            return;
        }

        // 필드 저장
        timeLimitTimer = timer;
        remainingSeconds = seconds;
        this.onRemainingSeconds = onRemainingSeconds;

        // 시작하자마자 한 번 알림 : 화면에 60초를 띄우기 위함
        onRemainingSeconds.accept(remainingSeconds);

        timer.setInterval(TIME_LIMIT_TICK_MILLIS);
        timer.start(this::tickTimeLimit);

    }

    /**
     * 플레이어의 상태가 바뀔 때마다 불린다 (각 플레이어의 갱신 알림).
     * 공격 줄 전달 → (결과를 정하는 심판이면) 승패 판정 → 일시정지·재개 맞추기 순서로 처리한다.
     * 경기가 끝났거나 일시정지를 맞추는 중(syncing)에 다시 불리면 무시한다.
     */
    private void handleUpdate() {
        if (finished || syncing) {
            return;
        }

        List<int[]> firstAttackRows = firstPlayer.takeAttackRows();
        List<int[]> secondAttackRows = secondPlayer.takeAttackRows();

        sendAttack(firstPlayer, secondPlayer, firstAttackRows);
        sendAttack(secondPlayer, firstPlayer, secondAttackRows);

        // 결과를 정하지 않는 P2P CLIENT의 심판은 공격 전달만 한다.
        // P2P에는 일시정지가 없어 아래 동기화도 필요 없다.
        if (!decidesResult) {
            return;
        }

        // 승패 판정
        // 동시에 게임 오버일 시 무승부
        if (firstPlayer.isGameOver() && secondPlayer.isGameOver()) {
            finish(MatchResult.Winner.DRAW, MatchResult.EndReason.GAME_OVER);
            return;
        }

        if (firstPlayer.isGameOver()) {
            finish(MatchResult.Winner.PLAYER_TWO, MatchResult.EndReason.GAME_OVER);
            return;
        }

        if (secondPlayer.isGameOver()) {
            finish(MatchResult.Winner.PLAYER_ONE, MatchResult.EndReason.GAME_OVER);
            return;
        }

        // 게임 일시정지, 재개 동기화
        GameStatus firstStatus = firstPlayer.getStatus();
        GameStatus secondStatus = secondPlayer.getStatus();

        if (!paused) {
            if (firstStatus == GameStatus.PAUSED || secondStatus == GameStatus.PAUSED) {
                syncing = true;
                paused = true;
                firstPlayer.pause();
                secondPlayer.pause();
                syncing = false;
            }
        } else {
            // 이제 진행 중인 플레이어는 연출 중에도 멈추므로, 멈춘 동안 PAUSED가 아니면 누군가 재개한 것
            if (firstStatus != GameStatus.PAUSED || secondStatus != GameStatus.PAUSED) {
                syncing = true;
                paused = false;
                firstPlayer.resume();
                secondPlayer.resume();
                syncing = false;
            }
        }

    }

    private void sendAttack(Player sender, Player receiver, List<int[]> attackRows) {
        GameKind source = sender.gameKind();
        int minimum = source == GameKind.TETRIS ? MIN_ATTACK_LINES : 1;
        if (attackRows.size() >= minimum) {
            receiver.receiveGarbageRows(attackRows);
        }
    }

    // 경기를 끝낸다: 시계를 멈추고, 멈추기 전 점수를 읽은 뒤 두 게임을 멈추고, 결과를 한 번 알린다.
    // finished를 가장 먼저 true로 두어, stop()에서 되돌아오는 갱신 알림이 무시되게 한다.
    private void finish(MatchResult.Winner winner, MatchResult.EndReason reason) {
        finished = true;
        if (timeLimitTimer != null) {
            timeLimitTimer.stop();
        }
        long firstScore = firstPlayer.getScore();
        long secondScore = secondPlayer.getScore();
        firstPlayer.stop();
        secondPlayer.stop();

        MatchResult result = new MatchResult(winner, reason, firstScore, secondScore);
        onMatchEnd.accept(result);
    }

    //  1초마다 불린다. 일시정지 중에는 시간을 줄이지 않고, 0이 되면 시간 종료로 끝낸다.
    private void tickTimeLimit() {
        if (finished) {
            timeLimitTimer.stop();
            return;
        }

        if (paused) {
            return;
        }

        remainingSeconds--;
        onRemainingSeconds.accept(remainingSeconds);
        if (remainingSeconds <= 0) {
            timeLimitTimer.stop();
            finishByTime();
        }
    }
}