package com.suse.matcher.json;

import java.util.Comparator;

/**
 * JSON representation of a match.
 * 
 * @param systemId the system id
 * @param subscriptionId the subscription id
 * @param productId the product id
 * @param cents the number of subscription cents used in this match
 */
public record JsonMatch (Long systemId, Long subscriptionId, Long productId, Integer cents)
    implements Comparable<JsonMatch> {

    private static final Comparator<JsonMatch> DEFAULT_COMPARATOR = Comparator.comparing(JsonMatch::systemId)
            .thenComparing(JsonMatch::productId)
            .thenComparing(JsonMatch::subscriptionId)
            .thenComparing(JsonMatch::cents);

    @Override
    public int compareTo(JsonMatch other) {
        return DEFAULT_COMPARATOR.compare(this, other);
    }
}
