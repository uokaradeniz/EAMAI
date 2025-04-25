package com.uokaradeniz.backend.image;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReportResponse {
    private String name;
    private String analysis;
    private String sessionId;
    private byte[] imageData;
}