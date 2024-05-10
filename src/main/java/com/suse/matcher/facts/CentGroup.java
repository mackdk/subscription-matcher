package com.suse.matcher.facts;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.kie.api.definition.type.PropertyReactive;

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
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        CentGroup centGroup = (CentGroup) o;

        return new EqualsBuilder()
                .append(id, centGroup.id)
                .append(cents, centGroup.cents)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(cents)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .append("cents", cents)
                .toString();
    }
}
