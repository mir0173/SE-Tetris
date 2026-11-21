package tetris.network;

import java.util.concurrent.TimeUnit;

final class PeerHeartbeat {
    static final long PROBE_INTERVAL_MILLIS = 1_000;
    static final long LAG_THRESHOLD_MILLIS = 400;
    static final long TIMEOUT_MILLIS = 5_000;

    private long lastProbeTime;
    private long lastProbeId;
    private long pendingProbeId;
    private long roundTripMillis = -1;

    PeerHeartbeat(long now) {
        lastProbeTime = now;
    }

    synchronized Check check(long now) {
        long elapsed = TimeUnit.NANOSECONDS.toMillis(now - lastProbeTime);
        if (pendingProbeId != 0) {
            boolean delayed = elapsed >= LAG_THRESHOLD_MILLIS || roundTripMillis >= LAG_THRESHOLD_MILLIS;
            return new Check(0, new PeerHealth(roundTripMillis, delayed), elapsed >= TIMEOUT_MILLIS);
        }
        if (elapsed >= PROBE_INTERVAL_MILLIS) {
            pendingProbeId = ++lastProbeId;
            lastProbeTime = now;
        }
        return new Check(pendingProbeId, new PeerHealth(roundTripMillis, roundTripMillis >= LAG_THRESHOLD_MILLIS),
                false);
    }

    synchronized void receivePong(long probeId, long now) {
        if (pendingProbeId == 0 || pendingProbeId != probeId) {
            return;
        }
        long elapsed = TimeUnit.NANOSECONDS.toMillis(now - lastProbeTime);
        if (elapsed < TIMEOUT_MILLIS) {
            roundTripMillis = elapsed;
            pendingProbeId = 0;
        }
    }

    record Check(long probeId, PeerHealth health, boolean timedOut) { }
}
