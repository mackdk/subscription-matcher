package com.suse.matcher.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Stream;

class CommandLineArgumentsTest {

    private static Path tempDirectory;

    @BeforeAll
    public static void setupTestFiles() throws IOException {
        tempDirectory = Files.createTempDirectory(CommandLineArgumentsTest.class.getSimpleName() + "-");

        Files.createFile(tempDirectory.resolve("input.json"));
        Files.createDirectory(tempDirectory.resolve("output"));
        Files.createDirectory(tempDirectory.resolve("log"));
    }

    @AfterAll
    public static void removeTestFiles() throws IOException {
        try (Stream<Path> fileStream = Files.walk(tempDirectory)) {
            fileStream
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::deleteOnExit);
        }
    }

    @DisplayName("Throws ParseException")
    @ParameterizedTest(name = "When {1} is invalid")
    @MethodSource("invalidArguments")
    void throwsExceptionWithInvalidArguments(String expectedMessage, String arg, String invalidValue) {
        var ex = assertThrows(
            IllegalArgumentException.class,
            () -> CommandLineArguments.parseCommandLine(new String[]{arg, invalidValue})
        );

        assertInstanceOf(ParseException.class, ex.getCause());
        assertEquals(expectedMessage, ex.getCause().getMessage());
    }

    @DisplayName("Use correct defaults when no arguments are specified")
    @Test
    void canCreateArgumentsWithCorrectDefaults() throws IOException {
        String[] emptyArgs = {};

        Arguments arguments = CommandLineArguments.parseCommandLine(emptyArgs);

        assertEquals(System.in, arguments.getInputStream());
        assertEquals(',', arguments.getDelimiter());
        assertEquals(Optional.empty(), arguments.getLoggingDirectory());
        assertEquals(Optional.empty(), arguments.getLoggingLevel());
        assertEquals(Path.of("."), arguments.getOutputDirectory());
    }

    @DisplayName("Set correct values when parameters are specified")
    @Test
    void canCreateArgumentsWithCorrectValues() throws IOException {
        String pathPrefix = tempDirectory.toAbsolutePath().toString();

        String fullArgs = new StringJoiner(" ")
            .add("--input " + pathPrefix + "/input.json")
            .add("--output-directory " + pathPrefix + "/output")
            .add("--log-directory " + pathPrefix + "/log")
            .add("--log-level DEBUG")
            .add("--delimiter ;")
            .toString();

        Arguments arguments = CommandLineArguments.parseCommandLine(fullArgs.split(" "));

        assertNotEquals(System.in, arguments.getInputStream());
        assertEquals(';', arguments.getDelimiter());
        assertEquals(Optional.of(Path.of(pathPrefix + "/log")), arguments.getLoggingDirectory());
        assertEquals(Optional.of(Level.DEBUG), arguments.getLoggingLevel());
        assertEquals(Path.of(pathPrefix + "/output"), arguments.getOutputDirectory());
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidArguments() {
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of("Given log level is invalid", "--log-level", "DUMMY"),
            org.junit.jupiter.params.provider.Arguments.of("Given input file does not exist or is not readable", "--input", "/this/does/not/exist"),
            org.junit.jupiter.params.provider.Arguments.of("Given output directory does not exist or is not a directory", "--output-directory", "/this/does/not/exist"),
            org.junit.jupiter.params.provider.Arguments.of("Given logging directory does not exist or is not a directory", "--log-directory", "/this/does/not/exist")
        );
    }
}
