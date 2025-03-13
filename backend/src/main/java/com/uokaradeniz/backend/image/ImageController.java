package com.uokaradeniz.backend.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;

import static com.uokaradeniz.backend.image.ImageService.UPLOAD_DIR;
import static com.uokaradeniz.backend.image.ImageService.imageList;

@RestController
@RequestMapping("/api")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);
    private final ImageService imageService;
    private final ImageRepository imageRepository;

    public ImageController(ImageService imageService, ImageRepository imageRepository) {
        this.imageService = imageService;
        this.imageRepository = imageRepository;
    }


    @PostMapping("/uploadImage")
    public ResponseEntity<?> getImageFromUser(@RequestParam("file") MultipartFile file, @RequestParam("sessionId") String sessionId) {
        try {
            File savedImage = imageService.saveImage(file, sessionId);
            return ResponseEntity.ok("Image retrieved successfully: " + savedImage.getAbsolutePath());
        } catch (IOException e) {
            log.error("Image couldn't be gathered: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Image couldn't be gathered: " + e.getMessage());
        }
    }

    @GetMapping("/testimages")
    private ResponseEntity<?> getImageList() {
        //todo get image properties from db
//        imageService.loadImagesFromDirectory();
        return ResponseEntity.ok("Image List: " + imageList);
    }
}
