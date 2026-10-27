package fit.biejk.entity;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Enumeration representing the possible statuses of an order.
 * <p>
 * Each status can define a set of valid transitions to other statuses.
 * </p>
 */
@Slf4j
public enum OrderStatus {

    /**
     * Initial status when an order is first created.
     */
    CREATED,

    /**
     * Status when an order is cancelled before completion.
     */
    CANCELLED,

    /**
     * Status indicating the client needs to take action (e.g., review proposals).
     */
    CLIENT_PENDING,

    /**
     * Status indicating the order has been fulfilled and is now completed.
     */
    COMPLETED;

    /**
     * Mapping of allowed transitions from one status to another.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = new HashMap<>();

    static {
        VALID_TRANSITIONS.put(CREATED, Set.of(CANCELLED, CLIENT_PENDING));
        VALID_TRANSITIONS.put(CLIENT_PENDING, Set.of(CLIENT_PENDING, CANCELLED, COMPLETED));
        VALID_TRANSITIONS.put(COMPLETED, Set.of()); // final state
        VALID_TRANSITIONS.put(CANCELLED, Set.of()); // final state
    }

    /**
     * Checks whether an order in this status still accepts new proposals.
     *
     * @return {@code true} for open orders, {@code false} for cancelled or completed ones.
     */
    public boolean acceptsProposals() {
        return this == CREATED || this == CLIENT_PENDING;
    }

    /**
     * Checks if transitioning from the current status to the specified new status is allowed.
     *
     * @param newStatus The status to transition to.
     * @return {@code true} if the transition is valid, {@code false} otherwise.
     */
    public boolean isValidTransition(final OrderStatus newStatus) {
        log.debug("Checking if transition from {} to {} is valid", this, newStatus);
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(newStatus);
    }

    /**
     * Retrieves the initial status used when creating new orders.
     *
     * @return The initial status {@link #CREATED}.
     */
    public static OrderStatus getStartOrder() {
        return CREATED;
    }

    /**
     * Attempts to perform a transition from the current status to the specified one.
     *
     * @param newStatus The desired status to transition to.
     * @return The new status if the transition is allowed.
     * @throws IllegalStateException if the transition is invalid.
     */
    public OrderStatus transitionTo(final OrderStatus newStatus) {
        log.info("Attempting transition from {} to {}", this, newStatus);
        if (!isValidTransition(newStatus)) {
            log.error("Invalid status transition from {} to {}", this, newStatus);
            throw new IllegalStateException("Invalid transition from " + this + " to " + newStatus);
        }
        log.debug("Transition from {} to {} successful", this, newStatus);
        return newStatus;
    }
}
