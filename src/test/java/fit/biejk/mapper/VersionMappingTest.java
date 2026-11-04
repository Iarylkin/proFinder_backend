package fit.biejk.mapper;

import fit.biejk.dto.OrderUpdateRequest;
import fit.biejk.dto.ProfessionalUpdateRequest;
import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.entity.CustomerProfile;
import fit.biejk.entity.Order;
import fit.biejk.entity.OrderResponse;
import fit.biejk.entity.ProfessionalProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The version is shown to the client in every response and is taken from a partial update.
 */
class VersionMappingTest {

    private static final long VERSION = 7L;

    private final OrderMapper orderMapper = new OrderMapperImpl();
    private final OrderResponseMapper orderResponseMapper = new OrderResponseMapperImpl();
    private final CustomerMapper customerMapper = new CustomerMapperImpl();
    private final ProfessionalMapper professionalMapper = new ProfessionalMapperImpl();

    @Test
    void responsesShowTheVersion() {
        Order order = new Order();
        order.setVersion(VERSION);
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setVersion(VERSION);
        CustomerProfile customer = new CustomerProfile();
        customer.setVersion(VERSION);
        ProfessionalProfile professional = new ProfessionalProfile();
        professional.setVersion(VERSION);

        assertEquals(VERSION, orderMapper.toDto(order).getVersion());
        assertEquals(VERSION, orderResponseMapper.toDto(orderResponse).getVersion());
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
}
