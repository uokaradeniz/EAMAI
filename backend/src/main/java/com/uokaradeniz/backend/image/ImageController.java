package com.uokaradeniz.backend.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);
    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/uploadImages")
    public ResponseEntity<?> getImagesFromUser(@RequestBody String jsonPayload) {
        try {
            imageService.saveImagesAndProcess(jsonPayload);
            return ResponseEntity.ok("Images retrieved successfully.");
        } catch (IOException e) {
            log.error("Images couldn't be gathered: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Images couldn't be gathered: " + e.getMessage());
        }
    }
    @GetMapping("/reports")
    public ResponseEntity<?> getResults(@RequestParam Long companyId) {
        List<ReportResponse> responseObject = imageService.prepareReportResults(companyId);

        return ResponseEntity.ok(responseObject);
    }

    @GetMapping("/deleteReports")
    public ResponseEntity<?> deleteAllReports() {
        imageService.deleteAllReports();
        return ResponseEntity.ok("All reports deleted successfully.");
    }
}
