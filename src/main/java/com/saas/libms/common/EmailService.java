package com.saas.libms.common;


import com.saas.libms.exception.InternalServerException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

@Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendInstitutionVerificationCode(String toEmail,
                                                String orgName,
                                                String code) {

        String subject = "Verify your Library System account ";
        String body = buildVerificationEmailBody(orgName, code);
        sendHtmlEmail(toEmail,subject, body);

    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlBody){

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,true,"UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody,true);

            mailSender.send(message);
            log.info("Email sent to: {}", toEmail);

        } catch (MessagingException ex) {
            log.error("Failed to send email to {}: {}", toEmail, ex.getMessage());
            throw new InternalServerException("Failed to send verification email. Please try again");
        }
    }

    private String buildVerificationEmailBody(String orgName, String code) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: auto;">
                    <h2 style="color: #2c3e50;">Library Management System</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>Thank you for registering. Use the code below to verify your institution:</p>
                    <div style="
                        font-size: 32px;
                        font-weight: bold;
                        letter-spacing: 8px;
                        color: #2980b9;
                        background: #f0f4f8;
                        padding: 16px 24px;
                        border-radius: 8px;
                        display: inline-block;
                        margin: 16px 0;
                    ">%s</div>
                    <p style="color: #7f8c8d;">This code expires in <strong>5 minutes</strong>.</p>
                    <p style="color: #7f8c8d; font-size: 13px;">
                        If you did not register, please ignore this email.
                    </p>
                </div>
                """.formatted(orgName, code);
    }


}
