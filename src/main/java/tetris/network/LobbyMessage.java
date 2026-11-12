package tetris.network;

import java.util.Objects;

/** 대기실 전송 메시지 */
record LobbyMessage(Type type, int version, PeerRole role, long revision,
        LobbySettings settings, boolean ready, long matchId) {
    static final int PROTOCOL_VERSION = 1;

    enum Type {
        HELLO, CONFIG, READY, START, START_ACK, START_COMMIT, START_REJECT
    }

    LobbyMessage {
        Objects.requireNonNull(type, "type");
        if (version != PROTOCOL_VERSION || revision < 0 || matchId < 0) {
            throw new IllegalArgumentException("지원하지 않는 메시지입니다.");
        }
        switch (type) {
            case HELLO -> Objects.requireNonNull(role, "role");
            case CONFIG -> {
                Objects.requireNonNull(settings, "settings");
                if (revision == 0) {
                    throw new IllegalArgumentException("설정 번호가 필요합니다.");
                }
            }
            case READY -> {
                if (revision == 0) {
                    throw new IllegalArgumentException("설정 번호가 필요합니다.");
                }
            }
            case START, START_ACK, START_COMMIT, START_REJECT -> {
                if (revision == 0 || matchId == 0) {
                    throw new IllegalArgumentException("설정과 경기 번호가 필요합니다.");
                }
            }
        }
    }

    static LobbyMessage hello(PeerRole role) {
        return new LobbyMessage(Type.HELLO, PROTOCOL_VERSION, role, 0, null, false, 0);
    }

    static LobbyMessage config(long revision, LobbySettings settings) {
        return new LobbyMessage(Type.CONFIG, PROTOCOL_VERSION, null, revision, settings, false, 0);
    }

    static LobbyMessage ready(long revision, boolean ready) {
        return new LobbyMessage(Type.READY, PROTOCOL_VERSION, null, revision, null, ready, 0);
    }

    static LobbyMessage start(Type type, long revision, long matchId) {
        return new LobbyMessage(type, PROTOCOL_VERSION, null, revision, null, false, matchId);
    }
}
