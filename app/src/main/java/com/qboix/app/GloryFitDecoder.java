package com.qboix.app;

import java.util.Calendar;
import java.util.Locale;

public final class GloryFitDecoder {
    private GloryFitDecoder() {}

    public static final class StepRecord {
        public final int year;
        public final int month;
        public final int day;
        public final int hour;
        public final int steps;
        public final String date;

        private StepRecord(int year, int month, int day, int hour, int steps) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.hour = hour;
            this.steps = steps;
            this.date = String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
        }
    }

    /**
     * QX11 HELLO_WATCH V006927 observed B1 record:
     * B1 | year high | year low | month | day | hour |
     * hourly steps high | hourly steps low | remaining record bytes.
     * The capture on 2026-11-02 showed 07 EA 0B 02 0A = 2026-11-02 10:00.
     * The same hour is notified repeatedly with an increasing cumulative value.
     */
    public static StepRecord decodeStepRecord(byte[] p) {
        if (p == null || p.length != 18) return null;
        int opcode = p[0] & 0xFF;
        if (opcode != 0xB1 && opcode != 0xB2) return null;

        int year = ((p[1] & 0xFF) << 8) | (p[2] & 0xFF);
        int month = p[3] & 0xFF;
        int day = p[4] & 0xFF;
        int hour = p[5] & 0xFF;
        int hourlySteps = ((p[6] & 0xFF) << 8) | (p[7] & 0xFF);

        if (year < 2020 || year > 2100 || month < 1 || month > 12 ||
                day < 1 || day > 31 || hour > 23 || hourlySteps > 50000) {
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setLenient(false);
        calendar.set(year, month - 1, day, hour, 0, 0);
        try {
            calendar.getTimeInMillis();
        } catch (IllegalArgumentException invalidDate) {
            return null;
        }

        return new StepRecord(year, month, day, hour, hourlySteps);
    }

    // Compatibility helper: returns this record's hourly count, not a daily total.
    public static int decodeSteps(byte[] p) {
        StepRecord record = decodeStepRecord(p);
        return record == null ? -1 : record.steps;
    }

    public static int decodeSpO2(byte[] p) {
        if (p == null || p.length < 4 || (p[0] & 0xFF) != 0x34) return -1;
        int value = p[3] & 0xFF;
        return value >= 70 && value <= 100 ? value : -1;
    }

    public static String decodePacketDate(byte[] p) {
        StepRecord record = decodeStepRecord(p);
        return record == null ? null : record.date;
    }
}
