package com.suse.matcher.deduction.facts;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(systemId)
            .append(subscriptionId)
            .toHashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object objIn) {
        if (!(objIn instanceof PinnedMatch)) {
            return false;
        }
        PinnedMatch other = (PinnedMatch) objIn;
        return new EqualsBuilder()
            .append(systemId, other.systemId)
            .append(subscriptionId, other.subscriptionId)
            .isEquals();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("systemId", systemId)
            .append("subscriptionId", subscriptionId)
            .toString();
    }
}
