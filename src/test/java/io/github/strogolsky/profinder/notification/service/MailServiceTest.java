package io.github.strogolsky.profinder.notification.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * A mail is a plain text message to one address.
 */
class MailServiceTest {

    @Test
    void mailIsSentAsPlainTextToTheAddress() {
        Mailer mailer = mock(Mailer.class);
        MailService service = new MailService(mailer);

        service.send("user@example.com", "Reset password", "Your code is 123456");

        ArgumentCaptor<Mail> sent = ArgumentCaptor.forClass(Mail.class);
        verify(mailer).send(sent.capture());
        assertEquals(List.of("user@example.com"), sent.getValue().getTo());
        assertEquals("Reset password", sent.getValue().getSubject());
        assertEquals("Your code is 123456", sent.getValue().getText());
    }
}
