package tetris.network;

public record PeerHealth(long roundTripMillis, boolean delayed) {
    static final PeerHealth MEASURING = new PeerHealth(-1, false);

    public String displayText() {
        if (delayed) {
            return "네트워크 응답 지연";
        }
        if (roundTripMillis < 0) {
            return "연결 상태 확인중";
        }
        return "지연 " + roundTripMillis + " ms";
    }
}
