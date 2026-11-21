package tetris.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import tetris.controller.GameTimer;
import tetris.model.*;
import tetris.view.GameView;

// AI-assisted code start
class P2pBattleControllerTest {
    @Test
    void publishNewestStateAndPreserveMatchObserver() {
        Fixture fixture = new Fixture();
        fixture.controller.start();
        int[] updates = {0};
        fixture.controller.getLocalPlayer().setOnUpdate(() -> updates[0]++);
        fixture.view.command.accept(GameCommand.MOVE_LEFT);
        fixture.view.command.accept(GameCommand.HARD_DROP);
        assertTrue(updates[0] >= 2);
        assertEquals(1, fixture.outgoing.size());
        fixture.stateTimer.fire();
        assertEquals(2, fixture.outgoing.size());
        assertTrue(fixture.outgoing.get(1).state().score() > 0);
        fixture.stateTimer.fire();
        assertEquals(2, fixture.outgoing.size());
        fixture.controller.close();
    }

    @Test
    void ignorePauseAndUseQuitToRequestLobbyReturn() {
        Fixture fixture = new Fixture();
        fixture.controller.start();
        fixture.view.command.accept(GameCommand.PAUSE);
        assertEquals(GameStatus.PLAYING, fixture.game.createSnapshot().getStatus());
        fixture.view.command.accept(GameCommand.QUIT_GAME);
        assertEquals(1, fixture.returnCalls);
        assertEquals(GameStatus.PLAYING, fixture.game.createSnapshot().getStatus());
        fixture.controller.close();
    }

    @Test
    void renderIncomingStateWithoutEchoingItOrApplyingUnfinishedAttackRules() throws Exception {
        Fixture fixture = new Fixture();
        fixture.controller.start();
        fixture.link.receive(BattleMessage.state(1, BattleStateData.fromSnapshot(BattleMessageTest.snapshot(777))));
        fixture.link.receive(BattleMessage.attack(2, List.of(new int[10], new int[10])));
        fixture.stateTimer.fire();
        assertEquals(List.of(777L), fixture.remoteScores);
        assertEquals(1, fixture.outgoing.size());
        assertEquals(0, fixture.game.getPendingGarbageRowCount());
        assertEquals(2, fixture.controller.getRemotePlayer().takeAttackRows().size());
        fixture.controller.close();
        System.out.println("[T20 분리] 상대 상태는 표시만 수행·상태 재전송 없음·공격 규칙 임의 적용 없음 PASS");
    }

    @Test
    void stopTimersAndIgnoreInputsAfterLeaving() {
        Fixture fixture = new Fixture();
        fixture.controller.start();
        fixture.controller.close();
        int sent = fixture.outgoing.size();
        fixture.view.command.accept(GameCommand.HARD_DROP);
        fixture.stateTimer.fire();
        assertEquals(sent, fixture.outgoing.size());
        assertEquals(GameStatus.QUIT, fixture.game.createSnapshot().getStatus());
        assertTrue(fixture.stateTimer.stopped);
        assertTrue(fixture.dropTimer.stopped);
    }

    private static final class Fixture {
        private final Game game = new Game(new Board(10, 20), new TetrominoGenerator(new Random(42)),
                Difficulty.NORMAL, GameMode.NORMAL);
        private final FakeView view = new FakeView();
        private final FakeTimer dropTimer = new FakeTimer();
        private final FakeTimer stateTimer = new FakeTimer();
        private final List<BattleMessage> outgoing = new ArrayList<>();
        private final List<Long> remoteScores = new ArrayList<>();
        private final BattleLink link = new BattleLink(Runnable::run, outgoing::add);
        private int returnCalls;
        private final P2pBattleController controller = new P2pBattleController(game, view, dropTimer,
                (delay, task) -> { }, stateTimer, link, state -> remoteScores.add(state.getScore()),
                text -> { }, () -> returnCalls++);
    }

    private static final class FakeView implements GameView {
        private Consumer<GameCommand> command;
        @Override public void setCommandHandler(Consumer<GameCommand> command) { this.command = command; }
        @Override public void render(GameSnapshot snapshot) { }
        @Override public void show() { }
        @Override public void close() { }
    }

    private static final class FakeTimer implements GameTimer {
        private Runnable action = () -> { };
        private boolean stopped;
        @Override public void start(Runnable action) { this.action = action; stopped = false; }
        @Override public void stop() { stopped = true; }
        @Override public void setInterval(long intervalMillis) { }
        private void fire() { action.run(); }
    }
}
// AI-assisted code end
