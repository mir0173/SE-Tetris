package tetris.network;

/** 대기실 상태 */
public record LobbyState(Phase phase, PeerRole role, int port, LobbySettings settings,
        boolean localReady, boolean remoteReady, String message) {
    public enum Phase {
        IDLE, HOSTING, CONNECTING, HANDSHAKING, CONNECTED, STARTING, STARTED, CLOSED
    }
}
