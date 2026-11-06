package fit.biejk.common.dto;

import fit.biejk.auth.dto.AuthRequest;
import fit.biejk.auth.dto.AuthResponse;
import fit.biejk.auth.dto.ChangeEmailRequest;
import fit.biejk.auth.dto.ChangePasswordRequest;
import fit.biejk.auth.dto.ResetPasswordRequest;
import fit.biejk.auth.entity.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DtoToStringTest {

    private static final String SECRET = "s3cr3t-value";

    @Test
    void authRequestHidesPassword() {
        AuthRequest dto = new AuthRequest("a@b.c", SECRET, UserRole.CUSTOMER);
        assertFalse(dto.toString().contains(SECRET));
    }

    @Test
    void changePasswordRequestHidesPasswords() {
        ChangePasswordRequest dto = new ChangePasswordRequest(SECRET, SECRET, SECRET);
        assertFalse(dto.toString().contains(SECRET));
    }

    @Test
    void resetPasswordRequestHidesSecrets() {
        ResetPasswordRequest dto = new ResetPasswordRequest("a@b.c", SECRET, SECRET, SECRET);
        assertFalse(dto.toString().contains(SECRET));
    }

    @Test
    void changeEmailRequestHidesPassword() {
        ChangeEmailRequest dto = new ChangeEmailRequest("a@b.c", SECRET);
        assertFalse(dto.toString().contains(SECRET));
    }

    @Test
    void authResponseHidesToken() {
        AuthResponse dto = new AuthResponse(SECRET);
        assertFalse(dto.toString().contains(SECRET));
    }
}
