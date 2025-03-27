package com.uokaradeniz.backend.image;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Entity
@RequiredArgsConstructor
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    private String name;
    private String path;
    private boolean processStatus;
    private byte[] imageData;
    private String processResult;
    private UUID sessionId;

    public Image(String originalFilename, String absolutePath, byte[] imageData, String sessionId) {
        this.name = originalFilename;
        this.path = absolutePath;
        this.imageData = imageData;
        this.sessionId = UUID.fromString(sessionId);
    }
}

