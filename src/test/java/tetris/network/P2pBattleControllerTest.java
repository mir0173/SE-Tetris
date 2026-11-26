package tetris.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import tetris.battle.BattleMode;
import tetris.battle.BattleSettings;
import tetris.battle.MatchResult;
import tetris.controller.GameTimer;
import tetris.model.*;
import tetris.puyo.PuyoGame;
import tetris.view.GameView;

// AI-assisted code start
class P2pBattleControllerTest {
    @Test
    void publishNewestStateOnlyOncePerUpdate() {
        Fixture fixture = new Fixture();
        fixture.controller.start();
        fixture.view.command.accept(GameCommand.MOVE_LEFT);
        fixture.view.command.accept(GameCommand.HARD_DROP);
        assertEquals(1, fixture.outgoing.size());
        fixture.stateTimer.fire();
        assertEquals(2, fixture.outgoing.size());
        assertTrue(fixture.outgoing.get(1).state().score() > 0);
        fixture.stateTimer.fire();
        assertEquals(2, fixture.outgoing.size());
        fixture.controller.close();
    }

    @Test
    void ignorePauseAndUseQuitToForfeitBeforeReturning() throws Exception {
        Fixture fixture = new Fixture();
        fixture.controller.start();
        fixture.view.command.accept(GameCommand.PAUSE);
        assertEquals(GameStatus.PLAYING, fixture.game.createSnapshot().getStatus());
        fixture.view.command.accept(GameCommand.QUIT_GAME);
        assertEquals(0, fixture.returnCalls);
        assertEquals(GameStatus.QUIT, fixture.game.getStatus());
        assertEquals(new MatchResult(MatchResult.Winner.PLAYER_TWO, MatchResult.EndReason.STOPPED, 0, 0),
                fixture.results.getFirst());
        assertEquals(BattleMessage.Type.RESULT, fixture.outgoing.getLast().type());
        fixture.view.command.accept(GameCommand.QUIT_GAME);
        assertEquals(0, fixture.returnCalls);
        fixture.link.receive(BattleMessage.control(BattleMessage.Type.RESULT_ACK, 1));
        assertEquals(1, fixture.returnCalls);
        fixture.link.receive(BattleMessage.control(BattleMessage.Type.RESULT_ACK, 2));
        assertEquals(1, fixture.returnCalls);
        fixture.controller.close();
    }

    @Test
    void renderIncomingStateAndApplyAttackWithTheSharedRules() throws Exception {
        Fixture fixture = new Fixture();
        fixture.controller.start();
        fixture.link.receive(BattleMessage.state(1, BattleStateData.fromSnapshot(BattleMessageTest.snapshot(777))));
        fixture.stateTimer.fire();
        assertEquals(List.of(777L), fixture.remoteScores);
        assertEquals(1, fixture.outgoing.size());
        fixture.link.receive(BattleMessage.attack(2, List.of(new int[10], new int[10])));
        assertEquals(2, fixture.game.getPendingGarbageRowCount());
        assertEquals(2, fixture.view.lastSnapshot.getPendingGarbageRows().size());
        fixture.stateTimer.fire();
        assertEquals(List.of(BattleMessage.Type.STATE, BattleMessage.Type.STATE),
                fixture.outgoing.stream().map(BattleMessage::type).toList());
        assertEquals(2, fixture.outgoing.getLast().state().pendingRows().size());
        fixture.controller.close();
        System.out.println("[T22 공격 수신] 상대 상태 재전송 없음·내 공격 대기열 즉시 표시 및 동기화 PASS");
    }

    @Test
    void clientForfeitsOnceThenUsesOnlyHostResult() throws Exception {
        Fixture fixture = new Fixture(PeerRole.CLIENT, 0);
        fixture.controller.start();
        fixture.controller.requestReturnToLobby();
        fixture.controller.requestReturnToLobby();
        assertEquals(List.of(BattleMessage.Type.STATE, BattleMessage.Type.FORFEIT),
                fixture.outgoing.stream().map(BattleMessage::type).toList());
        assertTrue(fixture.results.isEmpty());
        assertEquals(0, fixture.returnCalls);

        MatchResult hostResult = new MatchResult(MatchResult.Winner.PLAYER_ONE,
                MatchResult.EndReason.STOPPED, 30, 10);
        fixture.link.receive(BattleMessage.result(1, hostResult));
        fixture.link.receive(BattleMessage.result(2, hostResult));
        fixture.link.receive(BattleMessage.state(3, BattleStateData.fromSnapshot(BattleMessageTest.snapshot(90))));
        assertEquals(List.of(hostResult.swapSides()), fixture.results);
        assertTrue(fixture.remoteScores.isEmpty());
        assertEquals(BattleMessage.Type.RESULT_ACK, fixture.outgoing.getLast().type());
        assertEquals(3, fixture.outgoing.size());
        fixture.controller.requestReturnToLobby();
        assertEquals(1, fixture.returnCalls);
        System.out.println("[T20 기권] CLIENT 1회 요청·HOST 결과로 확정·후속 상태 무시 PASS");
    }

    @Test
    void hostAloneTimesTheMatchAndSendsScoreBasedResult() throws Exception {
        Fixture host = new Fixture(PeerRole.HOST, 10);
        Fixture client = new Fixture(PeerRole.CLIENT, 10);
        host.controller.start();
        client.controller.start();
        host.link.receive(BattleMessage.state(1, BattleStateData.fromSnapshot(BattleMessageTest.snapshot(50))));
        assertEquals(List.of(10), host.times);
        assertTrue(client.times.isEmpty());
        assertFalse(client.timeLimitTimer.started);
        assertTrue(host.timeLimitTimer.started);

        for (int second = 0; second < 10; second++) {
            host.timeLimitTimer.fire();
        }
        int timeMessages = host.times.size();
        List<BattleMessage> messages = host.outgoing.stream()
                .filter(message -> message.type() == BattleMessage.Type.TIME_LEFT
                        || message.type() == BattleMessage.Type.RESULT)
                .toList();
        for (BattleMessage message : messages) {
            client.link.receive(message);
        }
        MatchResult expected = new MatchResult(MatchResult.Winner.PLAYER_TWO,
                MatchResult.EndReason.TIME_UP, 0, 50);
        assertEquals(List.of(expected), host.results);
        assertEquals(List.of(expected.swapSides()), client.results);
        assertEquals(11, timeMessages);
        assertEquals(0, host.times.getLast());
        assertEquals(host.times, client.times);
        assertTrue(host.timeLimitTimer.stopped);
        assertTrue(client.dropTimer.stopped);
        System.out.printf("[T20 제한시간] HOST/CLIENT 시간 %s / 점수 0:50에서 HOST 패배 PASS%n", host.times);
    }

    @Test
    void stopTimersAndIgnoreInputsAfterLeaving() {
        Fixture fixture = new Fixture(PeerRole.HOST, 10);
        fixture.controller.start();
        fixture.controller.close();
        int sent = fixture.outgoing.size();
        fixture.view.command.accept(GameCommand.HARD_DROP);
        fixture.stateTimer.fire();
        assertEquals(sent, fixture.outgoing.size());
        assertEquals(GameStatus.QUIT, fixture.game.createSnapshot().getStatus());
        assertTrue(fixture.stateTimer.stopped);
        assertTrue(fixture.dropTimer.stopped);
        assertTrue(fixture.timeLimitTimer.stopped);
        assertTrue(fixture.results.isEmpty());
    }

    @Test
    void applySinglePuyoAttackPacketWithoutTetrisLineMinimum() throws Exception {
        PuyoGame game = new PuyoGame(1);
        FakeView view = new FakeView();
        List<BattleMessage> outgoing = new ArrayList<>();
        BattleLink link = new BattleLink(Runnable::run, outgoing::add);
        BattleSettings settings = new BattleSettings(BattleMode.NORMAL, Difficulty.NORMAL, 0, GameKind.PUYO);
        P2pBattleController controller = new P2pBattleController(game, view, new FakeTimer(), (delay, action) -> { },
                new FakeTimer(), new FakeTimer(), link, new MatchStart(1, PeerRole.HOST, settings),
                snapshot -> { }, text -> { }, result -> { }, seconds -> { }, () -> { });
        controller.start();
        try {
            int[] packet = {Board.GARBAGE_CELL, Board.GARBAGE_CELL, 0, 0, 0, 0};
            link.receive(BattleMessage.attack(1, List.of(packet)));
            assertEquals(2, game.getPendingGarbageRowCount());
            assertEquals(2, view.lastSnapshot.getPuyo().pendingGarbage());
            assertNotNull(outgoing.getFirst().state().puyo());
            assertEquals(6, outgoing.getFirst().state().cells()[0].length);
        } finally {
            controller.close();
        }
    }

    private static final class Fixture {
        private final Game game = new Game(new Board(10, 20), new TetrominoGenerator(new Random(42)),
                Difficulty.NORMAL, GameMode.NORMAL);
        private final FakeView view = new FakeView();
        private final FakeTimer dropTimer = new FakeTimer();
        private final FakeTimer stateTimer = new FakeTimer();
        private final FakeTimer timeLimitTimer = new FakeTimer();
        private final List<BattleMessage> outgoing = new ArrayList<>();
        private final List<Long> remoteScores = new ArrayList<>();
        private final List<MatchResult> results = new ArrayList<>();
        private final List<Integer> times = new ArrayList<>();
        private final BattleLink link = new BattleLink(Runnable::run, outgoing::add);
        private int returnCalls;
        private final P2pBattleController controller;

        private Fixture() {
            this(PeerRole.HOST, 0);
        }

        private Fixture(PeerRole role, int seconds) {
            BattleMode mode = seconds > 0 ? BattleMode.TIME_LIMIT : BattleMode.NORMAL;
            MatchStart start = new MatchStart(1, role, new BattleSettings(mode, Difficulty.NORMAL, seconds));
            controller = new P2pBattleController(game, view, dropTimer, (delay, task) -> { },
                    stateTimer, timeLimitTimer, link, start, state -> remoteScores.add(state.getScore()),
                    text -> { }, results::add, times::add, () -> returnCalls++);
        }
    }

    private static final class FakeView implements GameView {
        private Consumer<GameCommand> command;
        private GameSnapshot lastSnapshot;
        @Override public void setCommandHandler(Consumer<GameCommand> command) { this.command = command; }
        @Override public void render(GameSnapshot snapshot) { lastSnapshot = snapshot; }
        @Override public void show() { }
        @Override public void close() { }
    }

    private static final class FakeTimer implements GameTimer {
        private Runnable action = () -> { };
        private boolean started;
        private boolean stopped;
        @Override public void start(Runnable action) { this.action = action; started = true; stopped = false; }
        @Override public void stop() { stopped = true; }
        @Override public void setInterval(long intervalMillis) { }
        private void fire() { action.run(); }
    }
}
// AI-assisted code end
