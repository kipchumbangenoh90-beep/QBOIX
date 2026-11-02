package com.qboix.app;

import java.util.ArrayList;
import java.util.List;

/**
 * Decoder candidates. Each result carries a confidence label. Only candidates
 * marked verified=true are shown as values; the rest are logged for review.
 */
public final class Qx11DataEngine {

    public static final class Candidate {
        public final String metric, engine;
        public final int value;
        public final boolean verified;

        Candidate(String metric, String engine, int value, boolean verified) {
            this.metric = metric;
            this.engine = engine;
            this.value = value;
            this.verified = verified;
        }
    }

    private Qx11DataEngine() {}

    /** Standard Bluetooth SIG heart-rate measurement (2A37). Verified by spec. */
    public static Candidate standardHeartRate(byte[] p) {
        if (p == null || p.length < 2) return null;
        int flags = p[0] & 255;
        if ((flags & 1) != 0 && p.length < 3) return null;
        int bpm = (flags & 1) == 0 ? p[1] & 255 : ((p[2] & 255) << 8) | (p[1] & 255);
        return (bpm > 0 && bpm < 260)
                ? new Candidate("heart", "BLE 2A37", bpm, true) : null;
    }

    /** E5 11 00 <bpm> live stream. Observed in the official-app style packet; not yet seen in your captures. */
    public static Candidate gloryFitE5(byte[] p) {
        if (p == null || p.length < 4 || (p[0] & 255) != 0xE5
                || (p[1] & 255) != 0x11 || (p[2] & 255) != 0) return null;
        int bpm = p[3] & 255;
        return (bpm > 0 && bpm < 260)
                ? new Candidate("heart", "GloryFit E5 live", bpm, false) : null;
    }

    /** FD workout packet. UNVERIFIED field positions. Only present during a workout. */
    public static Candidate[] gloryFitWorkout(byte[] p) {
        if (p == null || p.length != 14 || (p[0] & 255) != 0xFD) return new Candidate[0];
        int bpm = p[2] & 255;
        int kcal = ((p[3] & 255) << 8) | (p[4] & 255);
        List<Candidate> out = new ArrayList<>();
        if (bpm > 0 && bpm < 260) out.add(new Candidate("heart", "FD workout (unverified)", bpm, false));
        if (kcal > 0 && kcal < 20000) out.add(new Candidate("calories", "FD workout (unverified)", kcal, false));
        return out.toArray(new Candidate[0]);
    }

    /**
     * F7 history packet. The field position is not confirmed. Returns a
     * candidate that is never published as a live value.
     */
    public static Candidate gloryFitHeartHistory(byte[] p) {
        if (p == null || p.length != 18 || (p[0] & 255) != 0xF7) return null;
        int v = p[6] & 255;
        return (v >= 30 && v <= 240)
                ? new Candidate("heart", "F7 history byte 6 (unverified)", v, false) : null;
    }
}