package com.suse.matcher.cli;

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

    public CommandLineArguments(String[] parameters) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        commandLine = parser.parse(MatcherOptions.getOptions(), parameters);

        if (commandLine.hasOption(MatcherOptions.LOG_LEVEL) &&
            Level.getLevel(commandLine.getOptionValue(MatcherOptions.LOG_LEVEL)) == null) {
            throw new ParseException("Given log level is invalid");
        }
        if (commandLine.hasOption(MatcherOptions.INPUT_FILE) &&
            !Files.isReadable(Path.of(commandLine.getOptionValue(MatcherOptions.INPUT_FILE)))) {
            throw new ParseException("Given input file does not exist or is not readable");
        }
        if (commandLine.hasOption(MatcherOptions.OUTPUT_DIRECTORY) &&
            !Files.isDirectory(Path.of(commandLine.getOptionValue(MatcherOptions.OUTPUT_DIRECTORY)))) {
            throw new ParseException("Given output directory does not exist or is not a directory");
        }
        if (commandLine.hasOption(MatcherOptions.LOG_DIRECTORY) &&
            !Files.isDirectory(Path.of(commandLine.getOptionValue(MatcherOptions.LOG_DIRECTORY)))) {
            throw new ParseException("Given logging directory does not exist or is not a directory");
        }
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
        catch (ParseException ex) {
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
