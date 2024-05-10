package com.suse.matcher.deduction.facts;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.kie.api.definition.type.PropertyReactive;

import java.util.Date;

/**
 * An entitlement to use one or more products on one or more
 * {@link System}s.
 */
@PropertyReactive
public class Subscription implements Comparable<Subscription> {
    /**
     * Encodes virtual machine assignment policies for a {@link Subscription}
     */
    public enum Policy {
        /**
         * This subscription can exclusively be assigned to a physical system, and
         * virtual machines running on top of it will not get the same subscription
         * for free.
         */
        PHYSICAL_ONLY("Physical deployment only"),
        /**
         * This subscription can exclusively be assigned to a physical system, and
         * virtual machines running on top of it will automatically get the same
         * subscription for free.
         */
        UNLIMITED_VIRTUALIZATION("Unlimited Virtual Machines"),
        /**
         * This subscription can either be assigned to a physical system that does
         * not host virtual machines or to up to two virtual machines.
         */
        ONE_TWO("1-2 Sockets or 1-2 Virtual Machines"),
        /**
         * This subscription can either be assigned to a physical system as well
         * as to a virtual system. It is meant for an instance but does not allow
         * any virtualization inheritance
         */
        INSTANCE("Per-instance"),
        /**
         * This subscription refers to an extension product, as such it requires a
         * compatible base product to be installed on the same system. Assuming the
         * base product has a matching subscription, its policy it has will
         * also be applied to the extension product.
         */
        INHERITED_VIRTUALIZATION("Inherited");

        private final String description;

        Policy(String descriptionIn){
            description = descriptionIn;
        }

        /**
         * Gets the description of this policy.
         *
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return description;
        }
    }

    // constructor-populated fields

    /** The id. */
    private final long id;

    /** The part number. */
    private final String partNumber;

    /** SCC Username. */
    private final String sccUsername;

    /** The friendly name. */
    private String name;

    /** The number of subscription units (usually systems) available in this subscription. */
    private Integer quantity;

    /** Start Date. */
    private Date startDate;

    /** End Date. */
    private Date endDate;

    // rule-computed fields

    /** Virtualization policy. */
    private Policy policy;

    /** Support level identifier. */
    private String supportLevel;

    /**  Populated CPU sockets or IFLs (s390x architecture), null for "instance subscriptions". */
    private Integer cpus = null;

    /**   Can this subscription be used multiple times on the same system?. */
    private boolean stackable;

    /** The Hard Bundle Id that this subscription belongs to. If null, this doesn't belong to any Hard Bundle */
    private Long hardBundleId;

    /** Does this subscription on its own represent a hard bundle? */
    private boolean singleSubscriptionHardBundle = false;

    /** Id of subscription to which this subscription has been aggregated **/
    private Long aggregatedSubscriptionId;

    /** Should this subscription be matched at all? Eg. expired subscriptions can be ignored. */
    private Boolean ignored = false;

    /**
     * Instantiates a new subscription.
     *
     * @param idIn the id
     * @param partNumberIn the part number
     * @param nameIn the friendly name
     * @param quantityIn the quantity
     * @param startDateIn the starts at
     * @param endDateIn the expires at
     * @param sccUsernameIn the scc org id
     */
    public Subscription(long idIn, String partNumberIn, String nameIn, Integer quantityIn, Date startDateIn, Date endDateIn,
            String sccUsernameIn) {
        id = idIn;
        partNumber = partNumberIn;
        name = nameIn;
        quantity = quantityIn;
        startDate = startDateIn;
        endDate = endDateIn;
        sccUsername = sccUsernameIn;
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
     * Gets the part number.
     *
     * @return the part number
     */
    public String getPartNumber() {
        return partNumber;
    }

    /**
     * Gets the SCC username.
     *
     * @return the SCC username
     */
    public String getSccUsername() {
        return sccUsername;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param nameIn the name
     */
    public void setName(String nameIn) {
        name = nameIn;
    }

    /**
     * Gets the number of subscriptions available.
     *
     * @return the quantity
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * Sets the number of subscriptions available.
     *
     * @param quantityIn the quantity
     */
    public void setQuantity(Integer quantityIn) {
        this.quantity = quantityIn;
    }

    /**
     * Gets the start date.
     *
     * @return the start date
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Sets the start date.
     *
     * @param startDateIn the start date
     */
    public void setStartDate(Date startDateIn) {
        this.startDate = startDateIn;
    }

    /**
     * Gets the end date.
     *
     * @return the end date
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Sets the end date.
     *
     * @param endDateIn the end date
     */
    public void setEndDate(Date endDateIn) {
        this.endDate = endDateIn;
    }

    /**
     * Gets the virtualization policy.
     *
     * @return the virtualization policy
     */
    public Policy getPolicy() {
        return policy;
    }

    /**
     * Sets the virtualization policy.
     *
     * @param policyIn the virtualization policy
     */
    public void setPolicy(Policy policyIn) {
        this.policy = policyIn;
    }

    /**
     * Gets the support level.
     *
     * @return the support level
     */
    public String getSupportLevel() {
        return supportLevel;
    }

    /**
     * Sets the support level
     *
     * @param supportLevelIn the support level
     */
    public void setSupportLevel(String supportLevelIn) {
        this.supportLevel = supportLevelIn;
    }

    /**
     * Gets the cpus.
     *
     * @return the cpus
     */
    public Integer getCpus() {
        return cpus;
    }

    /**
     * Sets the cpus
     *
     * @param cpusIn the cpus
     */
    public void setCpus(Integer cpusIn) {
        this.cpus = cpusIn;
    }

    /**
     * Gets if the subscription is stackable.
     *
     * @return true if stackable
     */
    public boolean isStackable() {
        return stackable;
    }

    /**
     * Sets if the subscription is stackable
     *
     * @param stackableIn true if stackable
     */
    public void setStackable(boolean stackableIn) {
        this.stackable = stackableIn;
    }

    /**
     * @return Returns the hardBundleId.
     */
    public Long getHardBundleId() {
        return hardBundleId;
    }

    /**
     * @param hardBundleId The hardBundleId to set.
     */
    public void setHardBundleId(Long hardBundleId) {
        this.hardBundleId = hardBundleId;
    }

    /**
     * @return true if this subscription on its own represents a hard bundle
     */
    public boolean isSingleSubscriptionHardBundle() {
        return singleSubscriptionHardBundle;
    }

    /**
     * Sets the singleSubscriptionHardBundle.
     *
     * @param singleSubscriptionHardBundleIn - the singleSubscriptionHardBundle
     */
    public void setSingleSubscriptionHardBundle(boolean singleSubscriptionHardBundleIn) {
        singleSubscriptionHardBundle = singleSubscriptionHardBundleIn;
    }

    /**
     * Gets the id of the aggregate subscription.
     *
     * @return the id of the aggregate subscription
     */
    public Long getAggregatedSubscriptionId() {
        return aggregatedSubscriptionId;
    }

    /**
     * Sets the id of the aggregated subscription
     *
     * @param aggregatedSubscriptionIdIn the id of the aggregate subscription
     */
    public void setAggregatedSubscriptionId(Long aggregatedSubscriptionIdIn) {
        this.aggregatedSubscriptionId = aggregatedSubscriptionIdIn;
    }

    /**
     * Returns true if this subscription has to be ignored for the matching
     * (eg. because it is expired)
     *
     * @return true if the subscription is to be ignored
     */
    public boolean isIgnored() {
        return ignored;
    }

    /**
     * Sets if this subscription has to be ignored for the matching
     * (eg. because it is expired)
     *
     * @param ignoredIn true if the subscription is to be ignored
     */
    public void setIgnored(Boolean ignoredIn) {
        this.ignored = ignoredIn;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Subscription oIn) {
        return new CompareToBuilder().append(id, oIn.id).toComparison();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(id)
            .toHashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object objIn) {
        if (!(objIn instanceof Subscription)) {
            return false;
        }
        Subscription other = (Subscription) objIn;
        return new EqualsBuilder()
            .append(id, other.id)
            .isEquals();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("id", id)
            .append("hardBundleId", hardBundleId)
            .toString();
    }
}
