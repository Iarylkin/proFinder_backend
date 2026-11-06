package fit.biejk.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class of every entity: a version for optimistic locking.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class VersionedEntity {

    /**
     * Version of the row; a concurrent update of an outdated row fails.
     * Not exposed in JSON.
     */
    @JsonIgnore
    @Version
    private Long version;
}
