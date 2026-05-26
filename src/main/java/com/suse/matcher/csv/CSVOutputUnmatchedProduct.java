package com.suse.matcher.csv;

import com.suse.matcher.facts.System;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A unmatched product as represented in a CSV output file.
 * @param productName the product name
 * @param unmatchedSystems the unmatched systems corresponding to the product
 */
public record CSVOutputUnmatchedProduct(String productName, List<System> unmatchedSystems) {

    /**  Header for the CSV output. */
    public static String[] getHeaders() {
        return new String[]{"Unmatched Product Name", "System Name","System ID", "CPUs"};
    }

    /**
     * Gets the CSV rows.
     * @return the CSV rows
     */
    public List<List<String>> getCSVRows() {
        List<List<String>> resultSet = new LinkedList<>();

        List<String> row = new LinkedList<>();
        row.add(productName);

        for (System system : unmatchedSystems) {
            row.add(system.name);
            row.add(String.valueOf(system.id));
            row.add(String.valueOf(system.cpus));
            resultSet.add(row);
            row = new ArrayList<>();
            row.add("");
        }
        return resultSet;
    }
}
