package fit.biejk.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fit.biejk.common.entity.BaseEntity;
import fit.biejk.common.validation.Limits;
import fit.biejk.core.entity.Profile;
import fit.biejk.messaging.entity.Chat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Base entity representing a system user.
 * <p>
 * The data of a role lives in a separate profile ({@link fit.biejk.core.entity.CustomerProfile} or
 * {@link fit.biejk.core.entity.ProfessionalProfile}).
 * It stores common attributes such as contact details, role, and avatar reference.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    /**
     * User's email address. Must be unique.
     */
    @Column(name = "email", nullable = false, unique = true, length = Limits.EMAIL_MAX_LENGTH)
    private String email;

    /**
     * User's password.
     */
    @Column(name = "password", nullable = false)
    @JsonIgnore
    private String password;

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
     * List of chats initiated by the user.
     */
    @OneToMany(mappedBy = "user1", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Chat> initiatedChats;

    /**
     * List of chats received by the user.
     */
    @OneToMany(mappedBy = "user2", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Chat> receivedChats;

    /**
     * Profile of the user: a customer or a professional profile, by role. Removed together with the user.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @JsonIgnore
    private Profile profile;
}
