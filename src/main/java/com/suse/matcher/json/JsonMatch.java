package com.suse.matcher.json;

/**
 * JSON representation of a match.
 */
public class JsonMatch {

    /** The system id. */
    private Long systemId;

    /** The subscription id. */
    private Long subscriptionId;

    /** The product id. */
    private Long productId;

    /** The number of subscription cents used in this match. */
    private Integer cents;

    /**
     * Standard constructor.
     *
     * @param systemIdIn the system id
     * @param subscriptionIdIn the subscription id
     * @param productIdIn the product id
     * @param centsIn the number of subscription cents used in this match
     */
    public JsonMatch(Long systemIdIn, Long subscriptionIdIn, Long productIdIn, Integer centsIn) {
        systemId = systemIdIn;
        subscriptionId = subscriptionIdIn;
        productId = productIdIn;
        cents = centsIn;
    }

    /**
     * Gets the system id.
     *
     * @return the system id
     */
    public Long getSystemId() {
        return systemId;
    }

    /**
     * Sets the system id.
     *
     * @param systemIdIn the new system id
     */
    public void setSystemId(Long systemIdIn) {
        systemId = systemIdIn;
    }

    /**
     * Gets the subscription id.
     *
     * @return the subscription id
     */
    public Long getSubscriptionId() {
        return subscriptionId;
    }

    /**
     * Sets the subscription id.
     *
     * @param subscriptionIdIn the new subscription id
     */
    public void setSubscriptionId(Long subscriptionIdIn) {
        subscriptionId = subscriptionIdIn;
    }

    /**
     * Gets the product id.
     *
     * @return the product id
     */
    public Long getProductId() {
        return productId;
    }

    /**
     * Sets the product id.
     *
     * @param productIdIn the new product id
     */
    public void setProductId(Long productIdIn) {
        productId = productIdIn;
    }

    /**
     * Gets the number of subscription cents used in this match.
     *
     * @return the number of subscription cents used in this match
     */
    public Integer getCents() {
        return cents;
    }

    /**
     * Sets the number of subscription cents used in this match.
     *
     * @param centsIn the new number of subscription cents used in this match
     */
    public void setCents(Integer centsIn) {
        cents = centsIn;
    }

}
