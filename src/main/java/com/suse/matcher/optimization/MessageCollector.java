package com.suse.matcher.optimization;

import com.suse.matcher.deduction.FactConverter;
import com.suse.matcher.deduction.facts.Message;
import com.suse.matcher.deduction.facts.PinnedMatch;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
        Set<Pair<Long, Long>> confirmedMatchFacts = FactConverter.getMatches(assignment)
            .map(m -> Pair.of(m.getSubscriptionId(), m.getSystemId()))
            .collect(Collectors.toSet());

        // add messages about unsatisfied pins
        List<Message> messages = assignment.getProblemFactStream(PinnedMatch.class)
            .filter(pin -> !confirmedMatchFacts.contains(Pair.of(pin.getSubscriptionId(), pin.getSystemId())))
            .map(unmatchedPin -> {
                return new Message(Message.Level.INFO, "unsatisfied_pinned_match", new TreeMap<>(Map.of(
                    "system_id", String.valueOf(unmatchedPin.getSystemId()),
                    "subscription_id", String.valueOf(unmatchedPin.getSubscriptionId())
                )));
            })
            .toList();

        assignment.getProblemFacts().addAll(messages);
    }
}
