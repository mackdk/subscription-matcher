package com.suse.matcher.json;

/**
 * JSON representation of a product.
 */
public class JsonProduct {

    /** The id. */
    private Long id;

    /** The friendly name. */
    private String name;

    /** The product class */
    private String productClass;

    /** true if this is a free product. */
    private Boolean free;

    /** true if this is a base product. */
    private Boolean base;

    /**
     * Standard constructor.
     *
     * @param idIn the id
     * @param nameIn the name
     * @param productClassIn the productClass
     * @param freeIn true if this is a free product
     * @param baseIn true if this is a base product
     */
    public JsonProduct(Long idIn, String nameIn, String productClassIn, Boolean freeIn, Boolean baseIn) {
        id = idIn;
        name = nameIn;
        productClass = productClassIn;
        free = freeIn;
        base = baseIn;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param idIn the new id
     */
    public void setId(Long idIn) {
        id = idIn;
    }

    /**
     * Gets the friendly name.
     *
     * @return the friendly name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the friendly name.
     *
     * @param nameIn the new friendly name
     */
    public void setName(String nameIn) {
        name = nameIn;
    }

    /**
     * @return the productClass
     */
    public String getProductClass() {
        return productClass;
    }

    /**
     * @param productClassIn the productClass to set
     */
    public void setProductClass(String productClassIn) {
        this.productClass = productClassIn;
    }

    /**
     * Checks if the product is free.
     *
     * @return true if this is a free product
     */
    public Boolean getFree() {
        return free;
    }

    /**
     * Changes whether this is a free product or not.
     *
     * @param freeIn true if this is a free product
     */
    public void setFree(Boolean freeIn) {
        free = freeIn;
    }

    /**
     * Returns true if this is a base product.
     *
     * @return true if this is a base product
     */
    public Boolean getBase() {
        return base;
    }

    /**
     * Set to true if this is a base product.
     *
     * @param baseIn true if this is a base product
     */
    public void setBase(Boolean baseIn) {
        base = baseIn;
    }
}
