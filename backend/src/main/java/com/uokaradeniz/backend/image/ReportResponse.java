package com.uokaradeniz.backend.image;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReportResponse {
    private String name;
    private String emotion;
    private String sessionId;
    private byte[] imageData;
}