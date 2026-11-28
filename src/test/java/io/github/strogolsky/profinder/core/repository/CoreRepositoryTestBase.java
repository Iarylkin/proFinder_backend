package io.github.strogolsky.profinder.core.repository;

import io.github.strogolsky.profinder.core.entity.Bid;
import io.github.strogolsky.profinder.core.entity.BidStatus;
import io.github.strogolsky.profinder.core.entity.CustomerProfile;
import io.github.strogolsky.profinder.core.entity.Location;
import io.github.strogolsky.profinder.core.entity.Order;
import io.github.strogolsky.profinder.core.entity.OrderStatus;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.github.strogolsky.profinder.core.entity.Review;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Builds and saves the rows that the tests of the repositories of the {@code core} database need. The
 * profiles take the id of a user of the {@code auth} database: it is a plain id here, nothing else is needed.
 */
abstract class CoreRepositoryTestBase {

    @Inject
    LocationRepository locations;

    @Inject
    CustomerRepository customers;

    @Inject
    ProfessionalRepository professionals;

    @Inject
    OrderRepository orders;

    @Inject
    BidRepository bids;

    @Inject
    ReviewRepository reviews;

    Location location(final String name) {
        Location location = new Location();
        location.setName(name);
        locations.persist(location);
        return location;
    }

    CustomerProfile customer(final Location location) {
        CustomerProfile customer = new CustomerProfile();
        customer.setId(UUID.randomUUID());
        customer.setLocation(location);
        customers.persist(customer);
        return customer;
    }

    ProfessionalProfile professional(final Location location) {
        ProfessionalProfile professional = new ProfessionalProfile();
        professional.setId(UUID.randomUUID());
        professional.setLocation(location);
        professionals.persist(professional);
        return professional;
    }

    Order order(final CustomerProfile customer, final Location location, final String title) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setLocation(location);
        order.setStatus(OrderStatus.CREATED);
        order.setTitle(title);
        orders.persist(order);
        return order;
    }

    Bid bid(final Order order, final ProfessionalProfile professional, final int price) {
        Bid bid = new Bid();
        bid.setOrder(order);
        bid.setProfessional(professional);
        bid.setPrice(price);
        bid.setStatus(BidStatus.CREATED);
        bids.persist(bid);
        return bid;
    }

    Review review(final CustomerProfile customer, final ProfessionalProfile professional, final int rating) {
        Review review = new Review();
        review.setCustomer(customer);
        review.setProfessional(professional);
        review.setRating(rating);
        review.setComment("A comment");
        reviews.persist(review);
        return review;
    }
}
