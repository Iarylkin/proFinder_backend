package fit.biejk.mapper;

import fit.biejk.dto.ProfessionalPublicDto;
import fit.biejk.entity.Professional;
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
        Professional professional = new Professional();
        professional.setEmail(EMAIL);
        professional.setPhoneNumber(PHONE);

        ProfessionalPublicDto dto = mapper.toPublicDto(professional);

        assertEquals(PHONE, dto.getPhoneNumber());
        assertFalse(dto.toString().contains(EMAIL));
        assertFalse(Arrays.stream(ProfessionalPublicDto.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("email")));
    }
}
