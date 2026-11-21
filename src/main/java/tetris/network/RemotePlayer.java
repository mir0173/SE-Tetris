package tetris.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import tetris.battle.Player;
import tetris.model.GameSnapshot;
import tetris.model.GameStatus;

/** 상대 PC의 상태를 보관한다.*/
public final class RemotePlayer implements Player {
    private final BattleLink link;
    private final Consumer<GameSnapshot> onSnapshot;
    private final int width;
    private final int height;
    private final List<int[]> attacks = new ArrayList<>();
    private GameSnapshot snapshot;
    private Runnable onUpdate = () -> { };

    public RemotePlayer(BattleLink link, int width, int height, Consumer<GameSnapshot> onSnapshot) {
        this.link = Objects.requireNonNull(link, "link");
        this.onSnapshot = Objects.requireNonNull(onSnapshot, "onSnapshot");
        this.width = width;
        this.height = height;
        snapshot = new GameSnapshot(new int[height][width], List.of(), null, 0, GameStatus.READY,
                1_000, null, null, List.of(), Map.of(), List.of());
    }

    public void accept(BattleMessage message) {
        switch (message.type()) {
            case STATE -> {
                int[][] cells = message.state().cells();
                if (cells.length != height || cells[0].length != width) {
                    throw new IllegalArgumentException("상대 보드 크기가 경기 설정과 다릅니다.");
                }
                snapshot = message.state().toSnapshot();
                onSnapshot.accept(snapshot);
                onUpdate.run();
            }
            case ATTACK -> {
                if (message.width() != snapshot.getCells()[0].length) {
                    throw new IllegalArgumentException("상대 공격 줄 폭이 보드와 다릅니다.");
                }
                attacks.addAll(message.attackRows());
                onUpdate.run();
            }
            case STOP -> {} // 실제 정지 처리는 경기 컨트롤러가 담당
        }
    }

    @Override public GameSnapshot createSnapshot() { return snapshot; }

    @Override
    public List<int[]> takeAttackRows() {
        List<int[]> result = attacks.stream().map(int[]::clone).toList();
        attacks.clear();
        return result;
    }

    @Override public void receiveGarbageRows(List<int[]> rows) { link.sendAttackRows(rows); }
    @Override public boolean isGameOver() { return snapshot.getStatus() == GameStatus.GAMEOVER; }
    @Override public long getScore() { return snapshot.getScore(); }
    @Override public void pause() {} // P2P에서는 로컬 일시정지를 허용하지 않음 
    @Override public void resume() {} // P2P에서는 로컬 일시정지를 허용하지 않음 
    @Override public void stop() { link.sendStop(); }
    @Override public void setOnUpdate(Runnable onUpdate) { this.onUpdate = Objects.requireNonNull(onUpdate); }
}
