package com.uokaradeniz.backend.report;

import com.uokaradeniz.backend.company.Company;
import com.uokaradeniz.backend.company.CompanyRepository;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Map;

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

    public void sendReportAsPdf(Long companyId, String reportContent, BufferedImage[] chartsArray) {
        byte[] pdfData = pdfGenerator.generateReport(reportContent, chartsArray);

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


    public BufferedImage createCompanyResultsBarChart(Map<String, Integer> data) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        data.forEach((k, v) -> dataset.addValue(v, "Count", k));
        JFreeChart barChart = ChartFactory.createBarChart(
                "Total Results of All Company Sessions",
                "Emotion",
                "Count",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                false,
                false
        );
        return barChart.createBufferedImage(500, 290);
    }

    public BufferedImage createCompanyUsagePieChart(Map<String, Integer> data) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        data.forEach(dataset::setValue);
        JFreeChart pieChart = ChartFactory.createPieChart(
                "All Registered Companies User Usages",
                dataset,
                true,
                true,
                false
        );
        PieSectionLabelGenerator labelGenerator = new StandardPieSectionLabelGenerator(
                "{0}: {1} ({2})", new DecimalFormat("0"), new DecimalFormat("0%"));
        PiePlot<?> plot = (PiePlot<?>) pieChart.getPlot();
        plot.setLabelGenerator(labelGenerator);
        return pieChart.createBufferedImage(500, 290);
    }
}