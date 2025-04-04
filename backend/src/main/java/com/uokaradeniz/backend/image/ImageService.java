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

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
            byte[] compressedImageData = compressImage(imageData);
            imageRepository.save(new Image(image.getOriginalFilename(), null, compressedImageData, sessionId));
        }

        log.info("Image List {}: {}", LocalDateTime.now(), imageList);
    }

    private byte[] compressImage(byte[] imageData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
        BufferedImage bufferedImage = ImageIO.read(bais);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.3f); // Adjust the quality value as needed
        }

        writer.write(null, new IIOImage(bufferedImage, null, null), param);
        writer.dispose();
        ios.close();
        baos.close();

        return baos.toByteArray();
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
            List<EcResponse> responseList = objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
            for (EcResponse EcResponse : responseList) {
                String imageName = EcResponse.getImage();
                String emotion = EcResponse.getEmotion();

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

    List<ReportResponse> prepareReportResults() {
        List<Image> images = imageRepository.findAllByProcessStatus(true).stream().toList();

        if (images.isEmpty()) {
            return null;
        }
        List<ReportResponse> results = new ArrayList<>();
        images.forEach(image -> {
            ReportResponse reportResponse = new ReportResponse();
            reportResponse.setName(image.getName());
            reportResponse.setEmotion(image.getProcessResult());
            reportResponse.setSessionId(image.getSessionId().toString());
            reportResponse.setImageData(image.getImageData());
            results.add(reportResponse);
        });

        return results;
    }

    public void deleteAllReports() {
        imageRepository.deleteAll();
    }
}

