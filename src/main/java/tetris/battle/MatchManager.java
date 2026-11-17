package tetris.battle;

import tetris.model.GameStatus;

import java.util.List;
import java.util.function.Consumer;

public class MatchManager {
    private final Player firstPlayer;
    private final Player secondPlayer;

    private final Consumer<MatchResult> onMatchEnd;
    private boolean finished;
    private boolean paused;
    private boolean syncing;

    public MatchManager(Player firstPlayer, Player secondPlayer, Consumer<MatchResult> onMatchEnd) {
        this.firstPlayer = firstPlayer;
        this.secondPlayer = secondPlayer;
        this.onMatchEnd = onMatchEnd;
    }

    public void start() {
        firstPlayer.setOnUpdate(() -> handleUpdate());
        secondPlayer.setOnUpdate(() -> handleUpdate());
    }

    public void finishByTime() {
        MatchResult result;
        if (finished) {
            return;
        }

        if (firstPlayer.getScore() > secondPlayer.getScore()) {
            result = MatchResult.PLAYER_ONE_WIN;
        } else if (firstPlayer.getScore() < secondPlayer.getScore()) {
            result = MatchResult.PLAYER_TWO_WIN;
        } else {
            result = MatchResult.DRAW;
        }
        finish(result);
    }

    private void handleUpdate() {
        if (finished || syncing) {
            return;
        }

        List<int[]> AttackRows_1P = firstPlayer.takeAttackRows();
        List<int[]> AttackRows_2P = secondPlayer.takeAttackRows();

        // T18 : 2줄 이상일 때만 공격, 10줄 제한
        if (!AttackRows_1P.isEmpty()) {
            secondPlayer.receiveGarbageRows(AttackRows_1P);
        }

        if (!AttackRows_2P.isEmpty()) {
            firstPlayer.receiveGarbageRows(AttackRows_2P);
        }

        if (firstPlayer.isGameOver()) {
            finish(MatchResult.PLAYER_TWO_WIN);
            return;
        } else if (secondPlayer.isGameOver()) {
            finish(MatchResult.PLAYER_ONE_WIN);
            return;
        }
        
        // 게임 일시정지, 재개 동기화
        GameStatus status_1P = firstPlayer.createSnapshot().getStatus();
        GameStatus status_2P = secondPlayer.createSnapshot().getStatus();

        if (!paused) {
            if (status_1P == GameStatus.PAUSED || status_2P == GameStatus.PAUSED) {
                syncing = true;
                paused = true;
                firstPlayer.pause();
                secondPlayer.pause();
                syncing = false;
            }
            syncing = false;
        } else if(paused) {
            if(status_1P == GameStatus.PLAYING || status_2P == GameStatus.PLAYING) {
                syncing = true;
                paused = false;
                firstPlayer.resume();
                secondPlayer.resume();
                syncing = false;
            }
        }

    }

    private void finish(MatchResult result) {
        finished = true;
        firstPlayer.stop();
        secondPlayer.stop();
        onMatchEnd.accept(result);
    }
}