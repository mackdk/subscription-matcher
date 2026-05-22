/*
 * Copyright (c) 2026 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 */
package com.suse.matcher;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.suse.matcher.json.JsonInput;
import com.suse.matcher.json.JsonOutput;
import com.suse.matcher.json.JsonSubscription;
import com.suse.matcher.json.JsonSystem;
import com.suse.matcher.json.JsonVirtualizationGroup;

import org.opentest4j.MultipleFailuresError;

import java.util.Objects;

/**
 * Utility class to validate the input and output of the scenario tests, ensuring the data
 * included match the expected test patterns and that the size of the input is reasonable
 * for a test.
 */
public class ScenarioValidator {

    private ScenarioValidator() {
        // Private constructor to prevent instantiation
    }

    /**
     * Validate the json input for the scenario, ensuring the data included match the
     * expected test patterns and that the size of the input is reasonable for a test.
     * @param jsonInput the JSON input to validate
     */
    public static void validateInput(JsonInput jsonInput) {
        assertNotNull(jsonInput, "The JSON input for the scenario cannot be null");
        
        assertAll("Missing required data in the input",
            () -> assertNotNull(jsonInput.getSystems(), "systems must not be null"),
            () -> assertNotNull(jsonInput.getProducts(), "products must not be null"),
            () -> assertNotNull(jsonInput.getSubscriptions(), "subscriptions must not be null"),
            () -> assertNotNull(jsonInput.getVirtualizationGroups(), "virtualization groups must not be null"),
            () -> assertNotNull(jsonInput.getPinnedMatches(), "pinned matches must not be null")
        );

        // Check that the input does not contain more than 100 systems, products and subscriptions to ensure the tests
        // runs in a reasonable time
        assertAll(
            () -> assertTrue(jsonInput.getSystems().size() <= 100,
                "The number of systems in the input is too high for a test scenario"),
            () -> assertTrue(jsonInput.getProducts().size() <= 100,
                "The number of products in the input is too high for a test scenario"),
            () -> assertTrue(jsonInput.getSubscriptions().size() <= 100,
                "The number of subscriptions in the input is too high for a test scenario")
        );

        // Ensure we don't have any system names or they match the "fake" pattern
        jsonInput.getSystems().stream()
            .map(JsonSystem::getName)
            .filter(Objects::nonNull)
            .forEach(systemName -> assertTrue(systemName.matches("sys-(\\d){3}\\.test\\.local"),
                "System name is not null and does not match the expected pattern: " + systemName));

        // Same for virtualization group names, if any
        jsonInput.getVirtualizationGroups().stream()
            .map(JsonVirtualizationGroup::getName)
            .filter(Objects::nonNull)
            .forEach(groupName -> assertTrue(groupName.matches("sys-(\\d){3}\\.test\\.local"),
                "Virtualization group name is not null and does not match the expected pattern: " + groupName));

        jsonInput.getSubscriptions().forEach(subscription -> assertCorrect(subscription));

        jsonInput.getPinnedMatches().forEach(match -> {
            assertSubscriptionIdInRange(match.getSubscriptionId());
        });
    }

    /**
     * Validate the json output for the scenario, checking that no sensitive data is included.
     * @param jsonOutput the JSON output to validate
     */
    public static void validateOutput(JsonOutput jsonOutput) {
        assertNotNull(jsonOutput, "The JSON output for the scenario cannot be null");

        assertAll("Missing required data in the output",
            () -> assertNotNull(jsonOutput.getSubscriptions(), "subscriptions must not be null"),
            () -> assertNotNull(jsonOutput.getMatches(), "matches must not be null"),
            () -> assertNotNull(jsonOutput.getSubscriptionPolicies(), "subscription policies must not be null"),
            () -> assertNotNull(jsonOutput.getMessages(), "messages must not be null")
        );

        jsonOutput.getSubscriptions().forEach(subscription -> assertCorrect(subscription));
        jsonOutput.getSubscriptionPolicies().forEach((subscriptionId, policy) -> {
            assertSubscriptionIdInRange(subscriptionId);
        });

        jsonOutput.getMatches().forEach(match -> {
            assertSubscriptionIdInRange(match.getSubscriptionId());
        });
    }

    private static void assertCorrect(JsonSubscription subscription) throws MultipleFailuresError {
        assertAll(
            () -> assertSubscriptionIdInRange(subscription.getId()),
            () -> assertTestUsername(subscription.getSccUsername())
        );
    }

    private static void assertTestUsername(String sccUsername) {
        if (sccUsername == null) {
            return;
        }
        assertTrue(sccUsername.toLowerCase().matches("^test-user-[a-z0-9]{6}$"),
            "SCC username does not match the expected pattern: " + sccUsername);
    }

    private static void assertSubscriptionIdInRange(Long subscriptionId) {
        if (subscriptionId == null) {
            return;
        }

        assertTrue(subscriptionId >= -100 && subscriptionId <= 100,
            "Subscription ID is out of range for a test scenario: " + subscriptionId);
    }
}
