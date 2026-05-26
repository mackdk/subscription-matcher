package com.suse.matcher.csv;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * A subscription as represented in a CSV output file.
 * @param partNumber the part number
 * @param name the subscription name
 * @param policy the subscription policy
 * @param quantity the quantity
 * @param startDate the start date
 * @param endDate the end date
 * @param matched number of subscriptions matched.
 */
public record CSVOutputSubscription(String partNumber, String name, String policy, Integer quantity, Date startDate, Date endDate, int matched) {

    /** Header for the CSV output. */
    public static String[] getHeaders() {
        return new String[]{"Part Number", "Description", "Policy", "Total Quantity", "Matched Quantity", "Start Date", "End Date"};
    }

    /**
     * Gets the CSV row.
     * @return the CSV row
     */
    public List<String> getCSVRow() {
        List<String> row = new ArrayList<>();
        row.add(partNumber);
        row.add(name);
        row.add(policy);
        row.add(String.valueOf(quantity));
        row.add(String.valueOf(matched));
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (startDate != null) {
            row.add(df.format(startDate));
        }
        if (endDate != null) {
            row.add(df.format(endDate));
        }
        return row;
    }
}
