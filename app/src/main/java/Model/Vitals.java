package Model;

public class Vitals {
    private String bpo2;
    private String spo2;
    private int time;

    public Vitals(String bpo2, String spo2, int time) {
        this.bpo2 = bpo2;
        this.spo2 = spo2;
        this.time = time;
    }
    public Vitals(){

    }

    public String getBpo2() {
        return bpo2;
    }

    public void setBpo2(String bpo2) {
        this.bpo2 = bpo2;
    }

    public String getSpo2() {
        return spo2;
    }

    public void setSpo2(String spo2) {
        this.spo2 = spo2;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }
}
