package com.uokaradeniz.backend.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<?> getImagesFromUser(@RequestParam("images") List<MultipartFile> images, @RequestParam("sessionId") String sessionId) {
        try {
            imageService.saveImagesAndProcess(images, sessionId);
            return ResponseEntity.ok("Images retrieved successfully.");
        } catch (IOException e) {
            log.error("Images couldn't be gathered: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Images couldn't be gathered: " + e.getMessage());
        }
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getResults() {
        List<ReportResponse> responseObject = imageService.prepareReportResults();

        return ResponseEntity.ok(responseObject);
    }

}
