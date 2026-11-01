package com.qboix.app;

public final class GloryFitDecoder {
    private GloryFitDecoder() {}
    public static int decodeSteps(byte[] p) {
        if (p == null || p.length < 8 || (p[0] & 0xFF) != 0xB1) return -1;
        return p[7] & 0xFF;
    }
    public static int decodeSpO2(byte[] p) {
        if (p == null || p.length < 4 || (p[0] & 0xFF) != 0x34) return -1;
        int value = p[3] & 0xFF;
        return value >= 70 && value <= 100 ? value : -1;
    }
    public static String decodePacketDate(byte[] p) {
        if (p == null || p.length < 6 || (p[0] & 0xFF) != 0xB1) return null;
        int year = ((p[1] & 0xFF) << 8) | (p[2] & 0xFF);
        int day = p[3] & 0xFF, month = p[4] & 0xFF, hour = p[5] & 0xFF;
        if (year < 2020 || year > 2100 || month < 1 || month > 12 || day < 1 || day > 31 || hour > 23) return null;
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear(); c.setLenient(false); c.set(year, month - 1, day, hour, 0, 0);
        try { c.getTimeInMillis(); } catch (IllegalArgumentException e) { return null; }
        return String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month, day);
    }
}
