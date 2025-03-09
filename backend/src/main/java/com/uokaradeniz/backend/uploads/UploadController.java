package com.uokaradeniz.backend.uploads;

import lombok.Data;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class UploadController {

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/resources/images";
    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private static List<Image> imageList = new ArrayList<>();

    @PostMapping("/uploadImages")
    public String getImagesFromUser(@RequestParam("files") MultipartFile[] files) {
        List<String> savedFilePaths = new ArrayList<>();
        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) uploadDir.mkdirs(); // Ensure directory exists

            for (MultipartFile file : files) {
                File savedFile = new File(uploadDir, Objects.requireNonNull(file.getOriginalFilename()));
                file.transferTo(savedFile);
                log.info("Image retrieved successfully: {}", savedFile.getAbsolutePath());
                imageList.add(new Image(file.getOriginalFilename(), savedFile.getAbsolutePath(), false));
                savedFilePaths.add(savedFile.getAbsolutePath());
            }

            return "Images retrieved successfully: " + String.join(", ", savedFilePaths);
        } catch (IOException e) {
            log.error("Images couldn't be gathered: {}", e.getMessage());
            return "Images couldn't be gathered: " + e.getMessage();
        }
    }

//    @GetMapping("/images/{filename}")
//    public ResponseEntity<byte[]> getImage(@PathVariable String filename) throws IOException {
//        for (Image image : imageList) {
//            File imageFile = image.getFile();
//            if (!imageFile.exists()) {
//                return ResponseEntity.status(404).body(null);
//            }
//            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
//            return ResponseEntity.ok()
//                    .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
//                    .body(imageBytes);
//        }
//    }

    @GetMapping
    private List<Image> getImageList() {
        //todo get image properties from db
        return null;
    }

    @Data
    private static class Image {
        private String name;
        private String path;
        private boolean processStatus;
        private File file;

        public Image(String originalFilename, String absolutePath, boolean processStatus) {
            this.name = originalFilename;
            this.path = absolutePath;
            this.processStatus = processStatus;
            file = new File(UPLOAD_DIR, name);
        }
    }
}