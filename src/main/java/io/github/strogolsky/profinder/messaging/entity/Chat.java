package io.github.strogolsky.profinder.messaging.entity;

import io.github.strogolsky.profinder.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Entity representing a chat between two users.
 * <p>
 * Each chat is uniquely identified by the combination of {@code user1} and {@code user2}.
 * Messages between the users are associated with this chat.
 * </p>
 */
@Entity
@Table(name = "chat", uniqueConstraints = @UniqueConstraint(columnNames = {"user1_id", "user2_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Chat extends BaseEntity {

    /**
     * First participant in the chat: the ID of a user of the auth module (a plain reference, not a relation).
     */
    @Column(name = "user1_id", nullable = false)
    private UUID user1Id;

    /**
     * Second participant in the chat: the ID of a user of the auth module (a plain reference, not a relation).
     */
    @Column(name = "user2_id", nullable = false)
    private UUID user2Id;

    /**
     * List of messages exchanged in the chat.
     * <p>
     * Messages are cascaded and orphan removal is enabled.
     * {@link JsonIgnore} prevents serialization of the entire message list by default.
     * </p>
     */
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ChatMessage> messages;

    /**
     * Checks whether the given user is one of the two participants of this chat.
     *
     * @param userId ID of the user to check
     * @return true if the user is a participant, false otherwise
     */
    public boolean hasParticipant(final UUID userId) {
        return userId != null
                && (user1Id.equals(userId) || user2Id.equals(userId));
    }
}
