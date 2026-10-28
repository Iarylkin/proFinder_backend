package fit.biejk.mapper;

import fit.biejk.dto.ProfessionalPublicDto;
import fit.biejk.entity.ProfessionalProfile;
import fit.biejk.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProfessionalPublicMappingTest {

    private static final String EMAIL = "private@example.com";
    private static final String PHONE = "+10000000000";

    private final ProfessionalMapper mapper = new ProfessionalMapperImpl();

    @Test
    void publicDtoKeepsPhoneButHasNoEmail() {
        User user = new User();
        user.setEmail(EMAIL);
        user.setPhoneNumber(PHONE);
        ProfessionalProfile professional = new ProfessionalProfile();
        professional.setUser(user);

        ProfessionalPublicDto dto = mapper.toPublicDto(professional);

        assertEquals(PHONE, dto.getPhoneNumber());
        assertFalse(dto.toString().contains(EMAIL));
        assertFalse(Arrays.stream(ProfessionalPublicDto.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("email")));
    }
}
