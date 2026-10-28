package fit.biejk.entity;

import jakarta.json.bind.annotation.JsonbTransient;
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
public class Chat {

    /**
     * Unique identifier for the chat.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * First participant in the chat.
     * <p>
     * {@link JsonbTransient} is used to avoid cyclic serialization.
     * </p>
     */
    @ManyToOne
    @JoinColumn(name = "user1_id", nullable = false)
    @JsonbTransient
    private User user1;

    /**
     * Second participant in the chat.
     * <p>
     * {@link JsonbTransient} is used to avoid cyclic serialization.
     * </p>
     */
    @ManyToOne
    @JoinColumn(name = "user2_id", nullable = false)
    @JsonbTransient
    private User user2;

    /**
     * List of messages exchanged in the chat.
     * <p>
     * Messages are cascaded and orphan removal is enabled.
     * {@link JsonbTransient} prevents serialization of the entire message list by default.
     * </p>
     */
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonbTransient
    private List<ChatMessage> messages;

    /**
     * Checks whether the given user is one of the two participants of this chat.
     *
     * @param userId ID of the user to check
     * @return true if the user is a participant, false otherwise
     */
    public boolean hasParticipant(final UUID userId) {
        return userId != null
                && (user1.getId().equals(userId) || user2.getId().equals(userId));
    }
}
