package com.apprh.backend.email.application;

import com.apprh.backend.email.infrastructure.MailProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties mailProperties;

    @Async("mailExecutor")
    public void send(String to, String subject, String message) {
        if (to == null || to.isBlank()) {
            return;
        }
        if (!mailProperties.enabled()) {
            log.info("[MAIL DÉSACTIVÉ] À : {} | Objet : {} | Message : {}", to, subject, message);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("[MAIL] Aucun serveur SMTP configuré (spring.mail.host vide). Email non envoyé à {}", to);
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(buildHtml(subject, message), true);
            mailSender.send(mimeMessage);
            log.info("Email envoyé à {} : {}", to, subject);
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'email à {} ({}) : {}", to, subject, e.getMessage());
        }
    }

    private String buildHtml(String title, String message) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto;border:1px solid #e2e8f0;border-radius:10px;overflow:hidden">
                  <div style="background:#4f46e5;color:#fff;padding:16px 24px;font-size:18px;font-weight:bold">AppRH</div>
                  <div style="padding:24px">
                    <h2 style="margin:0 0 12px;color:#0f172a;font-size:16px">%s</h2>
                    <p style="margin:0;color:#334155;line-height:1.6">%s</p>
                  </div>
                  <div style="background:#f8fafc;padding:12px 24px;color:#94a3b8;font-size:12px">
                    Message automatique — veuillez ne pas répondre à cet email.
                  </div>
                </div>
                """.formatted(escapeHtml(title), escapeHtml(message));
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
