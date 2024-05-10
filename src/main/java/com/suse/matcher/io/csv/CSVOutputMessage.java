package com.suse.matcher.io.csv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A message from the matcher as represented in a CSV output file.
 * @param type a label identifying the message type
 * @param data arbitrary data connected to this message.
 */
public record CSVOutputMessage(String type, Map<String, String> data) {

    /** Header for the CSV output. */
    public static String[] getHeaders() {
        return new String[]{"Message", "Additional data key", "Additional data value"};
    }

    /**
     * Gets the CSV rows.
     *
     * @return rows for the CSV output
     */
    public List<List<String>> getCSVRows() {
        List<List<String>> resultSet = new ArrayList<>();

        List<String> row = new ArrayList<>();
        row.add(type);

        for (Map.Entry<String, String> item : data.entrySet()) {
            row.add(item.getKey());
            row.add(item.getValue());
            resultSet.add(row);
            row = new ArrayList<>();
            row.add("");
        }
        return resultSet;
    }
}
