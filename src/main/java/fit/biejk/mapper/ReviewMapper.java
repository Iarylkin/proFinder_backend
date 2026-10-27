package fit.biejk.mapper;

import fit.biejk.dto.ReviewDto;
import fit.biejk.entity.Review;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper for converting between {@link Review} entity and {@link ReviewDto}.
 */
@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReviewMapper {

    /**
     * Maps a {@link Review} entity to its corresponding {@link ReviewDto}.
     *
     * @param entity the review entity
     * @return the corresponding DTO
     */
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "specialist.id", target = "specialistId")
    ReviewDto toDto(Review entity);

    /**
     * Maps a {@link ReviewDto} to a {@link Review} entity.
     * <p>
     * Relations (customer, specialist, order) should be set manually in service.
     * </p>
     *
     * @param dto the review DTO
     * @return the entity (without full relations)
     */
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "specialist", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    Review toEntity(ReviewDto dto);

    /**
     * Converts a list of entities to a list of DTOs.
     *
     * @param entityList list of entities
     * @return list of DTOs
     */
    List<ReviewDto> toDtoList(List<Review> entityList);

    /**
     * Converts a list of DTOs to a list of entities (without relations).
     *
     * @param dtoList list of DTOs
     * @return list of entities
     */
    List<Review> toEntityList(List<ReviewDto> dtoList);
}
