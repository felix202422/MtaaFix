package com.mtaafix.common.email;

import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * SMTP email channel. Disabled by default ({@code notifications.email.enabled=false}),
 * so deployments without a mail provider simply record deliveries as SKIPPED
 * instead of failing report workflows.
 */
@Component
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final boolean enabled;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;
    private final boolean starttls;

    public SmtpEmailSender(
            @Value("${notifications.email.enabled:false}") boolean enabled,
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.port:587}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${notifications.email.from:no-reply@mtaafix.local}") String from,
            @Value("${notifications.email.starttls:true}") boolean starttls) {
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.from = from;
        this.starttls = starttls;
    }

    @Override
    public boolean isConfigured() {
        return enabled && host != null && !host.isBlank();
    }

    @Override
    public void send(String to, String subject, String body) throws EmailSendException {
        if (!isConfigured()) {
            throw new EmailSendException("Email channel is not configured.");
        }
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(host);
            sender.setPort(port);
            if (username != null && !username.isBlank()) {
                sender.setUsername(username);
                sender.setPassword(password);
            }
            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", String.valueOf(username != null && !username.isBlank()));
            props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");
            props.put("mail.smtp.writetimeout", "5000");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            log.debug("Email handed to SMTP server for delivery");
        } catch (Exception ex) {
            throw new EmailSendException("SMTP delivery failed: " + ex.getMessage(), ex);
        }
    }
}
