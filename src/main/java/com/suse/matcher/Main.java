package com.suse.matcher;

import com.suse.matcher.io.JsonIO;
import com.suse.matcher.io.Log4J;
import com.suse.matcher.io.OutputWriter;
import com.suse.matcher.io.cli.Arguments;
import com.suse.matcher.io.cli.CommandLineArguments;
import com.suse.matcher.io.json.JsonInput;
import com.suse.matcher.optimization.Assignment;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Entry point for the command line version of this program.
 */
public class Main {

    /**
     * The main method.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        // Parse the command-line
        Arguments arguments = CommandLineArguments.parseCommandLine(args);
        if (arguments.isHelpRequest()) {
            CommandLineArguments.printHelpText();
            return;
        }

        // Initialize the logging system
        try (LoggerContext context = Log4J.initialize(arguments.getLoggingLevel(), arguments.getLoggingDirectory())) {
            Logger logger = context.getLogger(Main.class);
            logger.info("Starting subscription-matcher process");

            try {
                // create output writing objects
                OutputWriter writer = new OutputWriter(arguments.getOutputDirectory(), arguments.getDelimiter());

                // load input data
                String inputString = new String(arguments.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                // save a copy of input data in the output directory
                writer.writeJsonInput(inputString);

                // do the matching
                JsonInput input = new JsonIO().loadInput(inputString);
                Assignment assignment = new Matcher(false).match(input);

                // write output data
                writer.writeOutput(assignment, arguments.getLoggingLevel());

                logger.info("Whole execution took {}ms", System.currentTimeMillis() - start);
            }
            catch (IOException ex) {
                logger.error("Unexpected I/O error", ex);
                throw new UncheckedIOException(ex);
            }
            catch (RuntimeException ex) {
                logger.error("Unexpected error", ex);
                throw ex;
            }
        }
    }
}
