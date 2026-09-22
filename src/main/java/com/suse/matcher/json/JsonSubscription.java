package com.suse.matcher.json;

import java.util.Date;
import java.util.Set;

/**
 * JSON representation of a subscription.
 * @param id the subscription id
 * @param partNumber the part number
 * @param name the friendly name
 * @param quantity the number of available subscriptions
 * @param startDate the start date
 * @param endDate the end date
 * @param sccUsername the name of the user owning the subscription
 * @param productIds the product ids associated with this subscription
 */
public record JsonSubscription(
    Long id,
    String partNumber,
    String name,
    Integer quantity,
    Date startDate,
    Date endDate,
    String sccUsername,
    Set<Long> productIds
) {
}
