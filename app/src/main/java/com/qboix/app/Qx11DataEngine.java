package com.qboix.app;

import java.util.ArrayList;
import java.util.List;

/** Independent QX11 decoder candidates. Each result retains its source and live/history status. */
public final class Qx11DataEngine {
    public static final class Candidate {
        public final String metric, engine;
        public final int value;
        public final boolean live;
        Candidate(String metric, String engine, int value, boolean live) {
            this.metric=metric; this.engine=engine; this.value=value; this.live=live;
        }
    }
    private Qx11DataEngine() {}

    // Engine 1: Bluetooth SIG Heart Rate Measurement (2A37).
    public static Candidate standardHeartRate(byte[] p) {
        if (p==null || p.length<2) return null;
        int flags=p[0]&255;
        if ((flags&1)!=0 && p.length<3) return null;
        int bpm=(flags&1)==0 ? p[1]&255 : ((p[2]&255)<<8)|(p[1]&255);
        return bpm>0 && bpm<260 ? new Candidate("heart","BLE 2A37",bpm,true) : null;
    }
    // Engine 2: GloryFit dynamic stream E5 11 00 <bpm>.
    public static Candidate gloryFitE5(byte[] p) {
        if (p==null || p.length<4 || (p[0]&255)!=0xE5 || (p[1]&255)!=0x11 || (p[2]&255)!=0) return null;
        int bpm=p[3]&255;
        return bpm>0 && bpm<260 ? new Candidate("heart","GloryFit E5 live",bpm,true) : null;
    }
    // Engine 3: 14-byte FD workout packet: FD <sport> <hr> <cal16> ...
    public static Candidate[] gloryFitWorkout(byte[] p) {
        if (p==null || p.length!=14 || (p[0]&255)!=0xFD) return new Candidate[0];
        int bpm=p[2]&255, kcal=((p[3]&255)<<8)|(p[4]&255);
        List<Candidate> out=new ArrayList<>();
        if (bpm>0 && bpm<260) out.add(new Candidate("heart","GloryFit FD workout",bpm,true));
        if (kcal>0 && kcal<20000) out.add(new Candidate("calories","GloryFit FD workout",kcal,true));
        return out.toArray(new Candidate[0]);
    }
    // Engine 4: F7 history, kept historical so it cannot overwrite live BPM.
    public static Candidate gloryFitHeartHistory(byte[] p) {
        if (p==null || p.length!=18 || (p[0]&255)!=0xF7) return null;
        int latest=-1;
        for(int i=6;i<p.length;i++){ int v=p[i]&255; if(v>=30 && v<=240) latest=v; }
        return latest>0 ? new Candidate("heart","GloryFit F7 history",latest,false) : null;
    }
    // Engine 5: verified QX11-specific step and SpO2 decoders.
    public static Candidate qx11Steps(byte[] p) {
        GloryFitDecoder.StepRecord r=GloryFitDecoder.decodeStepRecord(p);
        return r==null ? null : new Candidate("steps","QX11 B1/B2 hourly record",r.steps,false);
    }
    public static Candidate qx11SpO2(byte[] p) {
        int v=GloryFitDecoder.decodeSpO2(p);
        return v<0 ? null : new Candidate("spo2","QX11 34 spot result",v,true);
    }
    public static boolean agree(Candidate a, Candidate b) {
        return a!=null && b!=null && a.metric.equals(b.metric) && a.value==b.value;
    }
}
