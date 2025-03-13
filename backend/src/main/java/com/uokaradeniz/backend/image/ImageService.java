package com.uokaradeniz.backend.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ImageService {
    static String UPLOAD_DIR = System.getProperty("user.dir") + "/resources/images";
    static final List<Image> imageList = new ArrayList<>();
    private final ImageRepository imageRepository;
    private final RestTemplate restTemplate;

    public ImageService(ImageRepository imageRepository, RestTemplate restTemplate) {
        this.imageRepository = imageRepository;
        this.restTemplate = restTemplate;
    }

    File saveImage(MultipartFile file, String sessionId) throws IOException {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) uploadDir.mkdirs(); // Ensure directory exists

        File savedFile = new File(uploadDir, Objects.requireNonNull(file.getOriginalFilename()));
        file.transferTo(savedFile);
        log.info("Image retrieved sucessfully: {}", savedFile.getAbsolutePath());
        //todo send image properties to db
        byte[] imageData = Files.readAllBytes(savedFile.toPath());
        imageRepository.save(new Image(savedFile.getAbsolutePath(), savedFile.getName(), imageData, sessionId));
        loadImagesFromDirectory(sessionId);
        log.info("Image List {}: {}", LocalDateTime.now(), imageList);
        sendImageToAIService();
        return savedFile;
    }

    void loadImagesFromDirectory(String sessionId) {
        imageList.clear();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(UPLOAD_DIR), "*.{jpg,jpeg,png}")) {
            for (Path entry : stream) {
                File file = entry.toFile();
                imageList.add(new Image(file.getName(), file.getAbsolutePath(), Files.readAllBytes(file.toPath()), sessionId));
            }
        } catch (IOException e) {
            log.error("Error reading images from directory: {}", e.getMessage());
        }
    }

    void sendImageToAIService() {
        imageList.forEach(image -> {
            Path imagePath = Paths.get(image.getPath());
            byte[] imageBytes;
            try {
                imageBytes = Files.readAllBytes(imagePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ByteArrayResource resource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return image.getName();
                }
            };

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String serverUrl = "http://127.0.0.1:5000/processImage";

            ResponseEntity<String> response = restTemplate.postForEntity(serverUrl, requestEntity, String.class);

            System.out.println("Response: " + response.getBody());
            imageRepository.findBySessionId(image.getSessionId()).forEach(returnedImage -> {
                returnedImage.setProcessStatus(true);
                returnedImage.setProcessResult(response.getBody());
                imageRepository.save(returnedImage);
            });
        });
    }
}

