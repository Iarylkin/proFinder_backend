package fit.biejk.service;

import fit.biejk.dto.ConfirmProposal;
import fit.biejk.entity.Customer;
import fit.biejk.entity.Order;
import fit.biejk.entity.OrderProposal;
import fit.biejk.entity.OrderStatus;
import fit.biejk.entity.ProposalStatus;
import fit.biejk.exception.StatusConflictException;
import fit.biejk.repository.OrderProposalRepository;
import fit.biejk.repository.OrderRepository;
import fit.biejk.search.OrderSearchMapper;
import fit.biejk.search.OrderSearchService;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderProposalServiceTest {

    private static final long ORDER_ID = 10L;
    private static final long PROPOSAL_ID = 20L;
    private static final long CUSTOMER_ID = 1L;
    private static final int FINAL_PRICE = 300;

    private OrderProposalRepository proposalRepository;
    private AuthService authService;
    private OrderProposalService service;
    private Order order;
    private OrderProposal proposal;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(OrderProposalRepository.class);
        authService = mock(AuthService.class);
        service = new OrderProposalService(
                mock(OrderRepository.class),
                authService,
                mock(OrderSearchService.class),
                mock(OrderSearchMapper.class),
                proposalRepository);

        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);

        order = new Order();
        order.setId(ORDER_ID);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CUSTOMER_PENDING);

        proposal = new OrderProposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setOrder(order);
        proposal.setStatus(ProposalStatus.CREATED);

        when(proposalRepository.findByIdOptional(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(authService.isCurrentUser(CUSTOMER_ID)).thenReturn(true);
    }

    private ConfirmProposal request(final ProposalStatus status) {
        return new ConfirmProposal(status, LocalDateTime.now().plusDays(1), FINAL_PRICE);
    }

    @Test
    void approvingAResponseCompletesTheOrderAndRejectsTheOthers() {
        Order result = service.confirmAndAssign(PROPOSAL_ID, request(ProposalStatus.APPROVED));

        assertSame(order, result);
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(FINAL_PRICE, order.getPrice());
        assertEquals(ProposalStatus.APPROVED, proposal.getStatus());
        verify(proposalRepository).rejectOthersForOrder(ORDER_ID, PROPOSAL_ID);
    }

    @Test
    void anyStatusExceptApprovedIsAConflict() {
        ConfirmProposal request = request(ProposalStatus.REJECTED);

        assertThrows(StatusConflictException.class, () -> service.confirmAndAssign(PROPOSAL_ID, request));

        assertEquals(OrderStatus.CUSTOMER_PENDING, order.getStatus());
        assertEquals(ProposalStatus.CREATED, proposal.getStatus());
        verify(proposalRepository, never()).rejectOthersForOrder(any(), any());
    }

    @Test
    void onlyTheOrderOwnerCanApprove() {
        when(authService.isCurrentUser(CUSTOMER_ID)).thenReturn(false);
        ConfirmProposal request = request(ProposalStatus.APPROVED);

        assertThrows(ForbiddenException.class, () -> service.confirmAndAssign(PROPOSAL_ID, request));

        assertEquals(OrderStatus.CUSTOMER_PENDING, order.getStatus());
    }

    @Test
    void approvingAMissingResponseIsNotFound() {
        ConfirmProposal request = request(ProposalStatus.APPROVED);

        assertThrows(NotFoundException.class, () -> service.confirmAndAssign(999L, request));
    }
}
