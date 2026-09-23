package com.suse.matcher.deduction.facts;

import org.kie.api.definition.type.PropertyReactive;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents a product that can be matched.
 */
@PropertyReactive
public class Product {

    /** The product id. */
    private final long id;

    /** The friendly name. */
    private final String name;

    /** The product class */
    private final String productClass;

    /** true if this is a free product. */
    private final boolean free;

    /** true if this is a base product. */
    private final boolean base;

    /**
     * Instantiates a new product.
     *
     * @param idIn the product id
     * @param nameIn the friendly name
     * @param productClassIn the productClass
     * @param freeIn true if this is a free product
     * @param baseIn true if this is a base product
     */
    public Product(long idIn, String nameIn, String productClassIn, boolean freeIn, boolean baseIn) {
        id = idIn;
        name = nameIn;
        productClass = productClassIn;
        free = freeIn;
        base = baseIn;
    }

    /**
     * Gets the product id.
     *
     * @return the product id
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the friendly name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the productClass
     */
    public String getProductClass() {
        return productClass;
    }

    /**
     * Checks if the product is free.
     *
     * @return true if this is a free product
     */
    public boolean isFree() {
        return free;
    }

    /**
     * Returns true if this is a base product.
     *
     * @return true if this is a base product
     */
    public boolean isBase() {
        return base;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product other)) {
            return false;
        }
        return Objects.equals(id, other.getId());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Product.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("productClass=" + productClass)
                .add("name=" + name)
                .toString();
    }
}
