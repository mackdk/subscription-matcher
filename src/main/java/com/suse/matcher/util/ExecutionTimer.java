package com.suse.matcher.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

/**
 * Timings is a utility class for dealing with time.
 */
public class ExecutionTimer {

    private static final Logger LOGGER = LogManager.getLogger(ExecutionTimer.class);

    private ExecutionTimer() {
        // private constructor
    }

    /**
     * Helper for logging the time some code took to execute.
     * @param log logger to use.
     * @param logLevel the log level
     * @param name a name/tag to describe whats being executed
     * @param fun the code to time
     * @param <T> type of return value
     * @param <E> type of the exception
     * @return returns whatever fun returns
     */
    public static <E extends Exception> void measure(String name, ThrowingRunnable<E> fun) throws E {
        measure(name, () -> {
            fun.run();
            return null;
        });
    }

    /**
     * Helper for logging the time some code took to execute.
     * @param log logger to use.
     * @param logLevel the log level
     * @param name a name/tag to describe whats being executed
     * @param fun the code to time
     * @param <T> type of return value
     * @param <E> type of the exception
     * @return returns whatever fun returns
     */
    public static <T, E extends Exception> T measure(String name, ThrowingSupplier<T, E> fun) throws E {
        if (!LOGGER.isInfoEnabled()) {
            return fun.get();
        }

        long start = System.nanoTime();
        T result = fun.get();
        long end = System.nanoTime();

        LOGGER.info("{} took {}.", name, Duration.ofNanos(end - start));

        return result;
    }

    @FunctionalInterface
    public static interface ThrowingSupplier<T, E extends Exception> {
    
        T get() throws E;
    }

    @FunctionalInterface
    public static interface ThrowingRunnable<E extends Exception> {
    
        void run() throws E;
    }

}
