package fit.biejk.mapper;

import fit.biejk.dto.ReviewCreateRequest;
import fit.biejk.dto.ReviewResponse;
import fit.biejk.entity.Review;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper for converting between {@link Review} entity and {@link ReviewResponse}.
 */
@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReviewMapper {

    /**
     * Maps a {@link Review} entity to its corresponding {@link ReviewResponse}.
     *
     * @param entity the review entity
     * @return the corresponding DTO
     */
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "professional.id", target = "professionalId")
    ReviewResponse toDto(Review entity);

    /**
     * Maps a {@link ReviewCreateRequest} to a {@link Review} entity.
     * <p>
     * Relations (customer, professional) are set manually in the service.
     * </p>
     *
     * @param request the request to create a review
     * @return the entity (without full relations)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "professional", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    Review toEntity(ReviewCreateRequest request);

    /**
     * Converts a list of entities to a list of DTOs.
     *
     * @param entityList list of entities
     * @return list of DTOs
     */
    List<ReviewResponse> toDtoList(List<Review> entityList);
}
