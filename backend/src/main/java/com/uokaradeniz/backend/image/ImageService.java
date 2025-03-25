package com.uokaradeniz.backend.image;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uokaradeniz.backend.utils.UsefulUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
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
import java.util.*;

import static org.apache.tomcat.util.http.fileupload.FileUtils.deleteDirectory;

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
//        UsefulUtils.cleanFolder(UPLOAD_DIR);
    }

    public void saveImagesAndProcess(List<MultipartFile> images, String sessionId) throws IOException {
//        UsefulUtils.cleanFolder(UPLOAD_DIR);
        saveImages(images, sessionId);
        sendImagesToAIService(sessionId);
    }

    void saveImages(List<MultipartFile> images, String sessionId) throws IOException {
//        File uploadDir = new File(UPLOAD_DIR);
//        if (!uploadDir.exists()) uploadDir.mkdirs(); // Ensure directory exists

        for (MultipartFile image : images) {
//            File savedImage = new File(uploadDir, Objects.requireNonNull(image.getOriginalFilename()));
//            image.transferTo(savedImage);
//            log.info("Image retrieved successfully: {}", savedImage.getAbsolutePath());
            // Save image properties to the database
            byte[] imageData = image.getBytes();
            imageRepository.save(new Image(image.getOriginalFilename(), null, imageData, sessionId));
        }

//        loadImagesFromDirectory(sessionId);
        log.info("Image List {}: {}", LocalDateTime.now(), imageList);
//        sendImagesToAIService();
    }

//    void loadImagesFromDirectory(String sessionId) {
//        imageList.clear();
//        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(UPLOAD_DIR), "*.{jpg,jpeg,png}")) {
//            for (Path entry : stream) {
//                File file = entry.toFile();
//                imageList.add(new Image(file.getName(), file.getAbsolutePath(), Files.readAllBytes(file.toPath()), sessionId));
//            }
//        } catch (IOException e) {
//            log.error("Error reading images from directory: {}", e.getMessage());
//        }
//    }

    @Async
    void sendImagesToAIService(String sessionId) {
        List<Image> images = imageRepository.findBySessionId(UUID.fromString(sessionId));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        images.forEach(image -> {
            ByteArrayResource resource = new ByteArrayResource(image.getImageData()) {
                @Override
                public String getFilename() {
                    return image.getName();
                }
            };
            body.add("images", resource);
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String serverUrl = "http://127.0.0.1:5000/processImages";

        ResponseEntity<String> response = restTemplate.postForEntity(serverUrl, requestEntity, String.class);

        System.out.println("Response: " + response.getBody());

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            List<ResponseObject> responseList = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            for (ResponseObject responseObject : responseList) {
                String imagePath = responseObject.getImage();
                String emotion = responseObject.getEmotion();

                images.stream()
                        .filter(image -> image.getPath().equals(imagePath))
                        .forEach(image -> {
                            image.setProcessResult(emotion);
                            imageRepository.save(image);
                        });
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON response", e);
        }
    }

    @Setter
    @Getter
    public static class ResponseObject {
        private String image;
        private String emotion;
    }
}

