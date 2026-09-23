package com.suse.matcher.deduction.facts;

import org.kie.api.definition.type.PropertyReactive;

import java.util.Date;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Encapsulates the date and time when the matching is performed. This wrapper
 * class is needed because we might want to simulate a match in the past
 * (typically for testing and debugging) or future (simulations).
 */
@PropertyReactive
public class Timestamp {

    /** Current timestamp. */
    private final Date timestamp;

    /**
     * Instantiates a new current time.
     *
     * @param timestampIn the timestamp
     */
    public Timestamp(Date timestampIn) {
        timestamp = timestampIn;
    }

    /**
     * Gets the timestamp.
     *
     * @return the timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Timestamp other)) {
            return false;
        }
        return Objects.equals(timestamp, other.getTimestamp());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Timestamp.class.getSimpleName() + "[", "]")
                .add("timestamp=" + timestamp)
                .toString();
    }
}
