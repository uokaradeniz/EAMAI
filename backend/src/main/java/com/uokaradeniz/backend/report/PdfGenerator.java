package com.uokaradeniz.backend.report;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

@Service
public class PdfGenerator {
    public byte[] generateReport(String content, BufferedImage[] chartsArray) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            Document document = new Document(new com.itextpdf.kernel.pdf.PdfDocument(writer));
            document.add(new Paragraph(content));

            Arrays.stream(chartsArray).toList().forEach(chart -> {
                try {
                    Image chartImage = bufferedImageToITextImage(chart);
                    document.add(chartImage);
                    document.add(new Paragraph("\n"));
                } catch (IOException e) {
                    throw new RuntimeException("Error converting BufferedImage to iText Image", e);
                }
            });
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    public Image bufferedImageToITextImage(BufferedImage chartImage) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(chartImage, "png", baos);
        return new Image(ImageDataFactory.create(baos.toByteArray()));
    }
}