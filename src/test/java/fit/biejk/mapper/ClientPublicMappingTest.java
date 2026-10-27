package fit.biejk.mapper;

import fit.biejk.dto.ClientPublicDto;
import fit.biejk.entity.Client;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ClientPublicMappingTest {

    private static final String EMAIL = "private@example.com";
    private static final String PHONE = "+10000000000";

    private final ClientMapper mapper = new ClientMapperImpl();

    @Test
    void publicDtoHasNoContactData() {
        Client client = new Client();
        client.setFirstName("Ann");
        client.setEmail(EMAIL);
        client.setPhoneNumber(PHONE);

        ClientPublicDto dto = mapper.toPublicDto(client);

        assertEquals("Ann", dto.getFirstName());
        assertFalse(dto.toString().contains(EMAIL));
        assertFalse(dto.toString().contains(PHONE));
        assertFalse(Arrays.stream(ClientPublicDto.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("email")
                        || field.getName().equals("phoneNumber")));
    }
}
