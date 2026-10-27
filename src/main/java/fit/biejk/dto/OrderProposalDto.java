package fit.biejk.dto;

import fit.biejk.entity.ProposalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a proposal related to an order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderProposalDto {

    /**
     * Unique identifier of the proposal.
     */
    private Long id;

    /**
     * Identifier of the associated order.
     */
    private Long orderId;

    /**
     * Identifier of the professional making the proposal.
     */
    private Long professionalId;

    /**
     * Description of the proposal. Must not be blank.
     */
    @NotBlank
    private String description;

    /**
     * Proposed price. Must not be null.
     */
    @NotNull
    private int price;

    /**
     * Current status of the proposal.
     */
    private ProposalStatus status;
}
