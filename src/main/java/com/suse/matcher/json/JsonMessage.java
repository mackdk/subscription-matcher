package com.suse.matcher.json;

import java.util.Map;

/**
 * JSON representation of a user message generated during the match (error, warning, etc.).
 */
public class JsonMessage {

    /** A label identifying the message type. */
    private String type;

    /** Arbitrary data connected to this message. */
    private Map<String, String> data;

    /**
     * Standard constructor.
     *
     * @param typeIn the type
     * @param dataIn the data
     */
    public JsonMessage(String typeIn, Map<String, String> dataIn) {
        type = typeIn;
        data = dataIn;
    }

    /**
     * Gets the a label identifying the message type.
     *
     * @return the a label identifying the message type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the a label identifying the message type.
     *
     * @param typeIn the new a label identifying the message type
     */
    public void setType(String typeIn) {
        type = typeIn;
    }

    /**
     * Gets the data connected to this message.
     *
     * @return the data connected to this message
     */
    public Map<String, String> getData() {
        return data;
    }

    /**
     * Sets the data connected to this message.
     *
     * @param dataIn the new data connected to this message
     */
    public void setData(Map<String, String> dataIn) {
        data = dataIn;
    }
}
