package com.suse.matcher.facts;

import org.kie.api.definition.type.PropertyReactive;

/**
 * Marker class expressing that a group of matches was generated from Inherited virtualization
 * matches. This is to stop infinite re-activation.
 */
@PropertyReactive
public class GroupInInheritedVirtualization {

    private int groupId;

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

    /**
     * Sets the groupId.
     *
     * @param groupIdIn - the groupId
     */
    public void setGroupId(int groupIdIn) {
        groupId = groupIdIn;
    }
}
