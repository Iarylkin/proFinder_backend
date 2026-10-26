package fit.biejk.mapper;

import fit.biejk.dto.SpecialistPublicDto;
import fit.biejk.entity.Specialist;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SpecialistPublicMappingTest {

    private static final String EMAIL = "private@example.com";
    private static final String PHONE = "+10000000000";

    private final SpecialistMapper mapper = new SpecialistMapperImpl();

    @Test
    void publicDtoKeepsPhoneButHasNoEmail() {
        Specialist specialist = new Specialist();
        specialist.setEmail(EMAIL);
        specialist.setPhoneNumber(PHONE);

        SpecialistPublicDto dto = mapper.toPublicDto(specialist);

        assertEquals(PHONE, dto.getPhoneNumber());
        assertFalse(dto.toString().contains(EMAIL));
        assertFalse(Arrays.stream(SpecialistPublicDto.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("email")));
    }
}
