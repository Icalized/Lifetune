package Model;

public class Vitals {
    private String bpm;
    private String spo2;
    private long time;

    public Vitals(String bpm, String spo2, long time) {
        this.bpm = bpm;
        this.spo2 = spo2;
        this.time = time;
    }

    public Vitals(){
    }


    public String getBpm() {
        return bpm;
    }

    public void setBpm(String bpm) {
        this.bpm = bpm;
    }

    public String getSpo2() {
        return spo2;
    }

    public void setSpo2(String spo2) {
        this.spo2 = spo2;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
