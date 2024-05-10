package com.suse.matcher.io.cli;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Describes the input arguments specified by the user on the command-line
 */
public interface Arguments {

    /**
     * Check if the user requested for help.
     * @return true if the user asked for help passing the appropriate parameter.
     */
    boolean isHelpRequest();

    /**
     * Retrieves an input stream for processing the input data.
     * @return the input stream of the data specified by the user.
     */
    InputStream getInputStream() throws IOException;

    /**
     * Retrieves the output directory.
     * @return the output directory.
     */
    Path getOutputDirectory();

    /**
     * Retrieves the log directory.
     * @return the log directory, if present.
     */
    Optional<Path> getLoggingDirectory();

    /**
     * Retrieves the log level.
     * @return the log level, if present.
     */
    Optional<Level> getLoggingLevel();


    /**
     * The delimiter used for generating CSV files.
     * @return the delimiter character used for CSV files.
     */
    char getDelimiter();
}
