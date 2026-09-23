package com.suse.matcher.optimization.facts;

import org.kie.api.definition.type.PropertyReactive;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * When calculating the total amount of subscription cents used, there
 * might be occasions in which a penalty is applied. This fact encapsulates
 * the penalty applied to a certain subscription and penalty group.
 */
@PropertyReactive
public class Penalty {

    /** The subscription id. */
    private final long subscriptionId;

    /** The penalty group id. */
    private final int penaltyGroupId;

    /** The penalty cents. */
    private final int cents;

    /**
     * Instantiates a new penalty.
     *
     * @param subscriptionIdIn the subscription id
     * @param penaltyGroupIdIn the penalty group id
     * @param centsIn the penalty cents
     */
    public Penalty(long subscriptionIdIn, int penaltyGroupIdIn, int centsIn) {
        subscriptionId = subscriptionIdIn;
        penaltyGroupId = penaltyGroupIdIn;
        cents = centsIn;
    }

    /**
     * Gets the subscription id.
     *
     * @return the subscription id
     */
    public long getSubscriptionId() {
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
     * Gets the penalty cents.
     *
     * @return the penalty cents
     */
    public int getCents() {
        return cents;
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriptionId, penaltyGroupId, cents);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Penalty other)) {
            return false;
        }
        return Objects.equals(subscriptionId, other.getSubscriptionId())
                && Objects.equals(penaltyGroupId, other.getPenaltyGroupId())
                && Objects.equals(cents, other.getCents());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Penalty.class.getSimpleName() + "[", "]")
                .add("subscriptionId=" + subscriptionId)
                .add("penaltyGroupId=" + penaltyGroupId)
                .add("penaltyCents=" + cents)
                .toString();
    }
}
