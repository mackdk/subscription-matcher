package com.suse.matcher.deduction.facts;

import org.kie.api.definition.type.PropertyReactive;

import java.util.Comparator;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * A PotentialMatch is a potential application of a {@link Subscription} to a {@link System}
 * and a {@link Product}.
 *
 * Such a fact is generated if the Subscription-System-Product triple is a legal one on its
 * own, that is, without taking any other PotentialMatches into account.
 *
 * PotentialMatches can be grouped together. PotentialMatches in such groups must either be
 * be all matched or none of them can be matched.
 */
@PropertyReactive
public class PotentialMatch implements Comparable<PotentialMatch> {

    private static final Comparator<PotentialMatch> DEFAULT_COMPARATOR = Comparator.comparing(PotentialMatch::getSystemId)
            .thenComparing(PotentialMatch::getProductId)
            .thenComparing(PotentialMatch::getSubscriptionId)
            .thenComparing(PotentialMatch::getCentGroupId)
            .thenComparing(PotentialMatch::getGroupId);

    /** The system id. */
    private final long systemId;

    /** The product id. */
    private final long productId;

    /** The subscription id. */
    private final long subscriptionId;

    /** The group id. */
    private final int groupId;

    /** The id of the cent group used in this match. More matches can share same cent group. */
    private int centGroupId;

    /**
     * Standard constructor.
     *
     * @param systemIdIn a system id
     * @param productIdIn an id of a product
     * @param subscriptionIdIn an id of subscription assigned to the system
     * @param centGroupIdIn the id of cent group
     * @param groupIdIn the group id
     */
    public PotentialMatch(long systemIdIn, long productIdIn, long subscriptionIdIn, int centGroupIdIn, int groupIdIn) {
        systemId = systemIdIn;
        productId = productIdIn;
        subscriptionId = subscriptionIdIn;
        centGroupId = centGroupIdIn;
        groupId = groupIdIn;
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
     * Gets the product id.
     *
     * @return the product id
     */
    public long getProductId() {
        return productId;
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
     * Gets the id of the cent group used by this match.
     *
     * @return the centGroupId
     */
    public int getCentGroupId() {
        return centGroupId;
    }

    /**
     * Sets the centGroupId.
     *
     * @param centGroupIdIn - the centGroupId
     */
    public void setCentGroupId(int centGroupIdIn) {
        centGroupId = centGroupIdIn;
    }

    /**
     * Gets the group id.
     *
     * @return the group id
     */
    public int getGroupId() {
        return groupId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(systemId, productId, subscriptionId, centGroupId, groupId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PotentialMatch other)) {
            return false;
        }
        return Objects.equals(systemId, other.getSystemId())
                && Objects.equals(productId, other.getProductId())
                && Objects.equals(subscriptionId, other.getSubscriptionId())
                && Objects.equals(centGroupId, other.getCentGroupId())
                && Objects.equals(groupId, other.getGroupId());
    }

    @Override
    public int compareTo(PotentialMatch other) {
        return DEFAULT_COMPARATOR.compare(this, other);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PotentialMatch.class.getSimpleName() + "[", "]")
                .add("systemId=" + systemId)
                .add("productId=" + productId)
                .add("subscriptionId=" + subscriptionId)
                .add("centGroupId=" + centGroupId)
                .add("groupId=" + groupId)
                .toString();
    }
}
