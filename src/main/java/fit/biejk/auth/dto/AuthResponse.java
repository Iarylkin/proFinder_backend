package fit.biejk.auth.dto;

/**
 * Data Transfer Object for authentication responses. Contains the JWT token returned upon successful
 * authentication.
 *
 * @param token JWT authentication token.
 */
public record AuthResponse(
        String token) {

    @Override
    public String toString() {
        return "AuthResponse[token=***]";
    }
}
