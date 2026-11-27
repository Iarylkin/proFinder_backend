package io.github.strogolsky.profinder.messaging.entity;

import io.github.strogolsky.profinder.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Entity representing a single message sent within a chat.
 * <p>
 * Each message is linked to a {@link Chat} and has a sender (the ID of a user),
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
     * User who sent the message: the ID of a user of the auth module (a plain reference, not a relation).
     */
    @Column(name = "sender_id")
    private UUID senderId;

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
    private LocalDateTime createAt = LocalDateTime.now(ZoneOffset.UTC);

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
