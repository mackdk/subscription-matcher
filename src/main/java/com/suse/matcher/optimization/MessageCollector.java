package com.suse.matcher.optimization;

import com.suse.matcher.deduction.FactConverter;
import com.suse.matcher.deduction.facts.Message;
import com.suse.matcher.deduction.facts.PinnedMatch;
import com.suse.matcher.io.json.JsonMatch;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Generates Messages facts and adds them to an Assignment.
 */
public class MessageCollector {

    private MessageCollector() {
        // Prevent instantiation
    }

    /**
     * Takes an Assignment after OptaPlanner is done with it in order to add user message objects.
     *
     * @param assignment the assignment
     */
    public static void addMessages(Assignment assignment) {
        // filter out interesting collections from facts
        Stream<PinnedMatch> pinnedMatchFacts = assignment.getProblemFactStream(PinnedMatch.class);

        Collection<JsonMatch> confirmedMatchFacts = FactConverter.getMatches(assignment);

        // add messages about unsatisfied pins
        Collection<Message> messages = new LinkedList<>();
        pinnedMatchFacts
            .filter(pin -> confirmedMatchFacts.stream() // filter unmatched pins
                    .noneMatch(m -> Objects.equals(m.getSubscriptionId(), pin.getSubscriptionId()) &&
                        Objects.equals(m.getSystemId(), pin.getSystemId()))
            )
            .forEach(unmatchedPin -> {
                Message message = new Message(Message.Level.INFO, "unsatisfied_pinned_match", new TreeMap<>(Map.of(
                    "system_id", String.valueOf(unmatchedPin.getSystemId()),
                    "subscription_id", String.valueOf(unmatchedPin.getSubscriptionId())
                )));
                messages.add(message);
            });

        assignment.getProblemFacts().addAll(messages);
    }
}