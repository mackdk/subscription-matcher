package com.suse.matcher.optimization.facts;

import org.kie.api.definition.type.PropertyReactive;

/**
 * Penalty used for 1-2 subscriptions only.
 * {@see Penalty}
 */
@PropertyReactive
public class OneTwoPenalty extends Penalty {
    /**
     * Instantiates a new penalty.
     *
     * @param subscriptionIdIn the subscription id
     * @param penaltyGroupIdIn the penalty group id
     * @param centsIn          the penalty cents
     */
    public OneTwoPenalty(long subscriptionIdIn, int penaltyGroupIdIn, int centsIn) {
        super(subscriptionIdIn, penaltyGroupIdIn, centsIn);
    }
}
