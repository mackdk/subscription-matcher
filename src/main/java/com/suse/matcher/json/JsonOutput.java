package com.suse.matcher.json;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JSON representation of the matcher's output.
 */
public class JsonOutput {

    /** Date and time of the match. */
    private Date timestamp;

    /** All matches, possible and confirmed. */
    private List<JsonMatch> matches;

    /** Mapping from subscription id to its policy */
    private Map<Long, String> subscriptionPolicies;

    /** The messages. */
    private List<JsonMessage> messages;

    /** The processed subscriptions **/
    private List<JsonSubscription> subscriptions;

    /**
     * Standard constructor.
     *
     * @param timestampIn the timestamp
     * @param matchesIn the matches
     * @param messagesIn the messages
     * @param subscriptionPoliciesIn mapping from subscription id to its policy
     * @param subscriptionsIn subscriptions (processed by matcher)
     */
    public JsonOutput(Date timestampIn, List<JsonMatch> matchesIn, List<JsonMessage> messagesIn,
            Map<Long, String> subscriptionPoliciesIn, List<JsonSubscription> subscriptionsIn) {
        timestamp = timestampIn;
        matches = matchesIn;
        messages = messagesIn;
        subscriptionPolicies = subscriptionPoliciesIn;
        subscriptions = subscriptionsIn;
    }

    /**
     * Gets the date and time of the match.
     *
     * @return the date and time of the match
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the date and time of the match.
     *
     * @param timestampIn the new date and time of the match
     */
    public void setTimestamp(Date timestampIn) {
        timestamp = timestampIn;
    }

    /**
     * Gets all possible and confirmed matches.
     *
     * @return the matches
     */
    public List<JsonMatch> getMatches() {
        return matches;
    }

    /**
     * Sets the matches.
     *
     * @param matchesIn the new matches
     */
    public void setMatches(List<JsonMatch> matchesIn) {
        matches = matchesIn;
    }

    /**
     * Gets the subscription to policy mapping.
     * @return the subscription to policy mapping.
     */
    public Map<Long, String> getSubscriptionPolicies() {
        return subscriptionPolicies;
    }

    /**
     * Sets the subscription to policy mapping.
     * @param subscriptionPoliciesIn subscription to policy map
     */
    public void setSubscriptionPolicies(Map<Long, String> subscriptionPoliciesIn) {
        this.subscriptionPolicies = subscriptionPoliciesIn;
    }

    /**
     * Gets the messages.
     *
     * @return the messages
     */
    public List<JsonMessage> getMessages() {
        return messages;
    }

    /**
     * Sets the messages.
     *
     * @param messagesIn the new messages
     */
    public void setMessages(List<JsonMessage> messagesIn) {
        messages = messagesIn;
    }

    /**
     * Gets the subscriptions.
     *
     * @return subscriptions
     */
    public List<JsonSubscription> getSubscriptions() {
        return subscriptions;
    }

    /**
     * Sets the subscriptions.
     *
     * @param subscriptions the subscriptions
     */
    public void setSubscriptions(List<JsonSubscription> subscriptions) {
        this.subscriptions = subscriptions;
    }
}
