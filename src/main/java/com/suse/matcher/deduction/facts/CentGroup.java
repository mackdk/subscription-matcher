package com.suse.matcher.deduction.facts;

import org.kie.api.definition.type.PropertyReactive;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Cent group - expresses the usage of subscription by a potential match(es)
 *
 * More potential matches can share the same cent group. If N potential matches have the same
 * cent group, only `cents` of the subscription are used (instead of N * `cents`).
 */
@PropertyReactive
public class CentGroup {

    /** The id **/
    private final int id;

    /** The number of cents consumed by this cent group **/
    private final int cents;

    public CentGroup(int id, int cents) {
        this.id = id;
        this.cents = cents;
    }
    /**
     * Gets the id.
     *
     * @return id
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the cents.
     *
     * @return cents
     */
    public int getCents() {
        return cents;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, cents);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CentGroup other)) {
            return false;
        }
        return Objects.equals(id, other.getId())
                && Objects.equals(cents, other.getCents());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CentGroup.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("cents=" + cents)
                .toString();
    }
}
