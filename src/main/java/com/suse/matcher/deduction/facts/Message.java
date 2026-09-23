package com.suse.matcher.deduction.facts;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents a message to be presented to the user, as a secondary informative output (errors, warnings, etc.)
 */
public class Message implements Comparable<Message> {

    private static final Comparator<Message> DEFAULT_COMPARATOR = Comparator.comparing(Message::getType)
            .thenComparing(Message::getData, (o1In, o2In) -> {
                Comparator<String> nullSafeComparator = Comparator.nullsFirst(Comparator.naturalOrder());
                return o1In.keySet().stream().sorted()
                    .map(key -> nullSafeComparator.compare(o1In.get(key), o2In.get(key)))
                    .filter(result -> result != 0)
                    .findFirst()
                    .orElse(0);
            });

    /**
     * Represents the severity level of this message.
     */
    public enum Level {
        /** Relevant for debugging, not to be shown to users in general. */
        DEBUG,
        /** Information for users, normal functioning. */
        INFO,
        /** Warnings about things that went wrong but still don't block the matcher. */
        WARNING
    }

    /** The severity level of this message. */
    private final Level severity;

    /** A label identifying the message type. */
    private final String type;

    /** Arbitrary data connected to this message. */
    private final Map<String, String> data;

    /**
     * Instantiates a new message.
     *
     * @param severityIn the severity
     * @param typeIn the type
     * @param dataIn the data
     */
    public Message(Level severityIn, String typeIn, Map<String, String> dataIn) {
        severity = severityIn;
        type = typeIn;
        data = dataIn;
    }

    /**
     * Gets the message severity.
     * @return the severity
     */
    public Level getSeverity() {
        return severity;
    }

    /**
     * Gets the message type.
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the data map.
     * @return the data map
     */
    public Map<String, String> getData() {
        return data;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Message other)) {
            return false;
        }
        return Objects.equals(type, other.getType())
                && Objects.equals(data, other.getData());
    }

    @Override
    public int compareTo(Message other) {
        return DEFAULT_COMPARATOR.compare(this, other);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Message.class.getSimpleName() + "[", "]")
                .add("type=" + type)
                .add("data=" + data)
                .toString();
    }
}
