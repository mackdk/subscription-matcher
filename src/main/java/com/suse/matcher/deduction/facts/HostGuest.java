package com.suse.matcher.deduction.facts;

import org.kie.api.definition.type.PropertyReactive;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents a virtual host-to-guest relationship.
 */
@PropertyReactive
public class HostGuest {

    /** The host system id. */
    private final long hostId;

    /** The guest system id. */
    private final long guestId;

    /**
     * Instantiates a new host-to-guest relationship object.
     *
     * @param hostIdIn the host id
     * @param guestIdIn the guest id
     */
    public HostGuest(long hostIdIn, long guestIdIn) {
        hostId = hostIdIn;
        guestId = guestIdIn;
    }

    /**
     * Gets the host id.
     *
     * @return the host id
     */
    public long getHostId() {
        return hostId;
    }

    /**
     * Gets the guest id.
     *
     * @return the guest id
     */
    public long getGuestId() {
        return guestId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostId, guestId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HostGuest other)) {
            return false;
        }
        return Objects.equals(hostId, other.getHostId())
                && Objects.equals(guestId, other.getGuestId());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", HostGuest.class.getSimpleName() + "[", "]")
                .add("hostId=" + hostId)
                .add("guestId=" + guestId)
                .toString();
    }
}
