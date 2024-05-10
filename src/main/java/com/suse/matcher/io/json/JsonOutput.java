package com.suse.matcher.io.json;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JSON representation of the matcher's output.
 * 
 * @param timestamp date and time of the match
 * @param matches all matches, possible and confirmed
 * @param subscriptionPolicies mapping from subscription id to its policy
 * @param messages the messages
 * @param subscriptions the processed subscriptions
 */
public record JsonOutput(
    Date timestamp,
    List<JsonMatch> matches,
    Map<Long, String> subscriptionPolicies,
    List<JsonSubscription> subscriptions,
    List<JsonMessage> messages
) {
}
