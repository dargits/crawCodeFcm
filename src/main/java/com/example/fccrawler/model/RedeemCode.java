package com.example.fccrawler.model;

public class RedeemCode {
    private String code;
    private String reward;
    private String date;
    private String status;
    private String raw;

    public RedeemCode() {}

    public RedeemCode(String code, String reward, String date, String status, String raw) {
        this.code = code;
        this.reward = reward;
        this.date = date;
        this.status = status;
        this.raw = raw;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getReward() { return reward; }
    public void setReward(String reward) { this.reward = reward; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRaw() { return raw; }
    public void setRaw(String raw) { this.raw = raw; }
}
