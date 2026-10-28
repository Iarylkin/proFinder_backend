package fit.biejk.mapper;

import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.entity.Category;
import fit.biejk.entity.CustomerProfile;
import fit.biejk.entity.ProfessionalProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProfileMappingTest {

    private final CustomerMapper customerMapper = new CustomerMapperImpl();
    private final ProfessionalMapper professionalMapper = new ProfessionalMapperImpl();

    @Test
    void customerChangesKeepUnsentFieldsNull() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setPhoneNumber("222222222");

        CustomerProfile changes = customerMapper.toEntity(request);

        assertEquals("222222222", changes.getUser().getPhoneNumber());
        assertNull(changes.getUser().getFirstName());
        assertNull(changes.getUser().getLastName());
        assertNull(changes.getUser().getLocation());
    }

    @Test
    void professionalChangesCarryDescriptionAndCategories() {
        Category plumbing = new Category();
        plumbing.setId(UUID.randomUUID());
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setDescription("I fix pipes");
        request.setCategories(List.of(plumbing));

        ProfessionalProfile changes = professionalMapper.toEntity(request);

        assertEquals("I fix pipes", changes.getDescription());
        assertEquals(List.of(plumbing), changes.getCategories());
        assertNull(changes.getUser().getFirstName());
        assertNull(changes.getUser().getPhoneNumber());
    }
}
