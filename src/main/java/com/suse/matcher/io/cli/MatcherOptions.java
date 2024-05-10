package com.suse.matcher.io.cli;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Class to define the command-line arguments for subscription-matcher
 */
class MatcherOptions {

    public static final Option HELP = Option.builder()
        .option("h")
        .longOpt("help")
        .desc("show this help")
        .build();

    public static final Option INPUT_FILE = Option.builder()
        .option("i")
        .longOpt("input")
        .hasArg()
        .desc("input.json file (Default: standard input)")
        .build();

    public static final Option OUTPUT_DIRECTORY = Option.builder()
        .option("o")
        .longOpt("output-directory")
        .hasArg()
        .desc("Output directory (Default: current directory)")
        .build();

    public static final Option LOG_DIRECTORY = Option.builder()
        .option("l")
        .longOpt("log-directory")
        .hasArg()
        .desc("Logging directory (Default: none, only log via STDERR)")
        .build();

    public static final Option LOG_LEVEL = Option.builder()
        .option("v")
        .longOpt("log-level")
        .hasArg()
        .desc("Log level (Default: INFO, Possible values: OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL)")
        .build();

    public static final Option DELIMITER = Option.builder()
        .option("d")
        .longOpt("delimiter")
        .hasArg()
        .desc("CSV Delimiter (Default: ,)")
        .build();

    private static final Options OPTIONS = new Options()
            .addOption(HELP)
            .addOption(INPUT_FILE)
            .addOption(OUTPUT_DIRECTORY)
            .addOption(LOG_DIRECTORY)
            .addOption(LOG_LEVEL)
            .addOption(DELIMITER);

    public static Options getOptions() {
        return OPTIONS;
    }

    private MatcherOptions() {
        // Prevent instantiation
    }
}
