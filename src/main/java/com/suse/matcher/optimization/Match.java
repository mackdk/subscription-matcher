package com.suse.matcher.optimization;

import com.suse.matcher.deduction.facts.PotentialMatch;
import com.suse.matcher.deduction.facts.Product;
import com.suse.matcher.deduction.facts.Subscription;
import com.suse.matcher.deduction.facts.System;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

/**
 * Represents a set of {@link PotentialMatch}es: {@link Subscription}-{@link System}-{@link Product}
 * associations.
 *
 * A Match can be confirmed by {@link OptaPlanner} or not depending on subscription
 * availability.
 *
 * Either all or none of the {@link PotentialMatch}es must be confirmed.
 */
@PlanningEntity
public class Match implements Comparable<Match> {

    /** A unique identifier for this Match. */
    private int id;

    /**
     * True if this match is taken by the planner, false if it is possible but
     * not taken, null if the planner did not evaluate this match yet.
     */
    @PlanningVariable(valueRangeProviderRefs = {"booleanRange"})
    private Boolean confirmed;

    /**
     * Standard constructor.
     *
     * @param idIn the id
     */
    public Match(int idIn) {
        id = idIn;
        confirmed = null;
    }

    /**
     * Default constructor.
     */
    Match() {
        // Empty constructor used by OptaPlanner
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Checks if this match has been confirmed.
     *
     * @return true if the match is confirmed
     */
    public boolean isConfirmed() {
        return BooleanUtils.isTrue(confirmed);
    }

    /**
     * Sets this match confirmed.
     *
     * @param confirmedIn the new confirmed value
     */
    public void setConfirmed(boolean confirmedIn) {
        confirmed = confirmedIn;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(id)
            // confirmed is a planning variable, so it must not be in hashCode
            .toHashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object objIn) {
        if (!(objIn instanceof Match)) {
            return false;
        }
        Match other = (Match) objIn;
        return new EqualsBuilder()
            .append(id, other.id)
            // confirmed is a planning variable, so it must not be in equals
            .isEquals();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("id", id)
            .append("confirmed", confirmed)
            .toString();
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Match other) {
        return new CompareToBuilder()
            .append(id, other.id)
            .toComparison();
    }
}
