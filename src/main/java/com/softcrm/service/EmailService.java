package com.softcrm.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    // ============ PUBLIC METHODS ============

    /**
     * Send welcome email with account credentials
     */
    public void sendWelcomeEmail(String toEmail, String name, String userType,
                                 String username, String password, String createdBy) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to SoftCRM - Your " + userType + " Account Created");

            String htmlContent = buildWelcomeEmailHtml(name, userType, username, password, createdBy);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
        }
    }

    /**
     * Send email verification link
     */
    public void sendVerificationEmail(String toEmail, String verificationUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("SoftCRM - Verify Your Email Address");

            String htmlContent = buildVerificationEmailHtml(verificationUrl);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
        }
    }

    /**
     * Send simple plain text email
     */
    public void sendSimpleEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", toEmail, e);
        }
    }

    /**
     * Send password reset OTP email
     */
    public void sendPasswordResetOtp(String toEmail, String otp) {
        String subject = "SoftCRM - Password Reset OTP";
        String body = "Your OTP to reset your password is: " + otp +
                "\n\nThis OTP is valid for 15 minutes.\n\n" +
                "If you didn't request this, please ignore this email.";
        sendSimpleEmail(toEmail, subject, body);
    }

    /**
     * Send 2FA OTP email
     */
    public void sendTwoFactorOtp(String toEmail, String otp) {
        String subject = "SoftCRM - Two Factor Authentication OTP";
        String body = "Your OTP for two-factor authentication is: " + otp +
                "\n\nThis OTP is valid for 5 minutes.\n\n" +
                "If you didn't attempt to login, please change your password immediately.";
        sendSimpleEmail(toEmail, subject, body);
    }

    // ============ PRIVATE HTML BUILDERS ============

    private String buildVerificationEmailHtml(String verificationUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 20px auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { padding: 30px; }
                    .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 8px; margin: 20px 0; font-weight: bold; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #718096; border-top: 1px solid #e2e8f0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📧 Verify Your Email</h1>
                        <p>Welcome to SoftCRM!</p>
                    </div>
                    <div class="content">
                        <p>Thank you for registering with SoftCRM. Please verify your email address to get started.</p>
                        <div style="text-align: center;">
                            <a href="%s" class="button">✅ Verify Email Address</a>
                        </div>
                        <p>Or copy and paste this link in your browser:</p>
                        <p style="background: #f8f9fa; padding: 10px; border-radius: 5px; word-break: break-all;">%s</p>
                        <p>This link will expire in 24 hours.</p>
                        <p>If you didn't create an account with SoftCRM, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2025 SoftCRM - Customer Relationship Management System</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(verificationUrl, verificationUrl);
    }

    private String buildWelcomeEmailHtml(String name, String userType,
                                         String username, String password, String createdBy) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 20px auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { padding: 30px; }
                    .credentials-box { background: #f8f9fa; border-left: 4px solid #667eea; padding: 20px; margin: 20px 0; border-radius: 8px; }
                    .credential-item { margin: 10px 0; font-family: monospace; font-size: 14px; }
                    .label { font-weight: bold; color: #4a5568; width: 100px; display: inline-block; }
                    .value { color: #2d3748; }
                    .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 8px; margin-top: 20px; font-weight: bold; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #718096; border-top: 1px solid #e2e8f0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Welcome to SoftCRM</h1>
                        <p>Your account has been created successfully</p>
                    </div>
                    <div class="content">
                        <p>Dear <strong>%s</strong>,</p>
                        <p>Good news! Your %s account has been created by <strong>%s</strong>.</p>
                        <div class="credentials-box">
                            <h3 style="margin-top: 0; color: #667eea;">🔐 Your Login Credentials</h3>
                            <div class="credential-item">
                                <span class="label">📧 Email:</span>
                                <span class="value">%s</span>
                            </div>
                            <div class="credential-item">
                                <span class="label">🔑 Password:</span>
                                <span class="value">%s</span>
                            </div>
                        </div>
                        <p><strong>⚠️ Important Security Tips:</strong></p>
                        <ul>
                            <li>Please change your password after first login</li>
                            <li>Do not share your credentials with anyone</li>
                            <li>Use a strong password with mix of letters, numbers, and symbols</li>
                        </ul>
                        <div style="text-align: center;">
                            <a href="http://localhost:8085" class="button">🚀 Login to SoftCRM</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>&copy; 2025 SoftCRM - Customer Relationship Management System</p>
                        <p>This is an automated email, please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(name, userType, createdBy, username, password);
    }
}