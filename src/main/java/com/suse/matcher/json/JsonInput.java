package com.suse.matcher.json;

import java.util.Date;
import java.util.List;

/**
 * JSON representation of the matcher's input.
 * 
 * @param timestamp date and time of the match (as it influences subscriptions)
 * @param systems the systems
 * @param virtualizationGroups groups of virtual guests
 * @param products the products
 * @param subscriptions the subscriptions
 * @param pinnedMatches the matches pinned by the user
 */
public record JsonInput (
    Date timestamp,
    List<JsonSystem> systems,
    List<JsonVirtualizationGroup> virtualizationGroups,
    List<JsonProduct> products,
    List<JsonSubscription> subscriptions,
    List<JsonMatch> pinnedMatches
) {
}
