package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.auth.service.UserService;
import io.github.strogolsky.profinder.core.entity.CustomerProfile;
import io.github.strogolsky.profinder.core.repository.CustomerRepository;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reading and deleting a customer. The update of the profile is in {@link CustomerServiceTest}.
 * Deleting a customer is deleting the account: the profile follows through the event {@code UserDeleted}.
 */
class CustomerServiceCrudTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    private UserService userService;
    private CustomerService service;
    private CustomerProfile customer;

    @BeforeEach
    void setUp() {
        CustomerRepository repository = mock(CustomerRepository.class);
        userService = mock(UserService.class);
        service = new CustomerService(repository, userService, mock(ProfileService.class));

        customer = new CustomerProfile();
        customer.setId(CUSTOMER_ID);
        when(repository.findById(CUSTOMER_ID)).thenReturn(customer);
    }

    @Test
    void customerIsFoundById() {
        assertSame(customer, service.getById(CUSTOMER_ID));
    }

    @Test
    void missingCustomerIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getById(UUID.randomUUID()));
    }

    @Test
    void deletingACustomerDeletesTheAccount() {
        service.delete(CUSTOMER_ID);

        verify(userService).delete(CUSTOMER_ID);
    }

    @Test
    void deletingAMissingAccountIsNotFound() {
        doThrow(new NotFoundException("no user")).when(userService).delete(CUSTOMER_ID);

        assertThrows(NotFoundException.class, () -> service.delete(CUSTOMER_ID));
    }
}
