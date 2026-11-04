package fit.biejk.mapper;

import fit.biejk.dto.OrderUpdateRequest;
import fit.biejk.dto.ProfessionalUpdateRequest;
import fit.biejk.dto.CustomerUpdateRequest;
import fit.biejk.entity.CustomerProfile;
import fit.biejk.entity.Order;
import fit.biejk.entity.Bid;
import fit.biejk.entity.ProfessionalProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The version is shown to the client in every response and is taken from a partial update.
 */
class VersionMappingTest {

    private static final long VERSION = 7L;

    private final OrderMapper orderMapper = new OrderMapperImpl();
    private final BidMapper bidMapper = new BidMapperImpl();
    private final CustomerMapper customerMapper = new CustomerMapperImpl();
    private final ProfessionalMapper professionalMapper = new ProfessionalMapperImpl();

    @Test
    void responsesShowTheVersion() {
        Order order = new Order();
        order.setVersion(VERSION);
        Bid bid = new Bid();
        bid.setVersion(VERSION);
        CustomerProfile customer = new CustomerProfile();
        customer.setVersion(VERSION);
        ProfessionalProfile professional = new ProfessionalProfile();
        professional.setVersion(VERSION);

        assertEquals(VERSION, orderMapper.toDto(order).version());
        assertEquals(VERSION, bidMapper.toDto(bid).version());
        assertEquals(VERSION, customerMapper.toDto(customer).version());
        assertEquals(VERSION, professionalMapper.toDto(professional).version());
    }

    @Test
    void partialUpdatesCarryTheVersionToTheEntity() {
        OrderUpdateRequest order = new OrderUpdateRequest(null, null, null, null, null, null, null, VERSION);
        CustomerUpdateRequest customer = new CustomerUpdateRequest(null, null, null, null, VERSION);
        ProfessionalUpdateRequest professional = new ProfessionalUpdateRequest(
                null, null, null, null, null, null, VERSION);

        assertEquals(VERSION, orderMapper.toEntity(order).getVersion());
        assertEquals(VERSION, customerMapper.toEntity(customer).getVersion());
        assertEquals(VERSION, professionalMapper.toEntity(professional).getVersion());
    }
}
