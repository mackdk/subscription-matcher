package com.suse.matcher.deduction.facts;

import org.kie.api.definition.type.PropertyReactive;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Marker class expressing that a group of matches was generated from Inherited virtualization
 * matches. This is to stop infinite re-activation.
 */
@PropertyReactive
public class GroupInInheritedVirtualization {

    private final int groupId;

    /**
     * Standard constructor.
     *
     * @param groupIdIn - the group id
     */
    public GroupInInheritedVirtualization(int groupIdIn) {
        this.groupId = groupIdIn;
    }

    /**
     * Gets the groupId.
     *
     * @return groupId
     */
    public int getGroupId() {
        return groupId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof GroupInInheritedVirtualization other)) {
            return false;
        }

        return Objects.equals(groupId, other.getGroupId());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GroupInInheritedVirtualization.class.getSimpleName() + "[", "]")
            .add("groupId=" + groupId)
            .toString();
    }
}
