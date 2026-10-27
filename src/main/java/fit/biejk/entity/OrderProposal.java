package fit.biejk.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GenerationType;
import jakarta.persistence.EnumType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a professional's proposal for fulfilling an order.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_proposal")
public class OrderProposal {

    /**
     * Unique identifier for the order proposal.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Professional who submitted the proposal.
     */
    @ManyToOne
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    /**
     * Order associated with this proposal.
     */
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Description provided by the professional in the proposal.
     */
    @Column(name = "description")
    private String description;

    /**
     * Proposed price for fulfilling the order.
     */
    @Column(name = "price", nullable = false)
    private int price;

    /**
     * Current status of the proposal.
     */
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ProposalStatus status;
}
