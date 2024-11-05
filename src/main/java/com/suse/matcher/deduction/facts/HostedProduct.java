package com.suse.matcher.deduction.facts;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.kie.api.definition.type.PropertyReactive;

/**
 * Represents a relationship between a Host and a product because of
 * an installation of that product on a Guest of the Host.
 */
@PropertyReactive
public class HostedProduct {

    /** The system id. */
    private final long systemId;

    /** The product id. */
    private final long productId;

    /**
     * Instantiates a new "hosted" installation relationship.
     *
     * @param systemIdIn the system id
     * @param productIdIn the product id
     */
    public HostedProduct(long systemIdIn, long productIdIn) {
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

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(systemId)
            .append(productId)
            .toHashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object objIn) {
        if (!(objIn instanceof HostedProduct)) {
            return false;
        }
        HostedProduct other = (HostedProduct) objIn;
        return new EqualsBuilder()
            .append(systemId, other.systemId)
            .append(productId, other.productId)
            .isEquals();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("systemId", systemId)
        .append("productId", productId)
        .toString();
    }
}
