package com.suse.matcher.deduction.facts;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * A relationship between a Virtualization Group and one of its Virtual Guest members.
 *
 * A Virtualization Group contains virtual guests which belong to the same cloud, VMWare
 * vCenter, etc.
 */
public class VirtualizationGroupMember {

    /** The virtualization group id. */
    private final int virtualizationGroupId;

    /** The guest system id. */
    private final long guestId;

    /**
     * Instantiates a new virtualization group member.
     *
     * @param virtualizationGroupIdIn the virtualization group id in
     * @param guestIdIn the guest id in
     */
    public VirtualizationGroupMember(int virtualizationGroupIdIn, long guestIdIn) {
        virtualizationGroupId = virtualizationGroupIdIn;
        guestId = guestIdIn;
    }

    /**
     * Gets the virtualization group id.
     *
     * @return the virtualization group id
     */
    public int getVirtualizationGroupId() {
        return virtualizationGroupId;
    }

    /**
     * Gets the guest system id.
     *
     * @return the guest system id
     */
    public long getGuestId() {
        return guestId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(virtualizationGroupId, guestId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VirtualizationGroupMember other)) {
            return false;
        }
        return Objects.equals(virtualizationGroupId, other.getVirtualizationGroupId())
                && Objects.equals(guestId, other.getGuestId());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", VirtualizationGroupMember.class.getSimpleName() + "[", "]")
                .add("virtualizationGroupId=" + virtualizationGroupId)
                .add("guestId=" + guestId)
                .toString();
    }
}
