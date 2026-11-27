package io.github.strogolsky.profinder.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.strogolsky.profinder.common.entity.BaseEntity;
import io.github.strogolsky.profinder.common.validation.Limits;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Base entity representing a system user.
 * <p>
 * The data of a role lives in a separate profile ({@link io.github.strogolsky.profinder.core.entity.CustomerProfile} or
 * {@link io.github.strogolsky.profinder.core.entity.ProfessionalProfile}).
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
    private final LocalDateTime createAt = LocalDateTime.now(ZoneOffset.UTC);

}
