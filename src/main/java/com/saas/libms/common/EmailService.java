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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    //Reservation
    // ─────────────────────────────────────────────────────────────────────────────
// Add these methods to your existing EmailService.java
// ─────────────────────────────────────────────────────────────────────────────

// ─── PUBLIC EMAIL METHODS ─────────────────────────────────────────────────────

    @Async
    public void sendReservationFulfilledEmail(String toEmail,
                                              String memberName,
                                              String bookTitle,
                                              String reservationPublicId,
                                              LocalDateTime reservedUntil) {
        String subject = "📖 Your Reserved Book Is Ready for Collection!";
        String body = buildFulfilledEmailBody(memberName, bookTitle, reservationPublicId, reservedUntil);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendReservationExpiredEmail(String toEmail,
                                            String memberName,
                                            String bookTitle,
                                            String reservationPublicId) {
        String subject = "⏰ Your Book Reservation Has Expired";
        String body = buildExpiredEmailBody(memberName, bookTitle, reservationPublicId);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendReservationCancelledEmail(String toEmail,
                                              String memberName,
                                              String bookTitle,
                                              String reservationPublicId,
                                              String reason) {
        String subject = "❌ Your Reservation Has Been Cancelled";
        String body = buildCancelledEmailBody(memberName, bookTitle, reservationPublicId, reason);
        sendHtmlEmail(toEmail, subject, body);
    }

// ─── PRIVATE EMAIL BODY BUILDERS ─────────────────────────────────────────────

    private String buildFulfilledEmailBody(String memberName,
                                           String bookTitle,
                                           String reservationId,
                                           LocalDateTime reservedUntil) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy 'at' HH:mm");
        String deadline = reservedUntil.format(formatter);

        return """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 560px; margin: auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 24px rgba(0,0,0,0.10);">

              <!-- Header -->
              <div style="background: linear-gradient(135deg, #1a7a4a 0%%, #2ecc71 100%%); padding: 36px 32px 28px;">
                <div style="font-size: 36px; margin-bottom: 8px;">📗</div>
                <h1 style="color: #ffffff; font-size: 22px; margin: 0; font-weight: 700; letter-spacing: -0.3px;">
                  Your Book Is Ready!
                </h1>
                <p style="color: rgba(255,255,255,0.85); margin: 6px 0 0; font-size: 14px;">
                  Great news — your reserved copy is waiting for you.
                </p>
              </div>

              <!-- Body -->
              <div style="padding: 32px;">
                <p style="color: #2d3748; font-size: 15px; margin: 0 0 20px;">
                  Hello <strong>%s</strong>,
                </p>
                <p style="color: #4a5568; font-size: 15px; line-height: 1.6; margin: 0 0 24px;">
                  A copy of the book you reserved has become available and has been set aside specifically for you.
                  Please visit the library to collect it.
                </p>

                <!-- Book Card -->
                <div style="background: #f7f9fc; border-left: 4px solid #2ecc71; border-radius: 8px; padding: 20px 24px; margin-bottom: 24px;">
                  <p style="color: #718096; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; margin: 0 0 6px;">Reserved Book</p>
                  <p style="color: #1a202c; font-size: 18px; font-weight: 700; margin: 0 0 12px;">%s</p>
                  <p style="color: #718096; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; margin: 0 0 4px;">Reservation ID</p>
                  <p style="color: #2d3748; font-size: 14px; font-weight: 600; font-family: monospace; margin: 0;">%s</p>
                </div>

                <!-- Deadline Warning -->
                <div style="background: #fff8e1; border: 1px solid #ffd54f; border-radius: 8px; padding: 16px 20px; margin-bottom: 28px; display: flex; align-items: flex-start; gap: 12px;">
                  <span style="font-size: 22px; flex-shrink: 0;">⏰</span>
                  <div>
                    <p style="color: #7d5100; font-size: 14px; font-weight: 700; margin: 0 0 4px;">Collection Deadline</p>
                    <p style="color: #8d6e00; font-size: 14px; margin: 0;">
                      Please collect by <strong>%s</strong>
                    </p>
                    <p style="color: #a07800; font-size: 12px; margin: 6px 0 0;">
                      If not collected in time, the reservation will expire and the copy will be returned to the general shelf.
                    </p>
                  </div>
                </div>

                <p style="color: #718096; font-size: 13px; line-height: 1.6; border-top: 1px solid #e2e8f0; padding-top: 20px; margin: 0;">
                  This is an automated notification from your Library Management System.
                  Please do not reply to this email.
                </p>
              </div>

            </div>
            """.formatted(memberName, bookTitle, reservationId, deadline);
    }

    private String buildExpiredEmailBody(String memberName,
                                         String bookTitle,
                                         String reservationId) {
        return """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 560px; margin: auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 24px rgba(0,0,0,0.10);">

              <!-- Header -->
              <div style="background: linear-gradient(135deg, #c0392b 0%%, #e74c3c 100%%); padding: 36px 32px 28px;">
                <div style="font-size: 36px; margin-bottom: 8px;">⏰</div>
                <h1 style="color: #ffffff; font-size: 22px; margin: 0; font-weight: 700; letter-spacing: -0.3px;">
                  Reservation Expired
                </h1>
                <p style="color: rgba(255,255,255,0.85); margin: 6px 0 0; font-size: 14px;">
                  Your collection window has passed.
                </p>
              </div>

              <!-- Body -->
              <div style="padding: 32px;">
                <p style="color: #2d3748; font-size: 15px; margin: 0 0 20px;">
                  Hello <strong>%s</strong>,
                </p>
                <p style="color: #4a5568; font-size: 15px; line-height: 1.6; margin: 0 0 24px;">
                  Unfortunately, your 48-hour collection window for the following book has passed.
                  The copy has been returned to the general shelf for other members.
                </p>

                <!-- Book Card -->
                <div style="background: #fff5f5; border-left: 4px solid #e74c3c; border-radius: 8px; padding: 20px 24px; margin-bottom: 24px;">
                  <p style="color: #718096; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; margin: 0 0 6px;">Expired Reservation</p>
                  <p style="color: #1a202c; font-size: 18px; font-weight: 700; margin: 0 0 12px;">%s</p>
                  <p style="color: #718096; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; margin: 0 0 4px;">Reservation ID</p>
                  <p style="color: #2d3748; font-size: 14px; font-weight: 600; font-family: monospace; margin: 0;">%s</p>
                </div>

                <!-- Action Suggestion -->
                <div style="background: #f0f7ff; border: 1px solid #bee3f8; border-radius: 8px; padding: 16px 20px; margin-bottom: 28px;">
                  <p style="color: #2b6cb0; font-size: 14px; font-weight: 700; margin: 0 0 6px;">💡 Want to try again?</p>
                  <p style="color: #3182ce; font-size: 14px; margin: 0; line-height: 1.5;">
                    You can place a new reservation for this book by visiting the library or contacting a librarian.
                    You will be placed at the end of the queue.
                  </p>
                </div>

                <p style="color: #718096; font-size: 13px; line-height: 1.6; border-top: 1px solid #e2e8f0; padding-top: 20px; margin: 0;">
                  This is an automated notification from your Library Management System.
                  Please do not reply to this email.
                </p>
              </div>

            </div>
            """.formatted(memberName, bookTitle, reservationId);
    }

    private String buildCancelledEmailBody(String memberName,
                                           String bookTitle,
                                           String reservationId,
                                           String reason) {
        return """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 560px; margin: auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 24px rgba(0,0,0,0.10);">

              <!-- Header -->
              <div style="background: linear-gradient(135deg, #6c757d 0%%, #495057 100%%); padding: 36px 32px 28px;">
                <div style="font-size: 36px; margin-bottom: 8px;">📋</div>
                <h1 style="color: #ffffff; font-size: 22px; margin: 0; font-weight: 700; letter-spacing: -0.3px;">
                  Reservation Cancelled
                </h1>
                <p style="color: rgba(255,255,255,0.85); margin: 6px 0 0; font-size: 14px;">
                  Your reservation has been cancelled by library staff.
                </p>
              </div>

              <!-- Body -->
              <div style="padding: 32px;">
                <p style="color: #2d3748; font-size: 15px; margin: 0 0 20px;">
                  Hello <strong>%s</strong>,
                </p>
                <p style="color: #4a5568; font-size: 15px; line-height: 1.6; margin: 0 0 24px;">
                  We're writing to let you know that your reservation for the following book has been cancelled.
                </p>

                <!-- Book Card -->
                <div style="background: #f7f7f7; border-left: 4px solid #6c757d; border-radius: 8px; padding: 20px 24px; margin-bottom: 24px;">
                  <p style="color: #718096; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; margin: 0 0 6px;">Cancelled Book</p>
                  <p style="color: #1a202c; font-size: 18px; font-weight: 700; margin: 0 0 12px;">%s</p>
                  <p style="color: #718096; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; margin: 0 0 4px;">Reservation ID</p>
                  <p style="color: #2d3748; font-size: 14px; font-weight: 600; font-family: monospace; margin: 0;">%s</p>
                </div>

                <!-- Reason -->
                <div style="background: #fff8e1; border: 1px solid #ffd54f; border-radius: 8px; padding: 16px 20px; margin-bottom: 28px;">
                  <p style="color: #7d5100; font-size: 13px; font-weight: 700; margin: 0 0 6px; text-transform: uppercase; letter-spacing: 0.5px;">Reason for Cancellation</p>
                  <p style="color: #5d4037; font-size: 15px; margin: 0; line-height: 1.6; font-style: italic;">"%s"</p>
                </div>

                <p style="color: #4a5568; font-size: 14px; line-height: 1.6; margin: 0 0 24px;">
                  If you believe this cancellation was made in error, or if you have any questions,
                  please visit the library or contact your librarian directly.
                </p>

                <p style="color: #718096; font-size: 13px; line-height: 1.6; border-top: 1px solid #e2e8f0; padding-top: 20px; margin: 0;">
                  This is an automated notification from your Library Management System.
                  Please do not reply to this email.
                </p>
              </div>

            </div>
            """.formatted(memberName, bookTitle, reservationId, reason);
    }


}
