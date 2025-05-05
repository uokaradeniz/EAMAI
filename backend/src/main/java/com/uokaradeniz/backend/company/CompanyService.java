package com.uokaradeniz.backend.company;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uokaradeniz.backend.report.EmailService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company registerCompany(String name, String email) {
        Company company = new Company();
        company.setName(name);
        company.setEmail(email);
        company.setApiKey(generateRandomApiKey());
        companyRepository.save(company);
        sendCompanyGreetingMail(company);
        return company;
    }

    private String generateRandomApiKey() {
        return String.format("%06d", (int) (Math.random() * 1_000_000));
    }

    public Optional<Company> authenticate(String jsonPayload) {
        String companyKey = "";
        try {
            JsonNode rootNode = new ObjectMapper().readTree(jsonPayload);
            companyKey = rootNode.get("companyKey").asText();
        } catch (IOException e) {
            throw new RuntimeException("Invalid JSON payload", e);
        }
        return companyRepository.findByApiKey(companyKey);
    }

    public void sendCompanyGreetingMail(Company company) {
        EmailService emailService = new EmailService();
        emailService.sendEmailWithAttachment(company.getEmail(), "Welcome to EAMAI",
                "Dear " + company.getName() + " Representative,\n\n" +
                        "Your tester account has been activated.\n " +
                        "Your company entry key is: " + company.getApiKey() + "\n\n" +
                        "Best regards,\n" +
                        "Uğur Oğuzhan Karadeniz", null, null);
    }
}