package com.uokaradeniz.backend.image;

import com.uokaradeniz.backend.company.Company;
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
    private UUID twinId;
    private boolean isPhoto;
    private String sessionDetails;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    public Image(String originalFilename, String absolutePath, byte[] imageData, String sessionId, String twinId, boolean isPhoto, Company company) {
        this.name = originalFilename;
        this.path = absolutePath;
        this.imageData = imageData;
        this.sessionId = UUID.fromString(sessionId);
        this.twinId = UUID.fromString(twinId);
        this.isPhoto = isPhoto;
        this.company = company;
    }
}