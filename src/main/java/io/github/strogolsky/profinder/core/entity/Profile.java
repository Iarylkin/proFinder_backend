package io.github.strogolsky.profinder.core.entity;

import io.github.strogolsky.profinder.auth.entity.User;
import io.github.strogolsky.profinder.common.entity.VersionedEntity;
import io.github.strogolsky.profinder.common.validation.Limits;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

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
     * Identifier of the profile, taken from the user it belongs to.
     */
    @Id
    private UUID id;

    /**
     * Account the profile belongs to.
     */
    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

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
