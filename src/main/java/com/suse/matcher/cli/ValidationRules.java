package com.suse.matcher.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.logging.log4j.Level;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * Validation rules for the command line arguments
 */
enum ValidationRules {
    VALID_LOG_LEVEL(
        MatcherOptions.LOG_LEVEL,
        value -> Level.getLevel(value) != null,
        "Given log level is invalid"
    ),
    INPUT_FILE_EXISTS(
        MatcherOptions.INPUT_FILE,
        value -> Files.isReadable(Path.of(value)),
        "Given input file does not exist or is not readable"
    ),
    OUTPUT_DIRECTORY_EXISTS(
        MatcherOptions.OUTPUT_DIRECTORY,
        value -> Files.isDirectory(Path.of(value)),
        "Given output directory does not exist or is not a directory"
    ),
    LOG_DIRECTORY_EXISTS(
        MatcherOptions.LOG_DIRECTORY,
        value -> Files.isDirectory(Path.of(value)),
        "Given logging directory does not exist or is not a directory"
    );

    private final Option option;
    private final Predicate<String> validator;
    private final String errorMessage;

    ValidationRules(Option option, Predicate<String> validator, String errorMessage) {
        this.option = option;
        this.validator = validator;
        this.errorMessage = errorMessage;
    }

    /**
     * Validates the given command line according to this rule. If the rule is not applicable, it is considered valid.
     * @param commandLine the command line to validate
     * @throws ArgumentParseException if the validation fails
     */
    public void validate(CommandLine commandLine) throws ArgumentParseException {
        if (!commandLine.hasOption(option)) {
            return;
        }

        String value = commandLine.getOptionValue(option);
        if (!validator.test(value)) {
            throw new ArgumentParseException(errorMessage);
        }
    }

    /**
     * Validates the given command line according to all defined rules.
     * @param commandLine the command line to validate
     * @throws ArgumentParseException if the validation fails
     */
    public static void validateAll(CommandLine commandLine) throws ArgumentParseException {
        for (ValidationRules rule : ValidationRules.values()) {
            rule.validate(commandLine);
        }
    }
}
