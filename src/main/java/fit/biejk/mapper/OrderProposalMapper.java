package fit.biejk.mapper;

import fit.biejk.dto.OrderProposalDto;
import fit.biejk.entity.OrderProposal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link OrderProposal}
 * entities and {@link OrderProposalDto} data transfer objects.
 */
@Mapper(componentModel = "jakarta")
public interface OrderProposalMapper {

    /**
     * Converts an OrderProposalDto to an OrderProposal entity.
     * Ignores nested order and professional IDs during mapping.
     *
     * @param orderProposalDto the DTO to convert
     * @return the mapped OrderProposal entity
     */
    @Mapping(target = "order.id", ignore = true)
    @Mapping(target = "professional.id", ignore = true)
    OrderProposal toEntity(OrderProposalDto orderProposalDto);

    /**
     * Converts an OrderProposal entity to an OrderProposalDto.
     * Maps the IDs of associated order and professional.
     *
     * @param orderProposal the entity to convert
     * @return the mapped OrderProposalDto
     */
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "professionalId", source = "professional.id")
    OrderProposalDto toDto(OrderProposal orderProposal);

    /**
     * Converts a list of OrderProposalDto objects to a list of entities.
     *
     * @param orderProposalDtoList list of DTOs
     * @return list of OrderProposal entities
     */
    List<OrderProposal> toEntityList(List<OrderProposalDto> orderProposalDtoList);

    /**
     * Converts a list of OrderProposal entities to a list of DTOs.
     *
     * @param orderProposalList list of entities
     * @return list of OrderProposalDto objects
     */
    List<OrderProposalDto> toDtoList(List<OrderProposal> orderProposalList);
}
