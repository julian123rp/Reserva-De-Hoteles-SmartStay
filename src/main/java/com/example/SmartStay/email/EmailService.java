package com.example.SmartStay.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class EmailService {

    private final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender javaMailSender;

    @Async
    public void sendEmail(String to, String subject, String text) {
        MimeMessage message = javaMailSender.createMimeMessage();

        // send email
        try {
            message.setFrom(new InternetAddress("contact@bookify.website"));
            message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(text, "text/html; charset=utf-8");

            javaMailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public String getTemplate(String template) {
        // get template
        String path = "src/main/resources/templates/" + template + ".html";

        // read file content
        String content = "";
        try {
            content = Files.readString(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }
}
