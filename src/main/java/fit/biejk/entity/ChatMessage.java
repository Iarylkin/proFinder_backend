package fit.biejk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a single message sent within a chat.
 * <p>
 * Each message is linked to a {@link Chat} and has a sender ({@link User}),
 * text content, and creation timestamp.
 * </p>
 */
@Entity
@Table(name = "chat-message")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage extends BaseEntity {

    /**
     * User who sent the message.
     * <p>
     * Represented by a many-to-one relation to {@link User}.
     * </p>
     */
    @ManyToOne
    private User sender;

    /**
     * Content of the message.
     */
    @Column(name = "content")
    private String content;

    /**
     * Timestamp when the message was created.
     * <p>
     * Initialized with the current time by default.
     * </p>
     */
    @Column(name = "create_at")
    private LocalDateTime createAt = LocalDateTime.now();

    /**
     * Chat to which the message belongs.
     * <p>
     * This is a required association.
     * </p>
     */
    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;
}
