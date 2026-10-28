package fit.biejk.resource;

import fit.biejk.dto.CustomerDto;
import fit.biejk.dto.ProfessionalDto;
import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.entity.Category;
import fit.biejk.entity.Customer;
import fit.biejk.entity.Professional;
import fit.biejk.entity.UserRole;
import fit.biejk.mapper.CustomerMapperImpl;
import fit.biejk.mapper.ProfessionalMapperImpl;
import fit.biejk.service.AuthService;
import fit.biejk.service.CustomerService;
import fit.biejk.service.ProfessionalService;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileResourceTest {

    private static final long USER_ID = 7L;

    private AuthService authService;
    private CustomerService customerService;
    private ProfessionalService professionalService;
    private ProfileResource resource;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        customerService = mock(CustomerService.class);
        professionalService = mock(ProfessionalService.class);
        resource = new ProfileResource(
                authService,
                customerService,
                professionalService,
                new CustomerMapperImpl(),
                new ProfessionalMapperImpl());
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
    }

    private void loggedInAs(final UserRole role) {
        when(authService.getCurrentUserRole()).thenReturn(Optional.of(role));
    }

    @Test
    void aCustomerGetsACustomerProfile() {
        loggedInAs(UserRole.CUSTOMER);
        when(customerService.getById(USER_ID)).thenReturn(new Customer());

        Response response = resource.getProfile();

        assertInstanceOf(CustomerDto.class, response.getEntity());
    }

    @Test
    void aProfessionalGetsAProfessionalProfile() {
        loggedInAs(UserRole.PROFESSIONAL);
        when(professionalService.getById(USER_ID)).thenReturn(new Professional());

        Response response = resource.getProfile();

        assertInstanceOf(ProfessionalDto.class, response.getEntity());
    }

    @Test
    void anAccountWithoutARoleIsForbidden() {
        when(authService.getCurrentUserRole()).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> resource.getProfile());
        assertThrows(ForbiddenException.class, () -> resource.updateProfile(new ProfileUpdateRequest()));
    }

    @Test
    void aCustomerUpdateGoesToTheCustomerService() {
        loggedInAs(UserRole.CUSTOMER);
        when(customerService.updateProfile(any(), any())).thenReturn(new Customer());
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setPhoneNumber("222222222");

        Response response = resource.updateProfile(request);

        assertInstanceOf(CustomerDto.class, response.getEntity());
        verify(professionalService, never()).updateProfile(any(), any());
    }

    @Test
    void aCustomerCannotSendProfessionalOnlyFields() {
        loggedInAs(UserRole.CUSTOMER);
        ProfileUpdateRequest withDescription = new ProfileUpdateRequest();
        withDescription.setDescription("I fix pipes");
        ProfileUpdateRequest withCategories = new ProfileUpdateRequest();
        withCategories.setCategories(List.of(new Category()));

        assertThrows(BadRequestException.class, () -> resource.updateProfile(withDescription));
        assertThrows(BadRequestException.class, () -> resource.updateProfile(withCategories));
        verify(customerService, never()).updateProfile(any(), any());
    }

    @Test
    void aProfessionalUpdateGoesToTheProfessionalService() {
        loggedInAs(UserRole.PROFESSIONAL);
        when(professionalService.updateProfile(any(), any())).thenReturn(new Professional());
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setDescription("I fix pipes");

        Response response = resource.updateProfile(request);

        assertInstanceOf(ProfessionalDto.class, response.getEntity());
        verify(customerService, never()).updateProfile(any(), any());
    }
}
