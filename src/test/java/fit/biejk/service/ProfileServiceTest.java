package fit.biejk.service;

import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.entity.Customer;
import fit.biejk.entity.Professional;
import fit.biejk.entity.User;
import fit.biejk.entity.UserRole;
import fit.biejk.repository.CustomerRepository;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileServiceTest {

    private static final long CUSTOMER_ID = 1L;
    private static final long PROFESSIONAL_ID = 2L;
    private static final long OTHER_ID = 3L;

    private CustomerService customerService;
    private ProfessionalService professionalService;
    private ProfileService service;
    private Customer customer;
    private Professional professional;

    @BeforeEach
    void setUp() {
        UserService userService = mock(UserService.class);
        customerService = mock(CustomerService.class);
        professionalService = mock(ProfessionalService.class);
        service = new ProfileService(userService, customerService, professionalService);

        customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setRole(UserRole.CUSTOMER);
        professional = new Professional();
        professional.setId(PROFESSIONAL_ID);
        professional.setRole(UserRole.PROFESSIONAL);
        User other = new User();
        other.setId(OTHER_ID);

        when(userService.getById(CUSTOMER_ID)).thenReturn(customer);
        when(userService.getById(PROFESSIONAL_ID)).thenReturn(professional);
        when(userService.getById(OTHER_ID)).thenReturn(other);
        when(customerService.getById(CUSTOMER_ID)).thenReturn(customer);
        when(professionalService.getById(PROFESSIONAL_ID)).thenReturn(professional);
    }

    @Test
    void getReturnsTheProfileMatchingTheRole() {
        assertSame(customer, service.get(CUSTOMER_ID));
        assertSame(professional, service.get(PROFESSIONAL_ID));
    }

    @Test
    void updateIsRoutedByRole() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        when(customerService.updateProfile(CUSTOMER_ID, request)).thenReturn(customer);
        when(professionalService.updateProfile(PROFESSIONAL_ID, request)).thenReturn(professional);

        assertSame(customer, service.update(CUSTOMER_ID, request));
        assertSame(professional, service.update(PROFESSIONAL_ID, request));
    }

    @Test
    void anAccountWithoutARoleHasNoProfile() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();

        assertThrows(ForbiddenException.class, () -> service.get(OTHER_ID));
        assertThrows(ForbiddenException.class, () -> service.update(OTHER_ID, request));
    }

    @Test
    void customerServiceRejectsProfessionalOnlyFields() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setDescription("I fix pipes");
        CustomerService real = new CustomerService(
                mock(CustomerRepository.class), mock(UserService.class));

        assertThrows(BadRequestException.class, () -> real.updateProfile(CUSTOMER_ID, request));
    }
}
