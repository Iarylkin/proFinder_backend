package io.github.strogolsky.profinder.core.repository;

import io.github.strogolsky.profinder.core.entity.Bid;
import io.github.strogolsky.profinder.core.entity.BidStatus;
import io.github.strogolsky.profinder.core.entity.CustomerProfile;
import io.github.strogolsky.profinder.core.entity.Location;
import io.github.strogolsky.profinder.core.entity.Order;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.github.strogolsky.profinder.core.entity.Review;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The queries of the repositories of the {@code core} database, on a real PostgreSQL database. Every test rolls
 * back what it saved. A page starts at 1.
 */
@QuarkusTest
class CoreRepositoriesTest extends CoreRepositoryTestBase {

    private static final int PAGE_SIZE = 2;

    private Location prague;
    private CustomerProfile anna;
    private CustomerProfile petr;
    private ProfessionalProfile jan;
    private ProfessionalProfile eva;

    private void data() {
        prague = location("Prague-" + UUID.randomUUID());
        anna = customer(prague);
        petr = customer(prague);
        jan = professional(prague);
        eva = professional(prague);
    }

    private static Set<UUID> ids(final List<? extends io.github.strogolsky.profinder.common.entity.BaseEntity> list) {
        Set<UUID> ids = new HashSet<>();
        list.forEach(e -> ids.add(e.getId()));
        return ids;
    }

    // ---- LocationRepository ----

    @Test
    @TestTransaction
    void locationIsFoundByItsName() {
        Location saved = location("Atlantis-" + UUID.randomUUID());

        assertEquals(saved.getId(), locations.findByName(saved.getName()).getId());
    }

    @Test
    @TestTransaction
    void unknownLocationNameGivesNull() {
        assertNull(locations.findByName("Nowhere-" + UUID.randomUUID()));
    }

    // ---- OrderRepository ----

    @Test
    @TestTransaction
    void ordersOfACustomerAreOnlyHisOwn() {
        data();
        Order first = order(anna, prague, "First");
        Order second = order(anna, prague, "Second");
        order(petr, prague, "Not Anna's");

        List<Order> result = orders.findByCustomerId(anna.getId(), 1, 10);

        assertEquals(Set.of(first.getId(), second.getId()), ids(result));
    }

    @Test
    @TestTransaction
    void ordersOfACustomerAreReadByPageWhichStartsAtOne() {
        data();
        order(anna, prague, "A");
        order(anna, prague, "B");
        order(anna, prague, "C");

        List<Order> firstPage = orders.findByCustomerId(anna.getId(), 1, PAGE_SIZE);
        List<Order> secondPage = orders.findByCustomerId(anna.getId(), 2, PAGE_SIZE);

        assertEquals(2, firstPage.size());
        assertEquals(1, secondPage.size());
        Set<UUID> all = ids(firstPage);
        all.addAll(ids(secondPage));
        assertEquals(3, all.size(), "no order is repeated or missing between the pages");
    }

    @Test
    @TestTransaction
    void customerWithoutOrdersGetsAnEmptyList() {
        data();

        assertTrue(orders.findByCustomerId(anna.getId(), 1, 10).isEmpty());
    }

    // ---- BidRepository ----

    @Test
    @TestTransaction
    void bidsOfAnOrderAreOnlyItsOwn() {
        data();
        Order sink = order(anna, prague, "Sink");
        Order roof = order(anna, prague, "Roof");
        Bid byJan = bid(sink, jan, 100);
        Bid byEva = bid(sink, eva, 120);
        bid(roof, jan, 300);

        assertEquals(Set.of(byJan.getId(), byEva.getId()), ids(bids.findByOrderId(sink.getId(), 1, 10)));
    }

    @Test
    @TestTransaction
    void bidsOfAProfessionalAreOnlyHisOwn() {
        data();
        Order sink = order(anna, prague, "Sink");
        Order roof = order(petr, prague, "Roof");
        Bid onSink = bid(sink, jan, 100);
        Bid onRoof = bid(roof, jan, 300);
        bid(sink, eva, 120);

        assertEquals(Set.of(onSink.getId(), onRoof.getId()), ids(bids.findByProfessionalId(jan.getId(), 1, 10)));
    }

    @Test
    @TestTransaction
    void bidsAreReadByPageWhichStartsAtOne() {
        data();
        Order sink = order(anna, prague, "Sink");
        bid(sink, jan, 1);
        bid(sink, eva, 2);
        bid(sink, professional(prague), 3);

        List<Bid> firstPage = bids.findByOrderId(sink.getId(), 1, PAGE_SIZE);
        List<Bid> secondPage = bids.findByOrderId(sink.getId(), 2, PAGE_SIZE);

        assertEquals(2, firstPage.size());
        assertEquals(1, secondPage.size());
    }

    @Test
    @TestTransaction
    void approvingOneBidRejectsOnlyTheOtherBidsOfThatOrder() {
        data();
        Order sink = order(anna, prague, "Sink");
        Order roof = order(anna, prague, "Roof");
        Bid approved = bid(sink, jan, 100);
        Bid other = bid(sink, eva, 120);
        Bid ofAnotherOrder = bid(roof, eva, 300);

        bids.rejectOthersForOrder(sink.getId(), approved.getId());
        bids.getEntityManager().clear();

        assertEquals(BidStatus.CREATED, bids.findById(approved.getId()).getStatus());
        assertEquals(BidStatus.REJECTED, bids.findById(other.getId()).getStatus());
        assertEquals(BidStatus.CREATED, bids.findById(ofAnotherOrder.getId()).getStatus());
    }

    // ---- ReviewRepository ----

    @Test
    @TestTransaction
    void reviewsOfAProfessionalAreOnlyHisOwn() {
        data();
        Review forJan = review(anna, jan, 5);
        Review alsoForJan = review(petr, jan, 3);
        review(anna, eva, 4);

        assertEquals(Set.of(forJan.getId(), alsoForJan.getId()),
                ids(reviews.findByProfessional(jan, 1, 10)));
    }

    @Test
    @TestTransaction
    void reviewsOfACustomerAreOnlyHisOwn() {
        data();
        Review byAnna = review(anna, jan, 5);
        Review alsoByAnna = review(anna, eva, 4);
        review(petr, jan, 3);

        assertEquals(Set.of(byAnna.getId(), alsoByAnna.getId()), ids(reviews.findByCustomer(anna, 1, 10)));
    }

    @Test
    @TestTransaction
    void ratingsIncludeTheReviewSavedASecondAgoWithoutAnExplicitFlush() {
        data();
        review(anna, jan, 5);
        review(petr, jan, 3);
        review(anna, eva, 1);

        List<Integer> ratings = reviews.findRatingsByProfessionalId(jan.getId());

        assertEquals(2, ratings.size());
        assertTrue(ratings.containsAll(List.of(5, 3)));
    }

    @Test
    @TestTransaction
    void professionalWithoutReviewsHasNoRatings() {
        data();

        assertTrue(reviews.findRatingsByProfessionalId(jan.getId()).isEmpty());
    }

    // ---- the profiles ----

    @Test
    @TestTransaction
    void aSavedProfileGetsItsLocationAndAnOwnCreationTime() {
        data();
        professionals.getEntityManager().flush();
        professionals.getEntityManager().clear();

        ProfessionalProfile read = professionals.findById(jan.getId());

        assertNotNull(read);
        assertEquals(prague.getId(), read.getLocation().getId());
        assertNotNull(read.getCreateAt());
    }
}
