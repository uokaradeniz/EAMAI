package com.uokaradeniz.backend.image;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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

    public void saveImagesAndProcess(String jsonPayload) throws IOException {
        saveImages(jsonPayload);
        JsonNode rootNode = new ObjectMapper().readTree(jsonPayload);
        String sessionId = rootNode.get("sessionId").asText();
        sendImagesToAIService(sessionId);
    }

    void saveImages(String jsonPayload) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonPayload);

        UUID sessionId = UUID.fromString(rootNode.get("sessionId").asText());

        JsonNode imagesNode = rootNode.get("images");
        if (imagesNode.isArray()) {
            for (JsonNode imageNode : imagesNode) {
                UUID twinId = UUID.fromString(imageNode.get("twinId").asText());
                String type = imageNode.get("type").asText();
                String originalFilename = imageNode.get("filename").asText();
                String data = imageNode.get("data").asText();

                boolean isPhoto = "photo".equalsIgnoreCase(type);

                byte[] imageData = decodeBase64(data);

                byte[] compressedImageData = compressImage(imageData);

                imageRepository.save(new Image(originalFilename, null, compressedImageData, sessionId.toString(), twinId.toString(), isPhoto));
            }
        }
    }

    private byte[] decodeBase64(String base64String) {
        // Remove any whitespace or invalid characters
        String cleanedBase64 = base64String.replaceAll("\\s+", "");

        try {
            // Decode the cleaned Base64 string
            return Base64.getDecoder().decode(cleanedBase64);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid Base64 input: " + e.getMessage(), e);
        }
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

