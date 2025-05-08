package com.uokaradeniz.backend.report;

import jakarta.activation.DataSource;
import jakarta.activation.DataHandler;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class EmailService {

    private final String mailUsername = System.getenv("GMAIL_USER");
    private final String mailPassword = System.getenv("GMAIL_PASSWORD");

    public void sendEmailWithAttachment(String to, String subject, String text, byte[] attachment, String attachmentName) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        String from = mailUsername;
        String password = mailPassword;

        Session session = Session.getInstance(props);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(text);
            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            if (attachment != null) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                DataSource dataSource = new ByteArrayDataSource(attachment, "application/pdf");
                attachmentPart.setDataHandler(new DataHandler(dataSource));
                attachmentPart.setFileName(attachmentName);
                multipart.addBodyPart(attachmentPart);
            }

            message.setContent(multipart);

            Transport.send(message, from, password);
        } catch (Exception e) {
            throw new RuntimeException("Error sending email", e);
        }
    }
}