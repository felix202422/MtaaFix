package com.mtaafix.common.email;

/**
 * Outbound email channel shared by notification delivery (FR-6.3) and the
 * password-reset flow (FR-1.7). Implementations must never log message
 * bodies or credentials.
 */
public interface EmailSender {

    /** True when the deployment is configured to actually send email. */
    boolean isConfigured();

    /**
     * Sends a plain-text email.
     *
     * @throws EmailSendException when the channel is configured but delivery
     *                            fails; callers record the failure as state,
     *                            never as a broken transaction
     */
    void send(String to, String subject, String body) throws EmailSendException;
}
