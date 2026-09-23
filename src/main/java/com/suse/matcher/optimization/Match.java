package com.suse.matcher.optimization;

import com.suse.matcher.deduction.facts.PotentialMatch;
import com.suse.matcher.deduction.facts.Product;
import com.suse.matcher.deduction.facts.Subscription;
import com.suse.matcher.deduction.facts.System;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.util.Comparator;
import java.util.Objects;
import java.util.StringJoiner;

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

    private static final Comparator<Match> DEFAULT_COMPARATOR = Comparator.comparing(Match::getId);

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
        return Boolean.TRUE.equals(confirmed);
    }

    /**
     * Sets this match confirmed.
     *
     * @param confirmedIn the new confirmed value
     */
    public void setConfirmed(boolean confirmedIn) {
        confirmed = confirmedIn;
    }

    @Override
    public int hashCode() {
        // confirmed is a planning variable, so it must not be in hashCode
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Match other)) {
            return false;
        }
        // confirmed is a planning variable, so it must not be in equals
        return Objects.equals(id, other.getId());
    }

    @Override
    public int compareTo(Match other) {
        return DEFAULT_COMPARATOR.compare(this, other);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Match.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("confirmed=" + confirmed)
                .toString();
    }
}
