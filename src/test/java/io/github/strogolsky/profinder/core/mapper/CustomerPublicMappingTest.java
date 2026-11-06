package io.github.strogolsky.profinder.core.mapper;

import io.github.strogolsky.profinder.auth.entity.User;
import io.github.strogolsky.profinder.core.dto.CustomerPublicResponse;
import io.github.strogolsky.profinder.core.entity.CustomerProfile;
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
        User user = new User();
        user.setEmail(EMAIL);
        CustomerProfile customer = new CustomerProfile();
        customer.setUser(user);
        customer.setFirstName("Ann");
        customer.setPhoneNumber(PHONE);

        CustomerPublicResponse dto = mapper.toPublicDto(customer);

        assertEquals("Ann", dto.firstName());
        assertFalse(dto.toString().contains(EMAIL));
        assertFalse(dto.toString().contains(PHONE));
        assertFalse(Arrays.stream(CustomerPublicResponse.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("email")
                        || field.getName().equals("phoneNumber")));
    }
}
