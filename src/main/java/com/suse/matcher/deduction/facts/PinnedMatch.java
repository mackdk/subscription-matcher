package com.suse.matcher.deduction.facts;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents a match that the customer would like to see in the solution.
 */
public class PinnedMatch {

    /** The system id. */
    private long systemId;

    /** The subscription id. */
    private long subscriptionId;

    /**
     * Standard constructor.
     *
     * @param systemIdIn a system id
     * @param subscriptionIdIn an id of subscription assigned to the system
     */
    public PinnedMatch(long systemIdIn, long subscriptionIdIn) {
        systemId = systemIdIn;
        subscriptionId = subscriptionIdIn;
    }

    /**
     * Default constructor.
     */
    public PinnedMatch() {
    }

    /**
     * Gets the system id.
     *
     * @return the system id
     */
    public long getSystemId() {
        return systemId;
    }

    /**
     * Gets the subscription id.
     *
     * @return the subscription id
     */
    public long getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(systemId, subscriptionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PinnedMatch other)) {
            return false;
        }
        return Objects.equals(systemId, other.getSystemId())
                && Objects.equals(subscriptionId, other.getSubscriptionId());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PinnedMatch.class.getSimpleName() + "[", "]")
                .add("systemId=" + systemId)
                .add("subscriptionId=" + subscriptionId)
                .toString();
    }
}
