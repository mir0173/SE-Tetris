package tetris.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

// AI-assisted code start
class BattleLinkTest {
    @Test
    void mergeConsecutiveStatesWithoutReorderingAttacks() throws Exception {
        List<Runnable> uiTasks = new ArrayList<>();
        List<BattleMessage> messages = new ArrayList<>();
        BattleLink link = new BattleLink(uiTasks::add, outgoing -> { });
        BattleStateData state = BattleStateData.fromSnapshot(BattleMessageTest.snapshot(1));
        link.receive(BattleMessage.state(1, state));
        link.receive(BattleMessage.state(2, state));
        link.receive(BattleMessage.attack(3, List.of(new int[]{99, 0})));
        link.receive(BattleMessage.state(4, state));
        link.receive(BattleMessage.state(5, state));
        link.setReceiver(messages::add);
        assertEquals(1, uiTasks.size());
        uiTasks.remove(0).run();
        assertEquals(List.of(2L, 3L, 5L), messages.stream().map(BattleMessage::sequence).toList());
        link.close();   
        System.out.println("[T20 수신 대기] 연속 상태는 최신 값으로 합치고 공격 순서 보존 PASS");
    }

    @Test
    void ignoreDuplicateAndOutOfOrderMessages() throws Exception {
        List<BattleMessage> messages = new ArrayList<>();
        BattleLink link = new BattleLink(Runnable::run, outgoing -> { });
        link.setReceiver(messages::add);
        BattleMessage attack = BattleMessage.attack(3, List.of(new int[]{99, 0}));
        link.receive(attack);
        link.receive(attack);
        link.receive(BattleMessage.attack(2, List.of(new int[]{99, 0})));
        assertEquals(1, messages.size());
        link.close();   
    }

    @Test
    void discardScheduledOldGameUpdatesWhenClosed() throws Exception {
        List<Runnable> uiTasks = new ArrayList<>();
        List<BattleMessage> messages = new ArrayList<>();
        BattleLink link = new BattleLink(uiTasks::add, outgoing -> { });
        link.setReceiver(messages::add);
        link.receive(BattleMessage.state(1, BattleStateData.fromSnapshot(BattleMessageTest.snapshot(1))));
        link.close();
        uiTasks.forEach(Runnable::run);
        assertTrue(messages.isEmpty());
        assertThrows(IllegalStateException.class, () -> link.sendState(BattleMessageTest.snapshot(2)));
    }

    @Test
    void routeRemotePlayerAttacksAndControlsWithoutRebroadcastingState() {
        List<BattleMessage> outgoing = new ArrayList<>();
        BattleLink link = new BattleLink(Runnable::run, outgoing::add);
        List<Long> renderedScores = new ArrayList<>();
        RemotePlayer remote = new RemotePlayer(link, 10, 20, snapshot -> renderedScores.add(snapshot.getScore()));
        remote.accept(BattleMessage.state(1, BattleStateData.fromSnapshot(BattleMessageTest.snapshot(42))));
        remote.accept(BattleMessage.attack(2, List.of(new int[10])));
        assertEquals(42, remote.getScore());
        assertEquals(List.of(42L), renderedScores);
        assertEquals(1, remote.takeAttackRows().size());
        assertTrue(remote.takeAttackRows().isEmpty());
        assertTrue(outgoing.isEmpty());
        remote.receiveGarbageRows(List.of(new int[10]));
        remote.pause();
        remote.resume();
        remote.stop();
        assertEquals(List.of(BattleMessage.Type.ATTACK, BattleMessage.Type.STOP),
                outgoing.stream().map(BattleMessage::type).toList());
    }

    @Test
    void closeAndNotifyWhenReceiverRejectsMessage() throws Exception {
        List<RuntimeException> failures = new ArrayList<>();
        BattleLink link = new BattleLink(Runnable::run, outgoing -> { }, failures::add);
        link.setReceiver(message -> { throw new IllegalArgumentException("잘못된 상대 상태"); });
        link.receive(BattleMessage.state(1, BattleStateData.fromSnapshot(BattleMessageTest.snapshot(1))));
        assertEquals(1, failures.size());
        assertThrows(IllegalStateException.class, () -> link.sendState(BattleMessageTest.snapshot(2)));
        link.close();   
    }
}
// AI-assisted code end
