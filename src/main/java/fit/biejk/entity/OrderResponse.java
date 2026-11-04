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
 * Entity representing a professional's response for fulfilling an order.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "responses")
public class OrderResponse extends BaseEntity {

    /**
     * Professional who submitted the response.
     */
    @ManyToOne
    @JoinColumn(name = "professional_id", nullable = false)
    private ProfessionalProfile professional;

    /**
     * Order associated with this response.
     */
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Description provided by the professional in the response.
     */
    @Column(name = "description", length = Limits.RESPONSE_DESCRIPTION_MAX_LENGTH)
    private String description;

    /**
     * Proposed price for fulfilling the order.
     */
    @Column(name = "price", nullable = false)
    private int price;

    /**
     * Current status of the response.
     */
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderResponseStatus status;
}
