package com.suse.matcher.io;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Bootstrap class for logging configuration. Configures Log4j 2 for the application,
 * allowing dynamic overrides of the default XML configuration.
 */
public class LoggingBootstrap {

    private LoggingBootstrap() {
        // Prevent instantiation
    }

    /**
     * Initialize the Log4j 2 configuration.
     *
     * @param level user chosen level
     * @param loggingDirectory directory for file logging
     *
     * @return the Log4j 2 {@link LoggerContext}
     */
    public static LoggerContext initialize(Optional<Level> level, Optional<Path> loggingDirectory) {
        // Read the default configuration from the XML file
        XmlConfiguration xmlConfiguration = getXmlConfiguration("log4j2.xml");

        // Create a new configuration instance with only the properties we want to override
        BuiltConfiguration builtConfiguration = createBuiltConfiguration(level, loggingDirectory);

        // Merge the two configurations, ensuring the properties override the XML defaults
        CompositeConfiguration configuration = new CompositeConfiguration(List.of(xmlConfiguration, builtConfiguration));
        LoggerContext context = Configurator.initialize(configuration);
        if (context == null) {
            throw new IllegalStateException("Unable to initialize Log4J 2 context");
        }

        // Reconfigure all the running contexts where the configuration does not match the one just created
        runningLoggingContextsStream(context)
            .filter(ctx -> ctx.getConfiguration() != configuration)
            .forEach(ctx -> ctx.reconfigure(configuration));

        return context;
    }

    private static XmlConfiguration getXmlConfiguration(String resourceName) {
        var xmlSource = ConfigurationSource.fromResource(resourceName, LoggingBootstrap.class.getClassLoader());
        var configuration = ConfigurationFactory.getInstance().getConfiguration(null, xmlSource);

        if (!(configuration instanceof XmlConfiguration xmlConfiguration)) {
            throw new IllegalStateException("Expected an XML configuration, but got: " + configuration.getClass());
        }

        return xmlConfiguration;
    }

    private static BuiltConfiguration createBuiltConfiguration(Optional<Level> level, Optional<Path> loggingDirectory) {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        builder.setConfigurationName("EmptyConfiguration");
        level.map(Level::name).ifPresent(levelName -> {
            builder.addProperty("internalLogLevel", levelName);
            builder.addProperty("externalLogLevel", levelName);
        });

        loggingDirectory.map(p -> p.toAbsolutePath().toString()).ifPresent(logDir -> {
            builder.addProperty("logDir", logDir);
        });

        return builder.build();
    }

    private static Stream<LoggerContext> runningLoggingContextsStream(LoggerContext createdContext) {
        if (LogManager.getFactory() instanceof Log4jContextFactory contextFactory) {
            List<LoggerContext> existingContexts = contextFactory.getSelector().getLoggerContexts();
            return Stream.concat(Stream.of(createdContext), existingContexts.stream()).distinct();
        }

        return Stream.of(createdContext);
    }
}
