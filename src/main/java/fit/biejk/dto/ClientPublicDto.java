package fit.biejk.dto;

import fit.biejk.entity.Location;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Public view of a client, returned to any authenticated user who knows the client ID.
 * <p>
 * Unlike {@link ClientDto} it contains no contact data (email, phone number).
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientPublicDto {

    /**
     * Unique identifier of the client.
     */
    private Long id;

    /**
     * First name of the client.
     */
    private String firstName;

    /**
     * Last name of the client.
     */
    private String lastName;

    /**
     * Geographical location associated with the client.
     */
    private Location location;

    /**
     * Timestamp of when the client account was created.
     */
    private LocalDateTime createAt;
}
