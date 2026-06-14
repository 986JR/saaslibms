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

    @Async
    public void sendReservationCollectedEmail(
            String toEmail,
            String memberName,
            String bookTitle,
            String reservationPublicId,
            String loanPublicId
    ) {
        String subject = "✅ Reserved Book Collected — Loan Active";

        String htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8"/>
              <style>
                body        { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 0; }
                .container  { max-width: 600px; margin: 30px auto; background: #fff;
                               border-radius: 10px; overflow: hidden;
                               box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
                .header     { background: linear-gradient(135deg, #16a34a, #15803d);
                               padding: 30px 20px; text-align: center; }
                .header h1  { color: #fff; margin: 0; font-size: 22px; }
                .body       { padding: 30px 25px; color: #333; }
                .card       { background: #f0fdf4; border-left: 4px solid #16a34a;
                               border-radius: 6px; padding: 16px 20px; margin: 20px 0; }
                .card p     { margin: 6px 0; font-size: 15px; }
                .label      { font-weight: bold; color: #15803d; }
                .footer     { background: #f9f9f9; text-align: center; padding: 16px;
                               font-size: 12px; color: #999; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>✅ Book Collected Successfully</h1>
                </div>
                <div class="body">
                  <p>Dear <strong>%s</strong>,</p>
                  <p>
                    Your reserved book has been collected and your loan is now active.
                    Please return the book by the due date shown at the library counter.
                  </p>
                  <div class="card">
                    <p><span class="label">Book:</span> %s</p>
                    <p><span class="label">Reservation:</span> %s</p>
                    <p><span class="label">Loan ID:</span> %s</p>
                  </div>
                  <p>
                    If you did not collect this book or believe this is an error,
                    please contact the library immediately.
                  </p>
                </div>
                <div class="footer">
                  This is an automated message from the Library Management System.
                </div>
              </div>
            </body>
            </html>
            """.formatted(memberName, bookTitle, reservationPublicId, loanPublicId);

        sendHtmlEmail(toEmail, subject, htmlBody);
        log.info("Reservation-collected email sent to '{}' for book '{}'.", toEmail, bookTitle);
    }


    // ─────────────────────────────────────────────────────────────────────────────
// ADD THIS METHOD to your existing EmailService.java in common/
// Place it alongside the other send*Email methods.
// ─────────────────────────────────────────────────────────────────────────────

    /**
     * Sends a styled HTML password reset email.
     *
     * @param toEmail          The user's email address
     * @param username         The user's display name (shown in the greeting)
     * @param resetLink        Full URL the user clicks to reset (contains raw token)
     * @param rawToken         The 6-character token (shown separately in the email for copy-paste)
     * @param expiryMinutes    How many minutes before the token expires (shown to the user)
     */
    @Async
    public void sendPasswordResetEmail(String toEmail,
                                       String username,
                                       String resetLink,
                                       String rawToken,
                                       int expiryMinutes) {
        String subject = "🔐 Reset Your Library System Password";

        String html = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
          <title>Password Reset</title>
        </head>
        <body style="margin:0;padding:0;background-color:#0f1117;font-family:'Segoe UI',Roboto,Arial,sans-serif;">

          <!-- Outer wrapper -->
          <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#0f1117;padding:40px 0;">
            <tr>
              <td align="center">

                <!-- Email card -->
                <table width="600" cellpadding="0" cellspacing="0"
                       style="background-color:#1a1d27;border-radius:16px;overflow:hidden;
                              box-shadow:0 8px 40px rgba(0,0,0,0.5);max-width:600px;width:100%%;">

                  <!-- ── HERO HEADER ── -->
                  <tr>
                    <td style="background:linear-gradient(135deg,#6c3fff 0%%,#a855f7 50%%,#ec4899 100%%);
                                padding:48px 40px 40px;text-align:center;">

                      <!-- Lock icon circle -->
                      <div style="width:72px;height:72px;background:rgba(255,255,255,0.15);
                                  border-radius:50%%;margin:0 auto 20px;
                                  display:flex;align-items:center;justify-content:center;
                                  font-size:36px;line-height:72px;">
                        🔐
                      </div>

                      <h1 style="margin:0 0 8px;color:#ffffff;font-size:26px;font-weight:700;
                                 letter-spacing:-0.5px;">
                        Password Reset Request
                      </h1>
                      <p style="margin:0;color:rgba(255,255,255,0.8);font-size:15px;">
                        Library Management System
                      </p>
                    </td>
                  </tr>

                  <!-- ── BODY ── -->
                  <tr>
                    <td style="padding:40px;">

                      <!-- Greeting -->
                      <p style="margin:0 0 8px;color:#a0a3b1;font-size:13px;
                                 text-transform:uppercase;letter-spacing:1px;font-weight:600;">
                        Hello,
                      </p>
                      <h2 style="margin:0 0 20px;color:#ffffff;font-size:22px;font-weight:600;">
                        %s
                      </h2>

                      <p style="margin:0 0 28px;color:#c4c7d4;font-size:15px;line-height:1.7;">
                        We received a request to reset the password for your Library Management System account.
                        Click the button below to choose a new password. This link is valid for
                        <strong style="color:#a855f7;">%d minutes</strong>.
                      </p>

                      <!-- ── CTA BUTTON ── -->
                      <table width="100%%" cellpadding="0" cellspacing="0">
                        <tr>
                          <td align="center" style="padding:0 0 32px;">
                            <a href="%s"
                               style="display:inline-block;background:linear-gradient(135deg,#6c3fff,#a855f7);
                                      color:#ffffff;font-size:16px;font-weight:700;text-decoration:none;
                                      padding:16px 48px;border-radius:12px;letter-spacing:0.3px;
                                      box-shadow:0 4px 20px rgba(108,63,255,0.5);">
                              Reset My Password →
                            </a>
                          </td>
                        </tr>
                      </table>

                      <!-- ── TOKEN CARD (copy-paste fallback) ── -->
                      <table width="100%%" cellpadding="0" cellspacing="0"
                             style="background-color:#0f1117;border:1px solid #2e3147;
                                    border-radius:12px;margin-bottom:28px;">
                        <tr>
                          <td style="padding:24px;text-align:center;">
                            <p style="margin:0 0 12px;color:#a0a3b1;font-size:12px;
                                       text-transform:uppercase;letter-spacing:1.5px;font-weight:600;">
                              Or use this reset code
                            </p>
                            <p style="margin:0;color:#a855f7;font-size:32px;font-weight:800;
                                       letter-spacing:8px;font-family:'Courier New',monospace;">
                              %s
                            </p>
                            <p style="margin:10px 0 0;color:#6b6f85;font-size:12px;">
                              Paste this code on the reset page if the button doesn't work
                            </p>
                          </td>
                        </tr>
                      </table>

                      <!-- ── EXPIRY WARNING BANNER ── -->
                      <table width="100%%" cellpadding="0" cellspacing="0"
                             style="background-color:#2d1f0e;border:1px solid #7c4a1e;
                                    border-radius:10px;margin-bottom:28px;">
                        <tr>
                          <td style="padding:16px 20px;">
                            <p style="margin:0;color:#f59e0b;font-size:14px;">
                              ⏱️  <strong>This link expires in %d minutes.</strong>
                              If you don't reset your password before then,
                              you'll need to request a new link.
                            </p>
                          </td>
                        </tr>
                      </table>

                      <!-- ── SECURITY NOTICE ── -->
                      <table width="100%%" cellpadding="0" cellspacing="0"
                             style="background-color:#0d1f1a;border:1px solid #1a4a36;
                                    border-radius:10px;margin-bottom:32px;">
                        <tr>
                          <td style="padding:16px 20px;">
                            <p style="margin:0;color:#4ade80;font-size:14px;">
                              🛡️  <strong>Didn't request this?</strong>
                              Your password will not change unless you click the link above.
                              You can safely ignore this email.
                            </p>
                          </td>
                        </tr>
                      </table>

                      <!-- ── LINK FALLBACK ── -->
                      <p style="margin:0 0 6px;color:#6b6f85;font-size:12px;">
                        If the button isn't clickable, copy and paste this URL into your browser:
                      </p>
                      <p style="margin:0;word-break:break-all;">
                        <a href="%s" style="color:#a855f7;font-size:12px;text-decoration:underline;">
                          %s
                        </a>
                      </p>

                    </td>
                  </tr>

                  <!-- ── FOOTER ── -->
                  <tr>
                    <td style="background-color:#13151f;padding:24px 40px;
                                border-top:1px solid #2e3147;text-align:center;">
                      <p style="margin:0 0 6px;color:#4b4f65;font-size:12px;">
                        This email was sent by the Library Management System.
                      </p>
                      <p style="margin:0;color:#4b4f65;font-size:11px;">
                        For security, this link can only be used once and expires in %d minutes.
                      </p>
                    </td>
                  </tr>

                </table>
                <!-- /email card -->

              </td>
            </tr>
          </table>

        </body>
        </html>
        """.formatted(
                username,          // greeting name
                expiryMinutes,     // "valid for X minutes" in intro
                resetLink,         // href on the button
                rawToken,          // 6-char code in the token card
                expiryMinutes,     // expiry warning banner
                resetLink,         // href in the fallback link
                resetLink,         // displayed text of fallback link
                expiryMinutes      // footer expiry note
        );

        sendHtmlEmail(toEmail, subject, html);
    }

    // EmailService.java — add this method

    public void sendInstitutionVerificationLink(String email, String institutionName, String verificationLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Verify Your Institution Email — SaasLib");
            helper.setText(buildVerificationEmailHtml(institutionName, verificationLink), true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", email);

        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", email, e.getMessage());
            throw new InternalServerException("Failed to send verification email");
        }
    }

    private String buildVerificationEmailHtml(String institutionName, String verificationLink) {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
          <title>Verify Your Email</title>
        </head>
        <body style="margin:0; padding:0; background-color:#f4f6f9; font-family: 'Segoe UI', Arial, sans-serif;">

          <table width="100%%" cellpadding="0" cellspacing="0" style="padding: 40px 0;">
            <tr>
              <td align="center">

                <!-- Card -->
                <table width="600" cellpadding="0" cellspacing="0"
                       style="background:#ffffff; border-radius:10px;
                              box-shadow: 0 2px 12px rgba(0,0,0,0.08); overflow:hidden;">

                  <!-- Header -->
                  <tr>
                    <td style="background: #1a1f36; padding: 36px 40px; text-align:center;">
                      <h1 style="margin:0; color:#ffffff; font-size:22px; font-weight:700;
                                 letter-spacing:0.5px;">
                        📚 SaasLib
                      </h1>
                      <p style="margin:8px 0 0; color:#9ba5c4; font-size:13px;">
                        Library Management System
                      </p>
                    </td>
                  </tr>

                  <!-- Body -->
                  <tr>
                    <td style="padding: 40px 40px 32px;">

                      <h2 style="margin:0 0 12px; color:#1a1f36; font-size:20px; font-weight:600;">
                        Verify Your Institution Email
                      </h2>

                      <p style="margin:0 0 10px; color:#4a5568; font-size:15px; line-height:1.6;">
                        Hi, <strong>%s</strong>,
                      </p>

                      <p style="margin:0 0 24px; color:#4a5568; font-size:15px; line-height:1.6;">
                        Thank you for registering your institution on SaasLib. Click the button
                        below to verify your email address and continue setting up your library.
                      </p>

                      <!-- Button -->
                      <table cellpadding="0" cellspacing="0" style="margin: 0 auto 28px;">
                        <tr>
                          <td align="center"
                              style="background:#4f46e5; border-radius:8px;">
                            <a href="%s"
                               style="display:inline-block; padding:14px 36px;
                                      color:#ffffff; font-size:15px; font-weight:600;
                                      text-decoration:none; letter-spacing:0.3px;">
                              Verify Email Address
                            </a>
                          </td>
                        </tr>
                      </table>

                      <!-- Fallback link -->
                      <p style="margin:0 0 8px; color:#718096; font-size:13px; text-align:center;">
                        Button not working? Copy and paste this link into your browser:
                      </p>
                      <p style="margin:0 0 28px; text-align:center;">
                        <a href="%s"
                           style="color:#4f46e5; font-size:12px; word-break:break-all;">
                          %s
                        </a>
                      </p>

                      <!-- Warning box -->
                      <table width="100%%" cellpadding="0" cellspacing="0"
                             style="background:#fff8e1; border-left:4px solid #f59e0b;
                                    border-radius:6px; margin-bottom:28px;">
                        <tr>
                          <td style="padding:14px 16px;">
                            <p style="margin:0; color:#92400e; font-size:13px; line-height:1.5;">
                              ⏱ This link expires in <strong>24 hours</strong>.
                              If it expires, you will need to register again.
                            </p>
                          </td>
                        </tr>
                      </table>

                      <p style="margin:0; color:#718096; font-size:13px; line-height:1.6;">
                        If you did not register an institution on SaasLib, you can safely
                        ignore this email. No account will be created.
                      </p>

                    </td>
                  </tr>

                  <!-- Footer -->
                  <tr>
                    <td style="background:#f8fafc; padding:20px 40px;
                               border-top:1px solid #e8ecf0; text-align:center;">
                      <p style="margin:0; color:#a0aec0; font-size:12px;">
                        © 2025 SaasLib · Multi-Tenant Library Management System
                      </p>
                      <p style="margin:6px 0 0; color:#a0aec0; font-size:12px;">
                        This is an automated message — please do not reply.
                      </p>
                    </td>
                  </tr>

                </table>
                <!-- End Card -->

              </td>
            </tr>
          </table>

        </body>
        </html>
        """.formatted(institutionName, verificationLink, verificationLink, verificationLink);
    }


}
