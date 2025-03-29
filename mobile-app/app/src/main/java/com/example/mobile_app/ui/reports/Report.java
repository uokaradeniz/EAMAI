package com.example.mobile_app.ui.reports;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Report {
    private String name;
    private String emotion;
    private byte[] imageData;
    private UUID sessionId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public static List<Report> mapJsonToReports(String jsonResponse) {
        Gson gson = new Gson();
        List<ReportJson> reportJsonObjects = gson.fromJson(jsonResponse, new TypeToken<List<ReportJson>>(){}.getType());

        return reportJsonObjects.stream().map(reportJsonObject -> {
            Report report = new Report();
            report.setName(reportJsonObject.getName());
            report.setEmotion(reportJsonObject.getEmotion());
            report.setImageData(Base64.getDecoder().decode(reportJsonObject.getImageData()));
            report.setSessionId(UUID.fromString(reportJsonObject.getSessionId()));
            return report;
        }).collect(Collectors.toList());
    }
}
