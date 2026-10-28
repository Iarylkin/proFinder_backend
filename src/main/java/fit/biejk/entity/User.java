package fit.biejk.entity;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Base entity representing a system user.
 * <p>
 * This class is extended by more specific user types, such as {@link Customer} and {@link Professional}.
 * It stores common attributes such as contact details, role, and avatar reference.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User extends BaseEntity {

    /**
     * Maximum length for firstName and lastName fields.
     */
    public static final int NAME_MAX_LENGTH = 50;

    /**
     * Maximum length for phoneNumber field.
     */
    public static final int PHONE_MAX_LENGTH = 20;

    /**
     * User's email address. Must be unique.
     */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * User's password.
     */
    @Column(name = "password", nullable = false)
    @JsonbTransient
    private String password;

    /**
     * User's first name.
     */
    @Column(name = "first_name", length = NAME_MAX_LENGTH)
    private String firstName;

    /**
     * User's last name.
     */
    @Column(name = "last_name", length = NAME_MAX_LENGTH)
    private String lastName;

    /**
     * User's phone number.
     */
    @Column(name = "phone_number", length = PHONE_MAX_LENGTH)
    private String phoneNumber;

    /**
     * User's geographical location.
     */
    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    /**
     * Role assigned to the user (e.g., CUSTOMER, PROFESSIONAL).
     */
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private UserRole role;

    /**
     * Timestamp when the user account was created.
     */
    @Column(name = "createAt", nullable = false, updatable = false)
    private final LocalDateTime createAt = LocalDateTime.now();

    /**
     * Key of the user's avatar stored in object storage (e.g. MinIO/S3).
     */
    @Column(name = "avatar_key")
    private String avatarKey;

    /**
     * List of chats initiated by the user.
     */
    @OneToMany(mappedBy = "user1", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonbTransient
    private List<Chat> initiatedChats;

    /**
     * List of chats received by the user.
     */
    @OneToMany(mappedBy = "user2", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonbTransient
    private List<Chat> receivedChats;
}
