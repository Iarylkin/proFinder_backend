package fit.biejk.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for sending emails using Quarkus Mailer.
 * <p>
 * Wraps the underlying {@link Mailer} and provides a simplified interface for sending plain text emails.
 * </p>
 */
@ApplicationScoped
@Slf4j
@RequiredArgsConstructor
public class MailService {

    /**
     * Injected Quarkus Mailer used to send emails.
     */
    private final Mailer mailer;

    /**
     * Sends a plain text email to the specified recipient.
     *
     * @param to      recipient's email address
     * @param subject email subject
     * @param body    plain text email content
     */
    public void send(final String to, final String subject, final String body) {
        log.info("Sending mail to " + to);
        mailer.send(Mail.withText(to, subject, body));
    }
}
