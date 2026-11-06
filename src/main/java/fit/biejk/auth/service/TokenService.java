package fit.biejk.auth.service;

import fit.biejk.auth.entity.User;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.List;

/**
 * Service for generating JWT tokens.
 */
@ApplicationScoped
public class TokenService {

    /**
     * Default token expiration time (24h).
     */
    private static final Duration DEFAULT_EXPIRATION = Duration.ofHours(24);

    /**
     * Builds a JWT for the given user.
     *
     * @param user       user entity
     * @param expiration token lifetime
     * @return signed JWT string
     */
    private String generateJWT(final User user, final Duration expiration) {
        return Jwt.issuer("quarkus-app")
                .subject(user.getId().toString())
                .claim("groups", List.of(user.getRole().name()))
                .expiresIn(expiration)
                .sign();
    }

    /**
     * Generates a JWT with default expiration.
     *
     * @param user user entity
     * @return signed JWT string
     */
    public String generateToken(final User user) {
        return generateJWT(user, DEFAULT_EXPIRATION);
    }
}
