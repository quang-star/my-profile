package dev.quang.portfolio.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetMailService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String smtpHost;
    private final String smtpUsername;
    private final String mailFrom;
    private final String frontendUrl;
    private final int expirationMinutes;

    public PasswordResetMailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${spring.mail.username:}") String smtpUsername,
            @Value("${app.mail.from:}") String mailFrom,
            @Value("${app.frontend-url:http://localhost:3005}") String frontendUrl,
            @Value("${app.password-reset.expiration-minutes:30}") int expirationMinutes
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.smtpHost = smtpHost == null ? "" : smtpHost.trim();
        this.smtpUsername = smtpUsername == null ? "" : smtpUsername.trim();
        this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
        this.frontendUrl = frontendUrl == null ? "http://localhost:3005" : frontendUrl.trim().replaceAll("/+$", "");
        this.expirationMinutes = expirationMinutes > 0 ? expirationMinutes : 30;
    }

    public boolean isSmtpConfigured() {
        return !smtpHost.isEmpty() && mailSenderProvider.getIfAvailable() != null;
    }

    public int getExpirationMinutes() {
        return expirationMinutes;
    }

    public String buildResetLink(String rawToken) {
        return frontendUrl + "/reset-password?token=" + rawToken;
    }

    public boolean sendResetEmail(String toEmail, String displayName, String rawToken) {
        String resetLink = buildResetLink(rawToken);
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (smtpHost.isEmpty() || mailSender == null) {
            log.warn("SMTP is not configured (SMTP_HOST is empty). Password reset link for {}: {}", toEmail, resetLink);
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            String fromAddress = !mailFrom.isEmpty() ? mailFrom : smtpUsername;
            if (!fromAddress.isEmpty()) {
                helper.setFrom(fromAddress);
            }
            helper.setTo(toEmail);
            helper.setSubject("Đặt lại mật khẩu / Reset your password — portfolio.studio");

            String safeName = escapeHtml(displayName == null || displayName.isBlank() ? toEmail : displayName);
            String safeLink = escapeHtml(resetLink);

            String html = """
                    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 520px; margin: 0 auto; padding: 24px; color: #18181b;">
                      <h2 style="margin: 0 0 12px; font-size: 20px; color: #1d4ed8;">portfolio.studio</h2>
                      <p style="margin: 0 0 12px; font-size: 14px; line-height: 1.6;">
                        Xin chào <strong>%s</strong>,
                      </p>
                      <p style="margin: 0 0 18px; font-size: 14px; line-height: 1.6;">
                        Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Nhấn vào nút bên dưới để tạo mật khẩu mới (hiệu lực trong <strong>%d phút</strong>):
                      </p>
                      <p style="margin: 0 0 20px;">
                        <a href="%s" style="display: inline-block; background: #1d4ed8; color: #ffffff; text-decoration: none; font-weight: 600; font-size: 14px; padding: 11px 22px; border-radius: 10px;">
                          Đặt lại mật khẩu / Reset Password
                        </a>
                      </p>
                      <p style="margin: 0 0 12px; font-size: 12px; color: #71717a; word-break: break-all;">
                        Hoặc mở đường dẫn này: <a href="%s" style="color: #1d4ed8;">%s</a>
                      </p>
                      <hr style="border: none; border-top: 1px solid #e4e4e7; margin: 20px 0;" />
                      <p style="margin: 0; font-size: 12px; color: #71717a;">
                        Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này. Tài khoản của bạn vẫn an toàn.
                      </p>
                    </div>
                    """.formatted(safeName, expirationMinutes, safeLink, safeLink, safeLink);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send password reset email to {}: {}", toEmail, ex.getMessage(), ex);
            return false;
        }
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
