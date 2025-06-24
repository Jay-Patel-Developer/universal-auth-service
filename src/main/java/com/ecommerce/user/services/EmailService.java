package com.ecommerce.user.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Service for sending emails
 */
@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.enabled:false}")
    private boolean emailEnabled;
    
    @Value("${app.mail.from:noreply@ecommerce.com}")
    private String fromEmail;
    
    @Value("${app.name:ECommerce Platform}")
    private String appName;
    
    /**
     * Send a simple text email
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param text Email content
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendEmail(String to, String subject, String text) {
        if (!emailEnabled || mailSender == null) {
            logger.info("Email disabled. Would send email to {}: {} - {}", to, subject, text);
            return true; // Simulate success when disabled
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            logger.info("Email sent successfully to {}: {}", to, subject);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
            return false;
        }
    }
    
    /**
     * Send an HTML email
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param htmlContent HTML email content
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!emailEnabled || mailSender == null) {
            logger.info("Email disabled. Would send HTML email to {}: {}", to, subject);
            return true; // Simulate success when disabled
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("HTML email sent successfully to {}: {}", to, subject);
            return true;
            
        } catch (MessagingException e) {
            logger.error("Failed to send HTML email to {}: {}", to, e.getMessage());
            return false;
        }
    }
    
    /**
     * Send MFA verification code email
     * 
     * @param to Recipient email address
     * @param code Verification code
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendMfaVerificationCode(String to, String code) {
        String subject = appName + " - Verification Code";
        String htmlContent = buildMfaEmailTemplate(code);
        
        if (emailEnabled && mailSender != null) {
            return sendHtmlEmail(to, subject, htmlContent);
        } else {
            // Fallback to simple text email
            String textContent = String.format(
                "Your %s verification code is: %s\n\n" +
                "This code will expire in 10 minutes.\n" +
                "If you didn't request this code, please ignore this email.",
                appName, code
            );
            return sendEmail(to, subject, textContent);
        }
    }
    
    /**
     * Send password reset email
     * 
     * @param to Recipient email address
     * @param resetToken Password reset token
     * @param resetUrl Reset URL
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendPasswordResetEmail(String to, String resetToken, String resetUrl) {
        String subject = appName + " - Password Reset";
        String htmlContent = buildPasswordResetEmailTemplate(resetUrl, resetToken);
        
        if (emailEnabled && mailSender != null) {
            return sendHtmlEmail(to, subject, htmlContent);
        } else {
            // Fallback to simple text email
            String textContent = String.format(
                "You requested a password reset for your %s account.\n\n" +
                "Click the following link to reset your password:\n%s\n\n" +
                "This link will expire in 1 hour.\n" +
                "If you didn't request this reset, please ignore this email.",
                appName, resetUrl
            );
            return sendEmail(to, subject, textContent);
        }
    }
    
    /**
     * Build HTML template for MFA verification email
     */
    private String buildMfaEmailTemplate(String code) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Verification Code</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: #f4f4f4; padding: 20px; border-radius: 10px; }
                    .content { background: white; padding: 30px; border-radius: 5px; }
                    .code { font-size: 24px; font-weight: bold; color: #2c3e50; background: #ecf0f1; padding: 15px; text-align: center; border-radius: 5px; margin: 20px 0; }
                    .footer { margin-top: 20px; font-size: 12px; color: #7f8c8d; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="content">
                        <h2>%s - Verification Code</h2>
                        <p>Your verification code is:</p>
                        <div class="code">%s</div>
                        <p>This code will expire in 10 minutes.</p>
                        <p>If you didn't request this code, please ignore this email.</p>
                        <div class="footer">
                            <p>This is an automated email. Please do not reply.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, appName, code);
    }
    
    /**
     * Build HTML template for password reset email
     */
    private String buildPasswordResetEmailTemplate(String resetUrl, String token) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Password Reset</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: #f4f4f4; padding: 20px; border-radius: 10px; }
                    .content { background: white; padding: 30px; border-radius: 5px; }
                    .button { display: inline-block; background: #3498db; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { margin-top: 20px; font-size: 12px; color: #7f8c8d; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="content">
                        <h2>%s - Password Reset</h2>
                        <p>You requested a password reset for your account.</p>
                        <p>Click the button below to reset your password:</p>
                        <a href="%s" class="button">Reset Password</a>
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all;">%s</p>
                        <p>This link will expire in 1 hour.</p>
                        <p>If you didn't request this reset, please ignore this email.</p>
                        <div class="footer">
                            <p>This is an automated email. Please do not reply.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, appName, resetUrl, resetUrl);
    }
    
    /**
     * Validate email address format
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }
}
