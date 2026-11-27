package io.github.strogolsky.profinder.core.entity;

import io.github.strogolsky.profinder.common.entity.VersionedEntity;
import io.github.strogolsky.profinder.common.validation.Limits;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.ColumnDefault;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class of the profiles that belong to a {@link User}.
 * <p>
 * A profile has no identifier of its own: its primary key is the {@code user_id}, the same value as the id of its
 * user. The class is abstract and has no table; each profile is stored in its own table
 * ({@code TABLE_PER_CLASS}), and a lookup by this class returns the profile of the right type.
 * </p>
 */
@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Profile extends VersionedEntity {

    /**
     * Identifier of the profile: the ID of the user it belongs to. The user lives in the auth
     * module, so this is a plain reference, not a relation.
     */
    @Id
    @Column(name = "user_id")
    private UUID id;

    /**
     * When the profile was created, which is when the person registered. Kept here because the account
     * lives in the auth module, whose data this module cannot join.
     */
    @Column(name = "create_at", nullable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createAt = LocalDateTime.now();

    /**
     * First name of the person.
     */
    @Column(name = "first_name", length = Limits.NAME_MAX_LENGTH)
    private String firstName;

    /**
     * Last name of the person.
     */
    @Column(name = "last_name", length = Limits.NAME_MAX_LENGTH)
    private String lastName;

    /**
     * Phone number of the person.
     */
    @Column(name = "phone_number", length = Limits.PHONE_MAX_LENGTH)
    private String phoneNumber;

    /**
     * Location of the person.
     */
    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    /**
     * Key of the avatar image in the file storage, if the person has one.
     */
    @Column(name = "avatar_key")
    private String avatarKey;
}
