package com.suse.matcher.deduction;

import com.suse.matcher.deduction.facts.CentGroup;
import com.suse.matcher.deduction.facts.HostGuest;
import com.suse.matcher.deduction.facts.InstalledProduct;
import com.suse.matcher.deduction.facts.Message;
import com.suse.matcher.deduction.facts.PinnedMatch;
import com.suse.matcher.deduction.facts.PotentialMatch;
import com.suse.matcher.deduction.facts.Product;
import com.suse.matcher.deduction.facts.Subscription;
import com.suse.matcher.deduction.facts.SubscriptionProduct;
import com.suse.matcher.deduction.facts.System;
import com.suse.matcher.deduction.facts.Timestamp;
import com.suse.matcher.deduction.facts.VirtualizationGroupMember;
import com.suse.matcher.io.json.JsonInput;
import com.suse.matcher.io.json.JsonMatch;
import com.suse.matcher.io.json.JsonMessage;
import com.suse.matcher.io.json.JsonOutput;
import com.suse.matcher.io.json.JsonProduct;
import com.suse.matcher.io.json.JsonSubscription;
import com.suse.matcher.io.json.JsonSystem;
import com.suse.matcher.io.json.JsonVirtualizationGroup;
import com.suse.matcher.optimization.Assignment;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * Converts JSON objects to facts (objects the rule engine and CSP solver can
 * reason about) and vice versa.
 */
public class FactConverter {

    private FactConverter() {
        // Prevent instantiation
    }

    /**
     * Converts JSON objects to facts (inputs to the rule engine).
     *
     * @param input a JSON input data blob
     * @return a collection of facts
     */
    public static Collection<Object> convertToFacts(JsonInput input) {
        Collection<Object> result = new LinkedList<>();

        result.add(new Timestamp(input.getTimestamp()));

        for (JsonSystem system : input.getSystems()) {
            result.add(new System(system.getId(), system.getName(), system.getCpus(), system.getPhysical()));
            for (Long guestId : system.getVirtualSystemIds()) {
                result.add(new HostGuest(system.getId(), guestId));
            }
            for (Long productId : system.getProductIds()) {
                result.add(new InstalledProduct(system.getId(), productId));
            }
        }

        for (JsonVirtualizationGroup group : input.getVirtualizationGroups()) {
            for (Long guestId : group.getVirtualGuestIds()) {
                result.add(new VirtualizationGroupMember(
                        Drools.generateId(group.getType(), group.getId()),
                        guestId));
            }
        }

        for (JsonProduct product : input.getProducts()) {
            result.add(new Product(
                    product.getId(),
                    product.getName(),
                    product.getProductClass(),
                    BooleanUtils.isTrue(product.getFree()),
                    BooleanUtils.isTrue(product.getBase())));
        }

        for (JsonSubscription subscription : input.getSubscriptions()) {
            result.add(new Subscription(
                    subscription.getId(),
                    subscription.getPartNumber(),
                    subscription.getName(),
                    subscription.getQuantity(),
                    subscription.getStartDate(),
                    subscription.getEndDate(),
                    subscription.getSccUsername()
            ));
            for (Long productId : subscription.getProductIds()) {
                result.add(new SubscriptionProduct(subscription.getId(), productId));
            }
        }

        for (JsonMatch match : input.getPinnedMatches()) {
            result.add(new PinnedMatch(match.getSystemId(), match.getSubscriptionId()));
        }

        return result;
    }

    /**
     * Converts the outputs from CSP solver in output JSON format.
     *
     * @param assignment the assignment object, output of the CSP solver
     * @return the output
     */
    public static JsonOutput convertToOutput(Assignment assignment) {
        Date timestamp = assignment.getProblemFactStream(Timestamp.class).findFirst()
            .map(Timestamp::getTimestamp)
            .orElse(new Date());

        List<JsonMatch> matches = getMatches(assignment);

        List<JsonMessage> messages = assignment.getProblemFactStream(Message.class)
                .sorted()
                .map(m -> new JsonMessage(
                        m.getType(),
                        m.getData()
                ))
                .collect(Collectors.toList());

        Map<Long, String> subscriptionPolicies = assignment
                .getProblemFactStream(Subscription.class)
                .filter(s -> s.getPolicy() != null)
                .sorted()
                .collect(Collectors.toMap(
                        s -> s.getId(),
                        s -> s.getPolicy().name().toLowerCase(),
                        throwingMerger(),
                        LinkedHashMap::new));

        return new JsonOutput(timestamp, matches, messages, subscriptionPolicies, getSubscriptions(assignment));
    }

    private static BinaryOperator<String> throwingMerger() {
        return (u,v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }

    /**
     * Returns a list of {@link JsonMatch}es from the {@link Assignment}.
     * @param assignment the assignment
     * @return matches
     */
    public static List<JsonMatch> getMatches(Assignment assignment) {
        Set<Integer> confirmedGroupIds = assignment.getMatches().stream()
                .filter(m -> m.isConfirmed())
                .map(m -> m.getId())
                .collect(Collectors.toSet());

        // map of cent group id -> cent group cents
        Map<Number, Integer> centGroupsCents = assignment.getProblemFactStream(CentGroup.class)
                .collect(Collectors.toMap(
                        cg -> cg.getId(),
                        cg -> cg.getCents()
                ));

        // how many confirmed Potential Matches share one Cent Group
        Map<Integer, Integer> centGroupMatchesCount = assignment.getProblemFactStream(PotentialMatch.class)
                .filter(pm -> confirmedGroupIds.contains(pm.getGroupId()))
                .collect(Collectors.toMap(
                        pm -> pm.getCentGroupId(),
                        pm -> 1,
                        (v1, v2) -> v1 + v2
                ));

        return assignment.getProblemFactStream(PotentialMatch.class)
            .filter(m -> confirmedGroupIds.contains(m.getGroupId())) // only confirmed matches
            .map(m -> new JsonMatch(
                m.getSystemId(),
                m.getSubscriptionId(),
                m.getProductId(),
                centGroupsCents.get(m.getCentGroupId()) / centGroupMatchesCount.getOrDefault(m.getCentGroupId(), 1)
            ))
            .sorted((a, b) -> new CompareToBuilder()
                .append(a.getSystemId(), b.getSystemId())
                .append(a.getProductId(), b.getProductId())
                .append(a.getSubscriptionId(), b.getSubscriptionId())
                .append(a.getCents(), b.getCents())
                .toComparison()
            )
            .collect(Collectors.toList());
    }

    /**
     * Processes the input subscriptions based on the matching results (e.g. merges subscriptions in hard bundles).
     *
     * @param assignment the solved assignment
     * @return the processed subscriptions
     */
    private static List<JsonSubscription> getSubscriptions(Assignment assignment) {
        Map<Long, Set<Long>> subProducts = assignment.getProblemFactStream(SubscriptionProduct.class)
                .collect(Collectors.groupingBy(
                    sp -> sp.getSubscriptionId(),
                    Collectors.mapping(sp -> sp.getProductId(), Collectors.toCollection(() -> new TreeSet<>()))
                ));
        return assignment.getProblemFactStream(Subscription.class)
                .sorted(Comparator.comparing(Subscription::getId))
                .map(s -> new JsonSubscription(
                    s.getId(),
                    s.getPartNumber(),
                    s.getName(),
                    s.getQuantity(),
                    s.getStartDate(),
                    s.getEndDate(),
                    s.getSccUsername(),
                    subProducts.get(s.getId())
                ))
                .collect(Collectors.toList());
    }
}