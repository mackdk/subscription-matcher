package com.suse.matcher.deduction.facts;

import org.kie.api.definition.type.PropertyReactive;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents a subscription to product relationship.
 */
@PropertyReactive
public class SubscriptionProduct {

    /** The subscription id. */
    private final long subscriptionId;

    /** The product id. */
    private final long productId;

    /**
     * Instantiates a new installation relationship.
     *
     * @param subscriptionIdIn the subscription id
     * @param productIdIn the product id
     */
    public SubscriptionProduct(long subscriptionIdIn, long productIdIn) {
        subscriptionId = subscriptionIdIn;
        productId = productIdIn;
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
     * Gets the product id.
     *
     * @return the product id
     */
    public long getProductId() {
        return productId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriptionId, productId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubscriptionProduct other)) {
            return false;
        }
        return Objects.equals(subscriptionId, other.getSubscriptionId())
                && Objects.equals(productId, other.getProductId());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SubscriptionProduct.class.getSimpleName() + "[", "]")
                .add("subscriptionId=" + subscriptionId)
                .add("productId=" + productId)
                .toString();
    }
}
