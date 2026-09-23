package com.suse.matcher.optimization.facts;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Representation of a confirmed match in a penalty group.
 **/
public class ConfirmedMatchInPenaltyGroup {

    private final long subscriptionId;
    private final int penaltyGroupId;
    private final long guestId;

    /**
     * Standard constructor.
     *
     * @param subscriptionIdIn - the subscription id
     * @param penaltyGroupIdIn - the penalty group id
     * @param guestIdIn - the guest id
     */
    public ConfirmedMatchInPenaltyGroup(Long subscriptionIdIn, int penaltyGroupIdIn, long guestIdIn) {
        this.subscriptionId = subscriptionIdIn;
        this.penaltyGroupId = penaltyGroupIdIn;
        this.guestId = guestIdIn;
    }

    /**
     * Gets the subscriptionId.
     *
     * @return subscriptionId
     */
    public Long getSubscriptionId() {
        return subscriptionId;
    }

    /**
     * Gets the penaltyGroupId.
     *
     * @return penaltyGroupId
     */
    public int getPenaltyGroupId() {
        return penaltyGroupId;
    }

    /**
     * Gets the guestId.
     *
     * @return guestId
     */
    public Long getGuestId() {
        return guestId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriptionId, penaltyGroupId, guestId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConfirmedMatchInPenaltyGroup other)) {
            return false;
        }
        return Objects.equals(penaltyGroupId, other.getPenaltyGroupId())
                && Objects.equals(guestId, other.getGuestId())
                && Objects.equals(subscriptionId, other.getSubscriptionId());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ConfirmedMatchInPenaltyGroup.class.getSimpleName() + "[", "]")
                .add("subscriptionId=" + subscriptionId)
                .add("penaltyGroupId=" + penaltyGroupId)
                .add("guestId=" + guestId)
                .toString();
    }
}
