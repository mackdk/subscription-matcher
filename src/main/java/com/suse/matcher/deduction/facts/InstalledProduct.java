package com.suse.matcher.deduction.facts;

import org.kie.api.definition.type.PropertyReactive;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents an installation relationship.
 */
@PropertyReactive
public class InstalledProduct {

    /** The system id. */
    private final long systemId;

    /** The product id. */
    private final long productId;

    /**
     * Instantiates a new installation relationship.
     *
     * @param systemIdIn the system id
     * @param productIdIn the product id
     */
    public InstalledProduct(long systemIdIn, long productIdIn) {
        systemId = systemIdIn;
        productId = productIdIn;
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

    @Override
    public int hashCode() {
        return Objects.hash(systemId, productId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InstalledProduct other)) {
            return false;
        }
        return Objects.equals(systemId, other.getSystemId())
                && Objects.equals(productId, other.getProductId());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InstalledProduct.class.getSimpleName() + "[", "]")
                .add("systemId=" + systemId)
                .add("productId=" + productId)
                .toString();
    }
}
