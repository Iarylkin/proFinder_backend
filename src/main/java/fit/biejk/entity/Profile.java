package fit.biejk.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
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
}
