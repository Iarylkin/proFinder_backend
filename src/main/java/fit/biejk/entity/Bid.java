package fit.biejk.entity;

import fit.biejk.validation.Limits;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a professional's bid for fulfilling an order.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bids")
public class Bid extends BaseEntity {

    /**
     * Professional who submitted the bid.
     */
    @ManyToOne
    @JoinColumn(name = "professional_id", nullable = false)
    private ProfessionalProfile professional;

    /**
     * Order associated with this bid.
     */
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Description provided by the professional in the bid.
     */
    @Column(name = "description", length = Limits.BID_DESCRIPTION_MAX_LENGTH)
    private String description;

    /**
     * Proposed price for fulfilling the order.
     */
    @Column(name = "price", nullable = false)
    private int price;

    /**
     * Current status of the bid.
     */
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private BidStatus status;
}
