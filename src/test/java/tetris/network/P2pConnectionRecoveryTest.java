package tetris.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import tetris.battle.BattleMode;
import tetris.battle.BattleSettings;
import tetris.model.Difficulty;
import tetris.network.LobbyState.Phase;

// AI-assisted code start
class P2pConnectionRecoveryTest {
    private static final BattleSettings SETTINGS = new BattleSettings(BattleMode.NORMAL, Difficulty.NORMAL, 0);

    @Test
    void distinguishLagFromTimeoutAndIgnoreLateOrMismatchedReplies() {
        PeerHeartbeat heartbeat = new PeerHeartbeat(0);
        long probe = heartbeat.check(nanos(1_000)).probeId();
        assertTrue(probe > 0);
        heartbeat.receivePong(probe + 1, nanos(1_100));
        assertFalse(heartbeat.check(nanos(1_399)).health().delayed());
        assertTrue(heartbeat.check(nanos(1_400)).health().delayed());
        assertFalse(heartbeat.check(nanos(5_999)).timedOut());
        heartbeat.receivePong(probe, nanos(6_000));
        assertTrue(heartbeat.check(nanos(6_000)).timedOut());
        System.out.println("[T21 경계값] 지연 400ms, 무응답 5000ms, 잘못된 응답 무시 PASS");
    }

    @Test
    @Timeout(20)
    void warnRecoverAndReconnectAfterUnansweredProbe() throws Exception {
        LinkedBlockingQueue<LobbyState> states = new LinkedBlockingQueue<>();
        try (P2pLobbyController host = new P2pLobbyController(Runnable::run, states::add, start -> { }, SETTINGS)) {
            host.host(0);
            int port = await(states, state -> state.phase() == Phase.HOSTING && state.port() > 0).port();
            try (Socket peer = new Socket("127.0.0.1", port)) {
                peer.setSoTimeout(4_000);
                DataInputStream input = new DataInputStream(peer.getInputStream());
                DataOutputStream output = new DataOutputStream(peer.getOutputStream());
                assertEquals(LobbyMessage.Type.HELLO, MessageCodec.read(input).type());
                MessageCodec.write(output, LobbyMessage.hello(PeerRole.CLIENT));
                assertEquals(LobbyMessage.Type.CONFIG, MessageCodec.read(input).type());

                LobbyMessage first = MessageCodec.read(input);
                assertEquals(LobbyMessage.Type.PING, first.type());
                await(states, state -> state.phase() == Phase.CONNECTED
                        && state.health() != null && state.health().delayed());
                MessageCodec.write(output, LobbyMessage.probe(LobbyMessage.Type.PONG, first.probeId()));

                LobbyMessage second = MessageCodec.read(input);
                assertEquals(LobbyMessage.Type.PING, second.type());
                MessageCodec.write(output, LobbyMessage.probe(LobbyMessage.Type.PONG, second.probeId()));
                LobbyState recovered = await(states, state -> state.health() != null
                        && state.health().roundTripMillis() >= 0 && !state.health().delayed());
                System.out.printf("[T21 실제 소켓] 지연 경고 → 정상 복구, 왕복 %dms PASS%n",
                        recovered.health().roundTripMillis());

                assertEquals(LobbyMessage.Type.PING, MessageCodec.read(input).type());
                LobbyState disconnected = await(states, state -> state.phase() == Phase.IDLE);
                assertTrue(disconnected.message().contains("응답하지 않았습니다"));
                assertNull(disconnected.health());
            }

            host.host(0);
            int nextPort = await(states, state -> state.phase() == Phase.HOSTING && state.port() > 0).port();
            LinkedBlockingQueue<LobbyState> clientStates = new LinkedBlockingQueue<>();
            LinkedBlockingQueue<String> remembered = new LinkedBlockingQueue<>();
            try (P2pLobbyController client = new P2pLobbyController(Runnable::run, clientStates::add,
                    start -> { }, SETTINGS, remembered::add)) {
                client.connect("127.0.0.1", nextPort);
                await(clientStates, state -> state.phase() == Phase.CONNECTED);
                assertEquals("127.0.0.1", remembered.poll());
                host.updateSettings(new BattleSettings(BattleMode.ITEM, Difficulty.NORMAL, 0));
                await(clientStates, state -> state.settings().mode() == BattleMode.ITEM);
                assertTrue(remembered.isEmpty());
            }
            System.out.println("[T21 실제 소켓] 무응답 종료 → 초기 화면 상태 → 다음 접속 성공 및 IP 1회 저장 요청 PASS");
        }
    }

    private static LobbyState await(LinkedBlockingQueue<LobbyState> states, Predicate<LobbyState> condition)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(7);
        while (true) {
            LobbyState state = states.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            assertNotNull(state, "연결 상태 전환 시간 초과");
            if (condition.test(state)) {
                return state;
            }
        }
    }

    private static long nanos(long millis) {
        return TimeUnit.MILLISECONDS.toNanos(millis);
    }
}
// AI-assisted code end
