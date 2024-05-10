package com.suse.matcher.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

class LoggingBootstrapTest {

    private PrintStream originalErr;

    private ByteArrayOutputStream capturedErr;

    @TempDir
    private Path loggingDirectory;

    private Path logFile;

    @BeforeEach
    void replaceStandardError() {
        originalErr = System.err;
        capturedErr = new ByteArrayOutputStream();

        System.setErr(new PrintStream(capturedErr, true, StandardCharsets.UTF_8));

        logFile = loggingDirectory.resolve("subscription-matcher.log");
    }

    @AfterEach
    void restoreStandardError() {
        System.setErr(originalErr);
    }

    @Test
    void shouldLogToConsoleByDefault() {
        try (LoggerContext context = LoggingBootstrap.initialize(Optional.empty(), Optional.empty())) {
            Logger logger = context.getLogger(LoggingBootstrapTest.class);

            logger.info("console-message");

            // Fake a logger from an internal library, which should have a different level
            Logger internalLogger = context.getLogger("org.drools.DummyTest");

            internalLogger.info("internal-message");
            internalLogger.error("internal-error");
        }

        // No file should be created by default
        assertFalse(Files.exists(logFile), "Expected log file NOT to be created");

        // Levels should remain those defined in the XML configuration
        assertLoggedToConsole(Level.INFO, "LoggingBootstrapTest", "console-message");
        assertNotLoggedToConsole(Level.INFO, "DummyTest", "internal-message");
        assertLoggedToConsole(Level.ERROR, "DummyTest", "internal-error");
    }

    @Test
    void shouldAlsoLogToFileWhenDirectoryIsSpecified() throws IOException {
        try (LoggerContext context = LoggingBootstrap.initialize(Optional.of(Level.INFO), Optional.of(loggingDirectory))) {
            Logger logger = context.getLogger(LoggingBootstrapTest.class);

            logger.info("console-and-file-message");
        }

        assertLoggedToConsole(Level.INFO, "LoggingBootstrapTest", "console-and-file-message");

        assertTrue(Files.exists(logFile), "Expected log file to be created");
        assertLoggedToFile(Level.INFO, "LoggingBootstrapTest", "console-and-file-message");
    }

    @Test
    void shouldApplyConfiguredLogLevel() {
        try (LoggerContext context = LoggingBootstrap.initialize(Optional.of(Level.ERROR), Optional.empty())) {
            Logger logger = context.getLogger(LoggingBootstrapTest.class);

            logger.info("info-message");
            logger.error("error-message");

            // Fake a logger from an internal library, which should have the same level now
            Logger internalLogger = context.getLogger("org.drools.DummyTest");

            internalLogger.warn("internal-warning");
            internalLogger.error("internal-error");
        }

        assertFalse(Files.exists(logFile), "Expected log file NOT to be created");

        assertNotLoggedToConsole(Level.INFO, "LoggingBootstrapTest", "info-message");
        assertLoggedToConsole(Level.ERROR, "LoggingBootstrapTest", "error-message");

        assertNotLoggedToConsole(Level.WARN, "DummyTest", "internal-warning");
        assertLoggedToConsole(Level.ERROR, "DummyTest", "internal-error");
    }

    @Test
    void shouldReconfigureExistingLoggingContext() {
        // Create loggers before invoking LoggingBootstrap.initialize, with LOG4J
        Logger namedLogger = LogManager.getLogger("pre.existing.logger");
        Logger classLogger = LogManager.getLogger(LoggingBootstrapTest.class);

        // These should not be present since the default configuration does not log DEBUG level
        namedLogger.debug("named-warm-up");
        classLogger.debug("class-warm-up");

        try (LoggerContext initializedContext = LoggingBootstrap.initialize(Optional.of(Level.DEBUG), Optional.empty())) {
            Configuration configuration = initializedContext.getConfiguration();

            assertUsingConfiguration(namedLogger, configuration);
            assertUsingConfiguration(classLogger, configuration);
        
            // If the configuration has been correctly updated, these messages should now be logged
            namedLogger.debug("named-post-init");
            classLogger.debug("class-post-init");
        }

        assertNotLoggedToConsole(Level.DEBUG, "logger", "named-warm-up");
        assertNotLoggedToConsole(Level.DEBUG, "LoggingBootstrapTest", "class-warm-up");

        assertLoggedToConsole(Level.DEBUG, "logger", "named-post-init");
        assertLoggedToConsole(Level.DEBUG, "LoggingBootstrapTest", "class-post-init");
    }

    @Test
    void shouldReconfigureExistingSLF4JLoggingContext() {
        // Create loggers before invoking Log4J.initialize with SLF4J
        org.slf4j.Logger namedLogger = LoggerFactory.getLogger("pre.existing.logger");
        org.slf4j.Logger classLogger = LoggerFactory.getLogger(LoggingBootstrapTest.class);

        // These should not be present since the default configuration does not log DEBUG level
        namedLogger.debug("named-warm-up");
        classLogger.debug("class-warm-up");

        try (LoggerContext initializedContext = LoggingBootstrap.initialize(Optional.of(Level.DEBUG), Optional.empty())) {
            // If the configuration has been correctly updated, these messages should now be logged
            namedLogger.debug("named-post-init");
            classLogger.debug("class-post-init");
        }

        assertNotLoggedToConsole(Level.DEBUG, "logger", "named-warm-up");
        assertNotLoggedToConsole(Level.DEBUG, "LoggingBootstrapTest", "class-warm-up");

        assertLoggedToConsole(Level.DEBUG, "logger", "named-post-init");
        assertLoggedToConsole(Level.DEBUG, "LoggingBootstrapTest", "class-post-init");
    }

    private void assertLoggedToConsole(Level level, String loggerName, String message) {
        String output = capturedErr.toString(StandardCharsets.UTF_8);

        assertLogPattern(output, level, loggerName, message, true);
    }

    private void assertNotLoggedToConsole(Level level, String loggerName, String message) {
        String output = capturedErr.toString(StandardCharsets.UTF_8);

        assertLogPattern(output, level, loggerName, message, false);
    }

    private void assertLoggedToFile(Level level, String loggerName, String message) throws IOException {
        String fileContent = Files.readString(logFile, StandardCharsets.UTF_8);

        assertLogPattern(fileContent, level, loggerName, message, true);
    }

    private static void assertLogPattern(String output, Level level, String logger, String message, boolean present) {
        String linePattern = "(?m)^([0-9-:, ]+\\s+){0,1}%s\\s+%s\\s+-\\s+%s$"
            .formatted(Pattern.quote(level.name()), Pattern.quote(logger), Pattern.quote(message));

        String assertionMessage = "Expected %slog line matching level/logger/message pattern. Output was: %s"
            .formatted(present ? "" : "no ", output);

        assertEquals(present, Pattern.compile(linePattern).matcher(output).find(), assertionMessage);
    }

    private static void assertUsingConfiguration(Logger logger, Configuration expectedConfiguration) {
        var loggerImpl = assertInstanceOf(org.apache.logging.log4j.core.Logger.class, logger,
            "Expected logger to be an instance of org.apache.logging.log4j.core.Logger");

        // The logger should be using the same configuration as the one returned by LoggingBootstrap.initialize
        assertSame(expectedConfiguration, loggerImpl.getContext().getConfiguration(),
            "Expected logger to be using the initialized Configuration");
    }
}
