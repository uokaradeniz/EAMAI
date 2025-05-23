package com.example.mobile_app.ui.reports;

public class ReportJson {

    private String name;
    private String analysis;
    private String imageData;
    private String sessionId;
    private String sessionDetails;
    private String twinId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionDetails() {
        return sessionDetails;
    }

    public String getTwinId() {
        return twinId;
    }

    public void setTwinId(String twinId) {
        this.twinId = twinId;
    }
}
