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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Writes output (to disk or standard output).
 */
public class OutputWriter {

    private static final Logger LOGGER = LogManager.getLogger(OutputWriter.class);

    private static final Comparator<System> SYSTEMS_BY_NAME = Comparator.comparing(s -> Objects.requireNonNullElse(s.getName(), ""));

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

        // convert from match cents to count
        Map<Long, Integer> matchedCounts = FactConverter.getMatches(assignment)
            .collect(Collectors.groupingBy(
                JsonMatch::getSubscriptionId,
                Collectors.collectingAndThen(
                    // Start at 0, map each match to its cents, and sum using addExact
                    Collectors.reducing(0, JsonMatch::getCents, Math::addExact),
                    // We want to count a subscription as used even if only a part of it is used.
                    // So we round up the cents to the next full subscription.
                    // see http://www.cs.nott.ac.uk/~psarb2/G51MPC/slides/NumberLogic.pdf
                    cents -> (cents + 99) / 100
                )
            ));

        List<CSVOutputSubscription> subscriptions = assignment.getProblemFactStream(Subscription.class)
            .filter(s -> s.getPolicy() != null)
            .filter(s -> s.getStartDate() != null && s.getEndDate() != null)
            .filter(s -> s.getQuantity() != null && s.getQuantity() > 0)
            .sorted(activeSubsFirst.thenComparing(s -> s.getPartNumber()))
            .map(s -> new CSVOutputSubscription(
                s.getPartNumber(),
                s.getName(),
                s.getPolicy().toString(),
                s.getQuantity(),
                s.getStartDate(),
                s.getEndDate(),
                matchedCounts.getOrDefault(s.getId(), 0)
            ))
            .toList();

        // prepare the format
        CSVFormat csvFormat = baseFormat.builder()
            .setHeader(CSVOutputSubscription.getHeaders())
            .build();

        // write CSV file
        try (FileWriter writer = new FileWriter(outputDirectory.resolve(CSV_SUBSCRIPTION_REPORT_FILE).toFile());
            CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
            for (CSVOutputSubscription csv : subscriptions) {
                printer.printRecord(csv.getCSVRow());
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
        Map<Long, System> systemsMap = assignment.getProblemFactStream(System.class)
                .collect(Collectors.toMap(System::getId, Function.identity()));

        Map<Long, Product> productsMap = assignment.getProblemFactStream(Product.class)
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // prepare map from (system id, product id) to Match object
        Map<Pair<Long, Long>, JsonMatch> matchMap = new HashMap<>();
        FactConverter.getMatches(assignment)
            .forEach(match -> matchMap.put(Pair.of(match.getSystemId(), match.getProductId()), match));

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
                    .map(e -> {
                        String productName = getProductNameById(e.getKey(), productsMap);
                        List<System> unmatchedSystems = getUnmatchedSystems(e.getValue(), systemsMap);

                        return new CSVOutputUnmatchedProduct(productName, unmatchedSystems);
                    })
                    .toList();

            for (CSVOutputUnmatchedProduct csv : unmatchedProductsCsvs) {
                printer.printRecords(csv.getCSVRows());
            }
        }
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
                .toList();

            for (Message message: messages) {
                CSVOutputMessage csvMessage = new CSVOutputMessage(message.getType(), message.getData());
                printer.printRecords(csvMessage.getCSVRows());
            }
        }
    }

    private static List<System> getUnmatchedSystems(Set<Long> systemIds, Map<Long, System> systemsMap) {
        return systemIds.stream()
            .flatMap(sid -> Optional.ofNullable(systemsMap.get(sid)).stream())
            .sorted(SYSTEMS_BY_NAME)
            .toList();
    }

    private static String getProductNameById(Long productId, Map<Long, Product> productsMap) {
        return Optional.ofNullable(productsMap.get(productId))
                .map(Product::getName)
                .orElse("Unknown product (" + productId + ")");
    }

}
