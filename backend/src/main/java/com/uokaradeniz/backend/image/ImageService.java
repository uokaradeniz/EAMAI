package com.uokaradeniz.backend.image;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uokaradeniz.backend.company.Company;
import com.uokaradeniz.backend.company.CompanyRepository;
import com.uokaradeniz.backend.report.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class ImageService {
    private final ImageRepository imageRepository;
    private final RestTemplate restTemplate;
    private final CompanyRepository companyRepository;

    public ImageService(ImageRepository imageRepository, RestTemplate restTemplate, CompanyRepository companyRepository) {
        this.imageRepository = imageRepository;
        this.restTemplate = restTemplate;
        this.companyRepository = companyRepository;
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
        Long companyId = rootNode.get("companyId").asLong();

        JsonNode imagesNode = rootNode.get("images");
        if (imagesNode.isArray()) {
            Company company = companyRepository.findById(companyId).orElseThrow(() -> new RuntimeException("Company not found"));

            for (JsonNode imageNode : imagesNode) {
                UUID twinId = UUID.fromString(imageNode.get("twinId").asText());
                String type = imageNode.get("type").asText();
                String originalFilename = imageNode.get("filename").asText();
                String data = imageNode.get("data").asText();

                boolean isPhoto = "photo".equalsIgnoreCase(type);
                byte[] imageData = decodeBase64(data);
                byte[] compressedImageData = compressImage(imageData);

                imageRepository.save(new Image(originalFilename, null, compressedImageData, sessionId.toString(), twinId.toString(), isPhoto, company));
            }
        }
    }

    private byte[] decodeBase64(String base64String) {
        String cleanedBase64 = base64String.replaceAll("\\s+", "");

        try {
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
            param.setCompressionQuality(0.3f);
        }

        writer.write(null, new IIOImage(bufferedImage, null, null), param);
        writer.dispose();
        ios.close();
        baos.close();

        return baos.toByteArray();
    }

    void sendImagesToAIService(String sessionId) {
        List<Image> images = imageRepository.findImagesByProcessStatusAndSessionId(false, UUID.fromString(sessionId));

        Map<UUID, Queue<Image>> imagesByTwinId = new HashMap<>();
        for (Image image : images) {
            imagesByTwinId.computeIfAbsent(image.getTwinId(), _ -> new LinkedList<>()).add(image);
        }

        for (Map.Entry<UUID, Queue<Image>> entry : imagesByTwinId.entrySet()) {
            UUID twinId = entry.getKey();
            Queue<Image> twinQueue = entry.getValue();

            while (!twinQueue.isEmpty()) {
                List<Image> pair = new ArrayList<>();
                for (int i = 0; i < 2 && !twinQueue.isEmpty(); i++) {
                    pair.add(twinQueue.poll());
                }

                List<Map<String, Object>> imagePayloads = new ArrayList<>();
                pair.forEach(image -> {
                    Map<String, Object> imagePayload = new HashMap<>();
                    imagePayload.put(image.isPhoto() ? "photo_data" : "screenshot_data", Base64.getEncoder().encodeToString(image.getImageData()));
                    imagePayload.put("name", image.getName());
                    imagePayloads.add(imagePayload);
                });

                imagePayloads.sort((a, b) -> {
                    boolean aIsPhoto = a.containsKey("photo_data");
                    boolean bIsPhoto = b.containsKey("photo_data");
                    return Boolean.compare(!aIsPhoto, !bIsPhoto);
                });

                Map<String, Object> requestPayload = new HashMap<>();
                requestPayload.put("twinId", twinId.toString());
                requestPayload.put("images", imagePayloads);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestPayload, headers);

                String serverUrl = "http://127.0.0.1:5000/processImages";

                ResponseEntity<String> response = restTemplate.postForEntity(serverUrl, requestEntity, String.class);

                System.out.println("Response for twinId " + twinId + ": " + response.getBody());

                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    List<EcResponse> responseList = objectMapper.readValue(response.getBody(), new TypeReference<>() {
                    });
                    for (EcResponse ecResponse : responseList) {
                        String analysis = ecResponse.getAnalysis();

                        imageRepository.findImagesByTwinId(twinId).forEach(image -> {
                            if (pair.stream().anyMatch(p -> p.getName().equals(image.getName()))) {
                                image.setProcessStatus(true);
                                image.setProcessResult(analysis);
                                imageRepository.save(image);
                            }
                        });
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Error processing JSON response", e);
                }
            }
        }
    }

    private void sendProcessResultsToAIService(Long companyId) {
        Map<String, List<String>> resultsBySessionId = new HashMap<>();

        imageRepository.findAllByProcessStatusAndIsPhotoAndCompanyId(true, true, companyId).forEach(image ->
                resultsBySessionId
                        .computeIfAbsent(String.valueOf(image.getSessionId()), _ -> new ArrayList<>())
                        .add(image.getProcessResult())
        );
        companyRepository.findById(companyId).ifPresent(company -> {
            company.setUsageCount(company.getUsageCount() + 1);
            companyRepository.save(company);
        });

        resultsBySessionId.forEach((sessionId, processResults) -> {
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("sessionId", sessionId);
            requestPayload.put("results", processResults);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestPayload, headers);

            String serverUrl = "http://127.0.0.1:5000/processResults";

            ResponseEntity<String> response = restTemplate.postForEntity(serverUrl, requestEntity, String.class);

            System.out.println("Response for sessionId " + sessionId + ": " + response.getBody());

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                String analysis = rootNode.get("processed_results").get(0).get("analysis").asText();

                imageRepository.findAllBySessionId(UUID.fromString(sessionId)).forEach(image -> {
                    image.setSessionDetails(analysis);
                    imageRepository.save(image);
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error processing JSON response", e);
            }
        });
    }

    List<ReportResponse> prepareReportResults(Long companyId) {
        if (imageRepository.existsBySessionDetailsEmpty()) {
            sendProcessResultsToAIService(companyId);
        }

        List<Image> images = imageRepository.findAllByProcessStatusAndIsPhotoAndCompanyId(true, true, companyId).stream().toList();

        if (images.isEmpty()) {
            return null;
        }
        List<ReportResponse> results = new ArrayList<>();
        images.forEach(image -> {
            ReportResponse reportResponse = new ReportResponse();
            reportResponse.setName(image.getName());
            reportResponse.setAnalysis(image.getProcessResult());
            reportResponse.setSessionId(image.getSessionId().toString());
            reportResponse.setImageData(image.getImageData());
            reportResponse.setSessionDetails(image.getSessionDetails());
            results.add(reportResponse);
        });
        sendReportResultsByEmail(companyId, results);

        return results;
    }

    public void deleteAllReports() {
        imageRepository.deleteAll();
    }

    public void sendReportResultsByEmail(Long companyId, List<ReportResponse> reportResults) {
        EmailService emailService = new EmailService();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        if (reportResults == null || reportResults.isEmpty()) {
            throw new RuntimeException("No report results available for the company.");
        }

        StringBuilder emailContent = new StringBuilder();
        emailContent.append("Dear ").append(company.getName()).append(" Representative,\n\n");
        emailContent.append("Here are your report results:\n");
        emailContent.append("Results for session: ").append(reportResults.getFirst().getSessionId()).append("\n\n");
        emailContent.append("Session Summary Keyword: ").append(reportResults.getFirst().getSessionDetails()).append("\n");
        emailContent.append("Total user usage count (after last session): ").append(company.getUsageCount()).append("\n\n");
        emailContent.append("For more information, use the EAMAI app.\n");
        emailContent.append("Best regards,\nEAMAI Service");

        emailService.sendEmailWithAttachment(
                company.getEmail(),
                "Your Report Results",
                emailContent.toString(),
                null,
                null
        );
    }
}

