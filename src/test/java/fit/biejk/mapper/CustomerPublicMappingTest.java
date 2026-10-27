package fit.biejk.mapper;

import fit.biejk.dto.CustomerPublicDto;
import fit.biejk.entity.Customer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CustomerPublicMappingTest {

    private static final String EMAIL = "private@example.com";
    private static final String PHONE = "+10000000000";

    private final CustomerMapper mapper = new CustomerMapperImpl();

    @Test
    void publicDtoHasNoContactData() {
        Customer customer = new Customer();
        customer.setFirstName("Ann");
        customer.setEmail(EMAIL);
        customer.setPhoneNumber(PHONE);

        CustomerPublicDto dto = mapper.toPublicDto(customer);

        assertEquals("Ann", dto.getFirstName());
        assertFalse(dto.toString().contains(EMAIL));
        assertFalse(dto.toString().contains(PHONE));
        assertFalse(Arrays.stream(CustomerPublicDto.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("email")
                        || field.getName().equals("phoneNumber")));
    }
}
