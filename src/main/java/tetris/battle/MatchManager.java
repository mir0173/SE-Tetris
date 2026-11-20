package tetris.battle;

import tetris.model.GameStatus;

import java.util.List;
import java.util.function.Consumer;
import static java.util.Objects.requireNonNull;

/**
 * 대전의 심판
 * 두 플레이어의 갱신 알림을 받아 공격 줄을 전달하고, 일시정지를 맞추고, 결과(승자, 끝난 이유, 점수)를 알린다.
 */
public class MatchManager {
    private static final int MIN_ATTACK_LINES = 2;

    private final Player firstPlayer;
    private final Player secondPlayer;

    private final Consumer<MatchResult> onMatchEnd;
    private boolean finished;
    private boolean paused;
    private boolean syncing;

    public MatchManager(Player firstPlayer, Player secondPlayer, Consumer<MatchResult> onMatchEnd) {
        requireNonNull(firstPlayer);
        requireNonNull(secondPlayer);
        requireNonNull(onMatchEnd);

        this.firstPlayer = firstPlayer;
        this.secondPlayer = secondPlayer;
        this.onMatchEnd = onMatchEnd;
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
        if (finished) {
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

    private void handleUpdate() {
        if (finished || syncing) {
            return;
        }

        List<int[]> firstAttackRows = firstPlayer.takeAttackRows();
        List<int[]> secondAttackRows = secondPlayer.takeAttackRows();

        // 블록 하나로 지운 줄이 MIN_ATTACK_LINES 이상일 때만 상대에게 보낸다 (L 아이템으로 지운 줄 포함)
        // 블록이 고정될 때마다 갱신 알림이 오므로, 한 번에 꺼낸 줄 = 블록 하나로 지운 줄
        if (firstAttackRows.size() >= MIN_ATTACK_LINES) {
            secondPlayer.receiveGarbageRows(firstAttackRows);
        }

        if (secondAttackRows.size() >= MIN_ATTACK_LINES) {
            firstPlayer.receiveGarbageRows(secondAttackRows);
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

    private void finish(MatchResult.Winner winner, MatchResult.EndReason reason) {
        finished = true;
        long firstScore = firstPlayer.getScore();
        long secondScore = secondPlayer.getScore();
        firstPlayer.stop();
        secondPlayer.stop();

        MatchResult result = new MatchResult(winner, reason, firstScore, secondScore);
        onMatchEnd.accept(result);
    }
}