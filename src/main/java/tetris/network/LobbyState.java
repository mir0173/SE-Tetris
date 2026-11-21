package tetris.network;

import tetris.battle.BattleSettings;

/** 대기실 상태 */
public record LobbyState(Phase phase, PeerRole role, int port, BattleSettings settings,
        boolean localReady, boolean remoteReady, String message, PeerHealth health) {
    public enum Phase {
        IDLE, HOSTING, CONNECTING, HANDSHAKING, CONNECTED, STARTING, STARTED, CLOSED
    }
}
