package fit.biejk.core.service;

import fit.biejk.auth.service.UserService;
import fit.biejk.common.exception.StaleVersionException;
import fit.biejk.core.entity.CustomerProfile;
import fit.biejk.core.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    private static final long VERSION = 2L;

    private ProfileService profileService;
    private CustomerService service;
    private CustomerProfile customer;

    @BeforeEach
    void setUp() {
        CustomerRepository repository = mock(CustomerRepository.class);
        profileService = mock(ProfileService.class);
        service = new CustomerService(repository, mock(UserService.class), profileService);

        customer = new CustomerProfile();
        customer.setId(CUSTOMER_ID);
        customer.setVersion(VERSION);
        when(repository.findById(CUSTOMER_ID)).thenReturn(customer);
    }

    @Test
    void updateProfileAppliesTheChangesOfTheCurrentVersion() {
        CustomerProfile changes = new CustomerProfile();
        changes.setVersion(VERSION);

        assertSame(customer, service.updateProfile(CUSTOMER_ID, changes));

        verify(profileService).updatePersonalData(customer, changes);
    }

    @Test
    void updateProfileWithAStaleVersionIsRejectedAndChangesNothing() {
        CustomerProfile changes = new CustomerProfile();
        changes.setVersion(VERSION - 1);

        assertThrows(StaleVersionException.class, () -> service.updateProfile(CUSTOMER_ID, changes));

        verify(profileService, never()).updatePersonalData(any(), any());
    }
}
