package com.suse.matcher.json;

import java.util.Map;

/**
 * JSON representation of a user message generated during the match (error, warning, etc.).
 * 
 * @param type a label identifying the message type
 * @param data arbitrary data connected to this message
 */
public record JsonMessage(String type, Map<String, String> data) {
}
