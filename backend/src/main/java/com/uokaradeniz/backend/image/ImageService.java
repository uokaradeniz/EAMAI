package com.uokaradeniz.backend.image;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class ImageService {
    static final List<Image> imageList = new ArrayList<>();
    private final ImageRepository imageRepository;
    private final RestTemplate restTemplate;

    public ImageService(ImageRepository imageRepository, RestTemplate restTemplate) {
        this.imageRepository = imageRepository;
        this.restTemplate = restTemplate;
    }

    public void saveImagesAndProcess(List<MultipartFile> images, String sessionId) throws IOException {
        saveImages(images, sessionId);
        sendImagesToAIService(sessionId);
    }

    void saveImages(List<MultipartFile> images, String sessionId) throws IOException {
        for (MultipartFile image : images) {
            byte[] imageData = image.getBytes();
            imageRepository.save(new Image(image.getOriginalFilename(), null, imageData, sessionId));
        }

        log.info("Image List {}: {}", LocalDateTime.now(), imageList);
    }

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
            List<ResponseObject> responseList = objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
            for (ResponseObject responseObject : responseList) {
                String imageName = responseObject.getImage();
                String emotion = responseObject.getEmotion();

                imageRepository.findImagesByName(imageName).forEach(image -> {
                    image.setProcessStatus(true);
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

