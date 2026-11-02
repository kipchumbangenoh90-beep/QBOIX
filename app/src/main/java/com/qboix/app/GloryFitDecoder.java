package com.qboix.app;

import java.util.Calendar;
import java.util.Locale;

/**
 * Decoders for QX11 packets. Field positions marked UNVERIFIED are inferred
 * from captures and must be checked against a known watch reading before use.
 */
public final class GloryFitDecoder {

    private GloryFitDecoder() {}

    public static final class StepRecord {
        public final int hour;
        public final int steps;
        public final String date;

        private StepRecord(String date, int hour, int steps) {
            this.date = date;
            this.hour = hour;
            this.steps = steps;
        }
    }

    /**
     * B1/B2 18-byte record. UNVERIFIED: the value may be a running total
     * rather than an hourly count. Layout observed in captures:
     * [0]=opcode, [1..2]=year (BE), [3]=month, [4]=day, [5]=hour, [6..7]=value (BE).
     */
    public static StepRecord decodeStepRecord(byte[] p) {
        if (p == null || p.length != 18) return null;

        int opcode = p[0] & 0xFF;
        if (opcode != 0xB1 && opcode != 0xB2) return null;

        int year = ((p[1] & 0xFF) << 8) | (p[2] & 0xFF);
        int month = p[3] & 0xFF;
        int day = p[4] & 0xFF;
        int hour = p[5] & 0xFF;
        int value = ((p[6] & 0xFF) << 8) | (p[7] & 0xFF);

        if (year < 2020 || year > 2100 || month < 1 || month > 12
                || day < 1 || day > 31 || hour > 23 || value > 50000) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setLenient(false);
        cal.set(year, month - 1, day, hour, 0, 0);
        try {
            cal.getTimeInMillis();
        } catch (IllegalArgumentException invalid) {
            return null;
        }

        String date = String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
        return new StepRecord(date, hour, value);
    }

    /** Standard 34 packet: value at byte 3. UNVERIFIED for this watch. */
    public static int decodeSpO2(byte[] p) {
        if (p == null || p.length < 4 || (p[0] & 0xFF) != 0x34) return -1;
        int v = p[3] & 0xFF;
        return (v >= 70 && v <= 100) ? v : -1;
    }
}