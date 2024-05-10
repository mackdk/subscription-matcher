package com.suse.matcher.io.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Default implementation for the application arguments
 */
public class CommandLineArguments implements Arguments {

    private final CommandLine commandLine;

    /**
     * Default constructor, parses the given command line
     * @param parameters the parameters received from the jvm
     * @throws ArgumentParseException if the given parameters are invalid
     */
    private CommandLineArguments(String[] parameters) throws ArgumentParseException {
        CommandLineParser parser = new DefaultParser();
        try {
            commandLine = parser.parse(MatcherOptions.getOptions(), parameters);
        } catch (ParseException e) {
            throw new ArgumentParseException(e.getMessage());
        }

        ValidationRules.validateAll(commandLine);
    }

    @Override
    public boolean isHelpRequest() {
        return commandLine.hasOption(MatcherOptions.HELP);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (!commandLine.hasOption(MatcherOptions.INPUT_FILE)) {
            return System.in;
        }

        return Files.newInputStream(Path.of(commandLine.getOptionValue(MatcherOptions.INPUT_FILE)));
    }

    @Override
    public Path getOutputDirectory() {
        return Optional.ofNullable(commandLine.getOptionValue(MatcherOptions.OUTPUT_DIRECTORY))
            .map(value -> Path.of(value))
            .orElseGet(() -> Path.of("."));
    }

    @Override
    public Optional<Path> getLoggingDirectory() {
        return Optional.ofNullable(commandLine.getOptionValue(MatcherOptions.LOG_DIRECTORY))
            .map(value -> Path.of(value));
    }

    @Override
    public Optional<Level> getLoggingLevel() {
        return Optional.ofNullable(commandLine.getOptionValue(MatcherOptions.LOG_LEVEL))
            .map(value -> Level.valueOf(value));
    }

    @Override
    public char getDelimiter() {
        return Optional.ofNullable(commandLine.getOptionValue(MatcherOptions.DELIMITER))
            .map(value -> value.charAt(0))
            .orElse(',');
    }

    /**
     * Parse the given command line
     * @param parameters the parameters received from the jvm
     * @return the parsed input {@link Arguments}
     */
    public static Arguments parseCommandLine(String[] parameters) {
        try {
            return new CommandLineArguments(parameters);
        }
        catch (ArgumentParseException ex) {
            throw new IllegalArgumentException("Unable to parse parameters", ex);
        }
    }

    /**
     * Print the help of the application on the standard output
     */
    public static void printHelpText() {
        new HelpFormatter().printHelp(
            "subscription-matcher",
            "options:",
            MatcherOptions.getOptions(),
            null,
            true
        );
    }
}
