package io.github.strogolsky.profinder.core.mapper;

import io.github.strogolsky.profinder.core.dto.BidCreateRequest;
import io.github.strogolsky.profinder.core.dto.BidResponse;
import io.github.strogolsky.profinder.core.dto.ReviewCreateRequest;
import io.github.strogolsky.profinder.core.dto.ReviewResponse;
import io.github.strogolsky.profinder.core.entity.Bid;
import io.github.strogolsky.profinder.core.entity.BidStatus;
import io.github.strogolsky.profinder.core.entity.CustomerProfile;
import io.github.strogolsky.profinder.core.entity.Order;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.github.strogolsky.profinder.core.entity.Review;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The conversion of a review and of a bid to what the client sees, and of what the client sends to an entity
 * that holds only the fields of the request: the server sets the owner, the status and the ids.
 */
class ReviewAndBidMapperTest {

    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final UUID BID_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID PROFESSIONAL_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    private final ReviewMapper reviewMapper = new ReviewMapperImpl();
    private final BidMapper bidMapper = new BidMapperImpl();

    private static Review review() {
        CustomerProfile customer = new CustomerProfile();
        customer.setId(CUSTOMER_ID);
        ProfessionalProfile professional = new ProfessionalProfile();
        professional.setId(PROFESSIONAL_ID);
        Review review = new Review();
        review.setId(REVIEW_ID);
        review.setCustomer(customer);
        review.setProfessional(professional);
        review.setRating(4);
        review.setComment("Good work");
        return review;
    }

    private static Bid bid() {
        Order order = new Order();
        order.setId(ORDER_ID);
        ProfessionalProfile professional = new ProfessionalProfile();
        professional.setId(PROFESSIONAL_ID);
        Bid bid = new Bid();
        bid.setId(BID_ID);
        bid.setVersion(2L);
        bid.setOrder(order);
        bid.setProfessional(professional);
        bid.setDescription("I can do it");
        bid.setPrice(500);
        bid.setStatus(BidStatus.CREATED);
        return bid;
    }

    @Test
    void reviewResponseCarriesTheIdsTheRatingAndTheComment() {
        ReviewResponse dto = reviewMapper.toDto(review());

        assertEquals(REVIEW_ID, dto.id());
        assertEquals(CUSTOMER_ID, dto.customerId());
        assertEquals(PROFESSIONAL_ID, dto.professionalId());
        assertEquals(4, dto.rating());
        assertEquals("Good work", dto.comment());
    }

    @Test
    void reviewsAreConvertedAsAList() {
        List<ReviewResponse> dtos = reviewMapper.toDtoList(List.of(review(), review()));

        assertEquals(2, dtos.size());
        assertEquals(PROFESSIONAL_ID, dtos.get(1).professionalId());
    }

    @Test
    void reviewFromARequestHoldsOnlyTheRatingAndTheComment() {
        Review created = reviewMapper.toEntity(new ReviewCreateRequest("Very good", 5));

        assertEquals(5, created.getRating());
        assertEquals("Very good", created.getComment());
        assertNull(created.getId());
        assertNull(created.getCustomer());
        assertNull(created.getProfessional());
    }

    @Test
    void bidResponseCarriesTheIdsThePriceAndTheStatus() {
        BidResponse dto = bidMapper.toDto(bid());

        assertEquals(BID_ID, dto.id());
        assertEquals(2L, dto.version());
        assertEquals(ORDER_ID, dto.orderId());
        assertEquals(PROFESSIONAL_ID, dto.professionalId());
        assertEquals("I can do it", dto.description());
        assertEquals(500, dto.price());
        assertEquals(BidStatus.CREATED, dto.status());
    }

    @Test
    void bidsAreConvertedAsAList() {
        assertEquals(2, bidMapper.toDtoList(List.of(bid(), bid())).size());
    }

    @Test
    void bidFromARequestHoldsOnlyTheDescriptionAndThePrice() {
        Bid created = bidMapper.toEntity(new BidCreateRequest("My offer", 300));

        assertEquals("My offer", created.getDescription());
        assertEquals(300, created.getPrice());
        assertNull(created.getId());
        assertNull(created.getVersion());
        assertNull(created.getOrder());
        assertNull(created.getProfessional());
        assertNull(created.getStatus());
    }
}
