package fit.biejk.mapper;

import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.entity.Category;
import fit.biejk.entity.Customer;
import fit.biejk.entity.Professional;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProfileMappingTest {

    private final CustomerMapper customerMapper = new CustomerMapperImpl();
    private final ProfessionalMapper professionalMapper = new ProfessionalMapperImpl();

    @Test
    void customerChangesKeepUnsentFieldsNull() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setPhoneNumber("222222222");

        Customer changes = customerMapper.toEntity(request);

        assertEquals("222222222", changes.getPhoneNumber());
        assertNull(changes.getFirstName());
        assertNull(changes.getLastName());
        assertNull(changes.getLocation());
    }

    @Test
    void professionalChangesCarryDescriptionAndCategories() {
        Category plumbing = new Category();
        plumbing.setId(1L);
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setDescription("I fix pipes");
        request.setCategories(List.of(plumbing));

        Professional changes = professionalMapper.toEntity(request);

        assertEquals("I fix pipes", changes.getDescription());
        assertEquals(List.of(plumbing), changes.getCategories());
        assertNull(changes.getFirstName());
        assertNull(changes.getPhoneNumber());
    }
}
