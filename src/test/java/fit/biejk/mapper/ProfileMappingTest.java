package fit.biejk.mapper;

import fit.biejk.dto.ProfessionalUpdateRequest;
import fit.biejk.dto.CustomerUpdateRequest;
import fit.biejk.entity.Category;
import fit.biejk.entity.CustomerProfile;
import fit.biejk.entity.ProfessionalProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProfileMappingTest {

    private final CustomerMapper customerMapper = new CustomerMapperImpl();
    private final ProfessionalMapper professionalMapper = new ProfessionalMapperImpl();

    @Test
    void customerChangesKeepUnsentFieldsNull() {
        CustomerUpdateRequest request = new CustomerUpdateRequest(null, null, Optional.of("222222222"), null, 0L);

        CustomerProfile changes = customerMapper.toEntity(request);

        assertEquals("222222222", changes.getPhoneNumber());
        assertNull(changes.getFirstName());
        assertNull(changes.getLastName());
        assertNull(changes.getLocation());
    }

    @Test
    void anEmptyTextIsCarriedToTheChangesSoTheServiceClearsTheField() {
        CustomerProfile changes = customerMapper.toEntity(new CustomerUpdateRequest(null, Optional.empty(), Optional.empty(), null, 0L));

        assertEquals("", changes.getLastName());
        assertEquals("", changes.getPhoneNumber());
        assertNull(changes.getFirstName());
    }

    @Test
    void professionalChangesCarryDescriptionAndCategories() {
        Category plumbing = new Category();
        plumbing.setId(UUID.randomUUID());
        ProfessionalUpdateRequest request = new ProfessionalUpdateRequest(
                null, null, null, null, Optional.of("I fix pipes"), List.of(plumbing), 0L);

        ProfessionalProfile changes = professionalMapper.toEntity(request);

        assertEquals("I fix pipes", changes.getDescription());
        assertEquals(List.of(plumbing), changes.getCategories());
        assertNull(changes.getFirstName());
        assertNull(changes.getPhoneNumber());
    }
}
