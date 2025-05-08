package com.uokaradeniz.backend.report;

import com.uokaradeniz.backend.company.Company;
import com.uokaradeniz.backend.company.CompanyRepository;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final PdfGenerator pdfGenerator;
    private final EmailService emailService;
    private final CompanyRepository companyRepository;

    public ReportService(PdfGenerator pdfGenerator, EmailService emailService, CompanyRepository companyRepository) {
        this.pdfGenerator = pdfGenerator;
        this.emailService = emailService;
        this.companyRepository = companyRepository;
    }

    public void sendReportAsPdf(Long companyId, String reportContent) {
        byte[] pdfData = pdfGenerator.generateReport(reportContent);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        emailService.sendEmailWithAttachment(
                company.getEmail(),
                "Your Report Results",
                "Dear " + company.getName() + " Representative,\n\nYour session results are attached.\n\nBest regards,\nEAMAI Service",
                pdfData,
                "Report.pdf"
        );
    }
}