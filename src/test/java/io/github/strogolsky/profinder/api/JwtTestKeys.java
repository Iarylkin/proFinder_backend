package io.github.strogolsky.profinder.api;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

/**
 * Makes a throw-away RSA key pair for the tests, so no key is kept in the repository and the tests do not
 * need the {@code keys/} folder of a developer: the private key signs the tokens, the public one verifies
 * them. Every request passes the JWT mechanism, even one to a public endpoint, so the key must exist.
 */
public class JwtTestKeys implements QuarkusTestResourceLifecycleManager {

    private static final int KEY_SIZE = 2048;
    private static final int LINE_LENGTH = 64;

    private Path directory;

    @Override
    public Map<String, String> start() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE);
            KeyPair pair = generator.generateKeyPair();

            directory = Files.createTempDirectory("profinder-jwt");
            Path privateKey = write("privateKey.pem", "PRIVATE KEY", pair.getPrivate().getEncoded());
            Path publicKey = write("publicKey.pem", "PUBLIC KEY", pair.getPublic().getEncoded());
            return Map.of(
                    "smallrye.jwt.sign.key.location", privateKey.toString(),
                    "mp.jwt.verify.publickey.location", publicKey.toString(),
                    "quarkus.jwt.auth.public-key-location", publicKey.toString());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException("cannot make the test keys", e);
        }
    }

    private Path write(final String name, final String type, final byte[] encoded) throws IOException {
        String body = Base64.getMimeEncoder(LINE_LENGTH, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(encoded);
        Path file = directory.resolve(name);
        Files.writeString(file, "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n");
        return file;
    }

    @Override
    public void stop() {
        if (directory == null) {
            return;
        }
        try (var files = Files.list(directory)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                Files.deleteIfExists(file);
            }
            Files.deleteIfExists(directory);
        } catch (IOException e) {
            // a temp directory that stays is harmless
        }
    }
}
