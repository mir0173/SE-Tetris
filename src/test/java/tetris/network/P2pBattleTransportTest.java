package tetris.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import tetris.battle.BattleSettings;
import tetris.battle.BattleMode;
import tetris.model.Difficulty;

// AI-assisted code start
@Timeout(15)
class P2pBattleTransportTest {
    private final List<Endpoint> endpoints = new ArrayList<>();

    @AfterEach
    void closeConnections() { endpoints.forEach(endpoint -> endpoint.controller.close()); }

    @Test
    void retainSocketAndExchangeBothPlayersState() throws Exception {
        Pair pair = startPair();
        BattleLink host = pair.host.link();
        BattleLink client = pair.client.link();
        host.sendState(BattleMessageTest.snapshot(123));
        // 수신 처리기 등록 전에 도착한 상태도 이후 전달되어야 한다.
        client.setReceiver(pair.client.messages::add);
        host.setReceiver(pair.host.messages::add);
        assertEquals(123, pair.client.message().state().score());
        client.sendState(BattleMessageTest.snapshot(456));
        assertEquals(456, pair.host.message().state().score());
        assertEquals(LobbyState.Phase.STARTED, pair.host.controller.getState().phase());
        System.out.println("[T20 실제 소켓] 대기실 연결 유지 → 양방향 보드·점수 송수신 PASS");
    }

    @Test
    void deliverAttackOnceAndPreserveOrder() throws Exception {
        Pair pair = startPair();
        BattleLink host = pair.host.link();
        pair.client.link().setReceiver(pair.client.messages::add);
        host.sendState(BattleMessageTest.snapshot(1));
        host.sendAttackRows(List.of(new int[]{99, 0, 99, 99, 99, 99, 99, 99, 99, 99}));
        host.sendState(BattleMessageTest.snapshot(2));
        BattleMessage first = pair.client.message();
        BattleMessage second = pair.client.message();
        BattleMessage third = pair.client.message();
        assertEquals(BattleMessage.Type.STATE, first.type());
        assertEquals(BattleMessage.Type.ATTACK, second.type());
        assertEquals(BattleMessage.Type.STATE, third.type());
        assertEquals(0, second.attackRows().get(0)[1]);
        assertTrue(first.sequence() < second.sequence() && second.sequence() < third.sequence());
    }

    @Test
    void returnFromClientAndStartAnotherGameOnSameConnection() throws Exception {
        Pair pair = startPair();
        BattleLink firstLink = pair.host.link();
        BattleLink firstClientLink = pair.client.link();
        pair.host.await(state -> state.phase() == LobbyState.Phase.STARTED);
        pair.client.await(state -> state.phase() == LobbyState.Phase.STARTED);
        pair.client.controller.requestReturnToLobby();
        pair.host.await(state -> state.phase() == LobbyState.Phase.CONNECTED);
        pair.client.await(state -> state.phase() == LobbyState.Phase.CONNECTED);
        assertThrows(IllegalStateException.class, () -> firstLink.sendState(BattleMessageTest.snapshot(1)));
        assertThrows(IllegalStateException.class, () -> firstClientLink.sendState(BattleMessageTest.snapshot(1)));
        assertDoesNotThrow(() -> firstClientLink.sendStateIfOpen(BattleMessageTest.snapshot(1)));
        pair.host.controller.toggleReady();
        pair.client.controller.toggleReady();
        MatchStart secondHost = pair.host.start();
        MatchStart secondClient = pair.client.start();
        assertEquals(2, secondHost.matchId());
        BattleLink second = pair.client.controller.getBattleLink(secondClient);
        second.setReceiver(pair.client.messages::add);
        pair.host.controller.getBattleLink(secondHost).sendState(BattleMessageTest.snapshot(99));
        assertEquals(99, pair.client.message().state().score());
        System.out.println("[T20 수명 관리] 클라이언트 복귀 요청·이전 경기 연결 무효화·재접속 없는 재시작 PASS");
    }

    @Test
    void reportLoopbackDeliveryTimes() throws Exception {
        Pair pair = startPair();
        BattleLink sender = pair.host.link();
        pair.client.link().setReceiver(pair.client.messages::add);
        for (int index = 0; index < 3; index++) {
            sender.sendState(BattleMessageTest.snapshot(index));
            pair.client.message();
        }
        double[] milliseconds = new double[30];
        for (int index = 0; index < milliseconds.length; index++) {
            long before = System.nanoTime();
            sender.sendState(BattleMessageTest.snapshot(index));
            assertEquals(index, pair.client.message().state().score());
            milliseconds[index] = (System.nanoTime() - before) / 1_000_000.0;
        }
        Arrays.sort(milliseconds);
        double max = milliseconds[milliseconds.length - 1];
        double p95 = milliseconds[(int) Math.ceil(milliseconds.length * 0.95) - 1];
        System.out.printf("[T20 로컬 소켓] 30회 송신→수신처리: p95=%.3fms, 최대=%.3fms, 기준≤200ms, %s%n",
                p95, max, max <= 200 ? "PASS" : "FAIL");
        assertTrue(max <= 200, "로컬 소켓 전달이 200ms를 초과했습니다.");
        System.out.println("[측정 범위] 같은 PC 소켓·수신 처리만 측정. 두 PC LAN·실제 화면 지연 검증은 별도입니다.");
    }

    private Pair startPair() throws Exception {
        Endpoint host = endpoint();
        Endpoint client = endpoint();
        host.controller.host(0);
        int port = host.await(state -> state.phase() == LobbyState.Phase.HOSTING && state.port() > 0).port();
        client.controller.connect("127.0.0.1", port);
        host.await(state -> state.phase() == LobbyState.Phase.CONNECTED);
        client.await(state -> state.phase() == LobbyState.Phase.CONNECTED);
        host.controller.toggleReady();
        client.controller.toggleReady();
        host.current = host.start();
        client.current = client.start();
        return new Pair(host, client);
    }

    private Endpoint endpoint() {
        Endpoint endpoint = new Endpoint();
        endpoints.add(endpoint);
        return endpoint;
    }

    private record Pair(Endpoint host, Endpoint client) { }

    private static final class Endpoint {
        private final BlockingQueue<LobbyState> states = new LinkedBlockingQueue<>();
        private final BlockingQueue<MatchStart> starts = new LinkedBlockingQueue<>();
        private final BlockingQueue<BattleMessage> messages = new LinkedBlockingQueue<>();
        private final P2pLobbyController controller = new P2pLobbyController(Runnable::run, states::add, starts::add,
                new BattleSettings(BattleMode.NORMAL, Difficulty.NORMAL, 0));
        private MatchStart current;

        private BattleLink link() { return controller.getBattleLink(current); }
        private MatchStart start() throws Exception {
            MatchStart value = starts.poll(5, TimeUnit.SECONDS);
            assertNotNull(value, "시작 확인 시간 초과");
            return value;
        }
        private BattleMessage message() throws Exception {
            BattleMessage value = messages.poll(5, TimeUnit.SECONDS);
            assertNotNull(value, "경기 메시지 수신 시간 초과");
            return value;
        }
        private LobbyState await(Predicate<LobbyState> condition) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (true) {
                LobbyState state = states.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                assertNotNull(state, "상태 전환 시간 초과: " + controller.getState());
                if (condition.test(state)) { return state; }
            }
        }
    }
}
// AI-assisted code end
