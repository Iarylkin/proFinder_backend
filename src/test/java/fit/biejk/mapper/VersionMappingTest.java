package fit.biejk.mapper;

import fit.biejk.dto.CustomerDto;
import fit.biejk.dto.OrderDto;
import fit.biejk.dto.OrderResponseDto;
import fit.biejk.dto.OrderUpdateRequest;
import fit.biejk.dto.ProfessionalDto;
import fit.biejk.dto.ProfessionalUpdateRequest;
import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.entity.CustomerProfile;
import fit.biejk.entity.Order;
import fit.biejk.entity.OrderResponse;
import fit.biejk.entity.ProfessionalProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The version is shown to the client in every response, is taken from a partial update, and is never taken from
 * a request that creates something.
 */
class VersionMappingTest {

    private static final long VERSION = 7L;

    private final OrderMapper orderMapper = new OrderMapperImpl();
    private final OrderResponseMapper responseMapper = new OrderResponseMapperImpl();
    private final CustomerMapper customerMapper = new CustomerMapperImpl();
    private final ProfessionalMapper professionalMapper = new ProfessionalMapperImpl();

    @Test
    void responsesShowTheVersion() {
        Order order = new Order();
        order.setVersion(VERSION);
        OrderResponse response = new OrderResponse();
        response.setVersion(VERSION);
        CustomerProfile customer = new CustomerProfile();
        customer.setVersion(VERSION);
        ProfessionalProfile professional = new ProfessionalProfile();
        professional.setVersion(VERSION);

        assertEquals(VERSION, orderMapper.toDto(order).getVersion());
        assertEquals(VERSION, responseMapper.toDto(response).getVersion());
        assertEquals(VERSION, customerMapper.toDto(customer).getVersion());
        assertEquals(VERSION, professionalMapper.toDto(professional).getVersion());
    }

    @Test
    void partialUpdatesCarryTheVersionToTheEntity() {
        OrderUpdateRequest order = new OrderUpdateRequest();
        order.setVersion(VERSION);
        ProfileUpdateRequest customer = new ProfileUpdateRequest();
        customer.setVersion(VERSION);
        ProfessionalUpdateRequest professional = new ProfessionalUpdateRequest();
        professional.setVersion(VERSION);

        assertEquals(VERSION, orderMapper.toEntity(order).getVersion());
        assertEquals(VERSION, customerMapper.toEntity(customer).getVersion());
        assertEquals(VERSION, professionalMapper.toEntity(professional).getVersion());
    }

    @Test
    void aVersionSentWhenCreatingIsIgnored() {
        OrderDto order = new OrderDto();
        order.setVersion(VERSION);
        OrderResponseDto response = new OrderResponseDto();
        response.setVersion(VERSION);
        CustomerDto customer = new CustomerDto();
        customer.setVersion(VERSION);
        ProfessionalDto professional = new ProfessionalDto();
        professional.setVersion(VERSION);

        assertNull(orderMapper.toEntity(order).getVersion());
        assertNull(responseMapper.toEntity(response).getVersion());
        assertNull(customerMapper.toEntity(customer).getVersion());
        assertNull(professionalMapper.toEntity(professional).getVersion());
    }
}
