package com.suse.matcher.deduction.facts;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents mapping of a virtual guest to a penalty group for 1-2 subscriptions.
 *
 * Penalty Group is a group of virtual guests for which a 1-2 subscription is splittable.
 */
public class PenaltyGroup {

    private final int id;
    private final long guestId;

    /**
     * Standard constructor.
     * @param idIn - the id
     * @param guestIdIn - the guest id
     */
    public PenaltyGroup(int idIn, long guestIdIn) {
        id = idIn;
        guestId = guestIdIn;
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
     * Gets the guestId.
     *
     * @return guestId
     */
    public Long getGuestId() {
        return guestId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, guestId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PenaltyGroup other)) {
            return false;
        }
        return Objects.equals(id, other.getId())
                && Objects.equals(guestId, other.getGuestId());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PenaltyGroup.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("guestId=" + guestId)
                .toString();
    }
}
