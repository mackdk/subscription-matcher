package com.suse.matcher.io;

import com.suse.matcher.Matcher;
import com.suse.matcher.deduction.FactConverter;
import com.suse.matcher.deduction.facts.InstalledProduct;
import com.suse.matcher.deduction.facts.Message;
import com.suse.matcher.deduction.facts.Product;
import com.suse.matcher.deduction.facts.Subscription;
import com.suse.matcher.deduction.facts.System;
import com.suse.matcher.deduction.facts.Timestamp;
import com.suse.matcher.io.csv.CSVOutputMessage;
import com.suse.matcher.io.csv.CSVOutputSubscription;
import com.suse.matcher.io.csv.CSVOutputUnmatchedProduct;
import com.suse.matcher.io.json.JsonMatch;
import com.suse.matcher.optimization.Assignment;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Writes output (to disk or standard output).
 */
public class OutputWriter {

    private static final Logger LOGGER = LogManager.getLogger(OutputWriter.class);

    // filenames
    private static final String JSON_INPUT_FILE = "input.json";
    private static final String JSON_OUTPUT_FILE = "output.json";
    private static final String JSON_OUTPUT_ALL_FILE = "output-all.json";
    private static final String CSV_SUBSCRIPTION_REPORT_FILE = "subscription_report.csv";
    private static final String CSV_UNMATCHED_PRODUCT_REPORT_FILE = "unmatched_product_report.csv";
    private static final String CSV_MESSAGE_REPORT_FILE = "message_report.csv";

    /** The output directory. */
    private final Path outputDirectory;

    /** The CSV format. */
    private final CSVFormat baseFormat;

    /**
     * Instantiates a new writer.
     *
     * @param outputDirectoryIn an output directory path. If empty, current directory is used
     * as default
     * @param delimiter an optional CSV delimiter. If empty, comma is used as default
     */
    public OutputWriter(Path outputDirectoryIn, char delimiter) {
        outputDirectory = outputDirectoryIn;
        baseFormat = CSVFormat.EXCEL.builder()
            .setDelimiter(delimiter)
            .build();
    }

    /**
     * Write the output files to the specified directory.
     *
     * @param assignment output from {@link Matcher}
     * @param logLevel the logging level
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void writeOutput(Assignment assignment, Optional<Level> logLevel) throws IOException {
        writeJsonOutput(assignment);
        writeCSVSubscriptionReport(assignment);
        writeCSVUnmatchedProductReport(assignment);
        writeCSVMessageReport(assignment);

        try {
            Files.deleteIfExists(outputDirectory.resolve(JSON_OUTPUT_ALL_FILE));
        }
        catch (Exception ex) {
            LOGGER.error("Unable to delete file {} in directory {}: {}", JSON_OUTPUT_ALL_FILE, outputDirectory, ex.getMessage());
        }

        logLevel.filter(l -> l.isMoreSpecificThan(Level.DEBUG)).ifPresent(l -> writeAllFacts(assignment));
    }

    private void writeAllFacts(Assignment assignment) {
        try (PrintWriter writer = new PrintWriter(outputDirectory.resolve(JSON_OUTPUT_ALL_FILE).toFile())) {
            JsonIO io = new JsonIO();
            writer.write(io.toJson(assignment));
        }
        catch (FileNotFoundException e) {
            throw new IllegalStateException("Unable to write to facts to file", e);
        }
    }

    /**
     * Writes the raw input file in JSON format.
     *
     * @param input the input
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void writeJsonInput(String input) throws IOException {
        Files.writeString(outputDirectory.resolve(JSON_INPUT_FILE), input, Charset.defaultCharset());
    }

    /**
     * Writes the raw output file in JSON format.
     *
     * @param assignment output from {@link Matcher}
     * @throws FileNotFoundException if the output directory was not found
     */
    public void writeJsonOutput(Assignment assignment) throws FileNotFoundException {
        try (PrintWriter writer = new PrintWriter(outputDirectory.resolve(JSON_OUTPUT_FILE).toFile())) {
            JsonIO io = new JsonIO();
            writer.write(io.toJson(FactConverter.convertToOutput(assignment)));
        }
    }

    /**
     * Writes the CSV subscription report.
     *
     * @param assignment output from {@link Matcher}
     * @throws IOException if an I/O error occurs
     */
    public void writeCSVSubscriptionReport(Assignment assignment) throws IOException {
        Date timestamp = assignment.getProblemFactStream(Timestamp.class).findFirst()
            .map(Timestamp::getTimestamp)
            .orElse(new Date());

        Comparator<Subscription> activeSubsFirst = (s1, s2) -> {
            int s1Active = timestamp.after(s1.getStartDate()) && timestamp.before(s1.getEndDate()) ? 0 : 1;
            int s2Active = timestamp.after(s2.getStartDate()) && timestamp.before(s2.getEndDate()) ? 0 : 1;
            return s1Active - s2Active;
        };

        Stream<Subscription> subscriptions = assignment.getProblemFactStream(Subscription.class)
            .filter(s -> s.getPolicy() != null)
            .filter(s -> s.getStartDate() != null && s.getEndDate() != null)
            .filter(s -> s.getQuantity() != null && s.getQuantity() > 0)
            .sorted(activeSubsFirst.thenComparing(s -> s.getPartNumber()));

        Map<Long, CSVOutputSubscription> outsubs = new LinkedHashMap<>();
        subscriptions.forEach(s -> {
            CSVOutputSubscription csvs = new CSVOutputSubscription(
                s.getPartNumber(),
                s.getName(),
                s.getPolicy().toString(),
                s.getQuantity(),
                s.getStartDate(),
                s.getEndDate()
            );
            outsubs.put(s.getId(), csvs);
        });

        // compute cents by subscription id
        Map<Long, Integer> matchedCents = new HashMap<>();
        FactConverter.getMatches(assignment)
            .forEach(m -> matchedCents.merge(m.getSubscriptionId(), m.getCents(), Math::addExact));

        // update output
        matchedCents.forEach((subscriptionId, cents) -> {
            if (outsubs.containsKey(subscriptionId)) {
                // convert from cents to count
                // we want the potential matches (e.g. only 20 cents of a
                // subscription is used) to be counted as an used subscription
                // see http://www.cs.nott.ac.uk/~psarb2/G51MPC/slides/NumberLogic.pdf
                outsubs.get(subscriptionId).setMatched((cents + 100 - 1) / 100);
            }
        });

        // prepare the format
        CSVFormat csvFormat = baseFormat.builder()
            .setHeader(CSVOutputSubscription.getHeaders())
            .build();

        // write CSV file
        try (FileWriter writer = new FileWriter(outputDirectory.resolve(CSV_SUBSCRIPTION_REPORT_FILE).toFile());
            CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
            for (Map.Entry<Long, CSVOutputSubscription> item : outsubs.entrySet()) {
                printer.printRecord(item.getValue().getCSVRow());
            }
        }
    }

    /**
     * Writes the CSV report of unmatched products and corresponding systems.
     *
     * @param assignment output from {@link Matcher}
     * @throws IOException if an I/O error occurs
     */
    public void writeCSVUnmatchedProductReport(Assignment assignment) throws IOException {
        Collection<JsonMatch> confirmedMatchFacts = FactConverter.getMatches(assignment);

        List<System> systems = assignment.getProblemFactStream(System.class)
                .sorted(Comparator.comparing(a -> a.getId()))
                .collect(Collectors.toList());

        // prepare map from (system id, product id) to Match object
        Map<Pair<Long, Long>, JsonMatch> matchMap = new HashMap<>();
        for (JsonMatch match : confirmedMatchFacts) {
            matchMap.put(Pair.of(match.getSystemId(), match.getProductId()), match);
        }

        // prepare the format
        CSVFormat csvFormat = baseFormat.builder()
            .setHeader(CSVOutputUnmatchedProduct.getHeaders())
            .build();

        // write CSV file
        try (FileWriter writer = new FileWriter(outputDirectory.resolve(CSV_UNMATCHED_PRODUCT_REPORT_FILE).toFile());
             CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
            // create map of product id -> set of systems ids with this product and filter out successful matches
            Map<Long, Set<Long>> unmatchedProductSystems = assignment.getProblemFactStream(InstalledProduct.class)
                    .filter(sp -> matchMap.get(Pair.of(sp.getSystemId(), sp.getProductId())) == null)
                    .collect(Collectors.groupingBy(
                        InstalledProduct::getProductId,
                        Collectors.mapping(InstalledProduct::getSystemId, Collectors.toSet())
                    ));

            List<CSVOutputUnmatchedProduct> unmatchedProductsCsvs = unmatchedProductSystems.entrySet().stream()
                    .map(e -> new CSVOutputUnmatchedProduct(
                            productNameById(assignment, e.getKey()),
                            e.getValue().stream().flatMap(sid -> systemById(systems, sid).stream()).collect(Collectors.toList())
                    ))
                    .collect(Collectors.toList());

            // cant use java 8 forEach as printer throws a checked exception
            for (CSVOutputUnmatchedProduct csv : unmatchedProductsCsvs) {
                csv.getUnmatchedSystems().sort((Comparator.comparing(s -> Objects.requireNonNullElse(s.getName(), ""))));
                printer.printRecords(csv.getCSVRows());
            }
        }
    }

    private Optional<System> systemById(Collection<System> systems, Long systemId) {
        return systems.stream()
                .filter(s -> Objects.equals(systemId, s.getId()))
                .findFirst();
    }

    private String productNameById(Assignment assignment, Long productId) {
        return assignment.getProblemFactStream(Product.class)
                .filter(p -> p.getId().equals(productId))
                .map(p -> p.getName())
                .findFirst()
                .orElse("Unknown product (" + productId + ")");
    }

    /**
     * Writes the CSV message report.
     *
     * @param assignment output from {@link Matcher}
     * @throws IOException if an I/O error occurs
     */
    public void writeCSVMessageReport(Assignment assignment) throws IOException {
        // prepare the format
        CSVFormat csvFormat = baseFormat.builder()
            .setHeader(CSVOutputMessage.getHeaders())
            .build();

        // write CSV file
        try (FileWriter writer = new FileWriter(outputDirectory.resolve(CSV_MESSAGE_REPORT_FILE).toFile());
                CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {

            List<Message> messages = assignment.getProblemFactStream(Message.class)
                .filter(m -> m.getSeverity() != Message.Level.DEBUG)
                .sorted()
                .collect(Collectors.toList());

            for (Message message: messages) {
                CSVOutputMessage csvMessage = new CSVOutputMessage(message.getType(), message.getData());
                printer.printRecords(csvMessage.getCSVRows());
            }
        }
    }

}