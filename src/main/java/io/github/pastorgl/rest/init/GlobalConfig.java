package io.github.pastorgl.rest.init;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public abstract class GlobalConfig {
    public static final String PROPERTY_SERVER_PORT = "server.port";
    public static final String PROPERTY_SERVER_INTERFACE = "server.interface";
    public static final String PROPERTY_DATABASE_URL = "database.url";
    public static final String PROPERTY_AUTH_CHECK_ENDPOINT = "auth.check.endpoint";
    public static final String OPTION_CONFIG_PATH = "configPath";
    public static final String OPTION_SERVER_PORT = "port";
    public static final String OPTION_SERVER_INTERFACE = "iface";
    public static final String OPTION_DATABASE_URL = "databaseUrl";
    public static final String OPTION_AUTH_CHECK_ENDPOINT = "authCheckEndpoint";

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalConfig.class);

    private final boolean withDatabaseUrl;
    private final boolean withAuthCheckEndpoint;

    protected Properties properties;
    protected CommandLine commandLine;
    protected Options options;

    protected GlobalConfig(boolean withDatabaseUrl, boolean withAuthCheckEndpoint) {
        this.withDatabaseUrl = withDatabaseUrl;
        this.withAuthCheckEndpoint = withAuthCheckEndpoint;

        options = new Options()
                .addOption("c", OPTION_CONFIG_PATH, true, "Path to configuration file")
                .addOption("p", OPTION_SERVER_PORT, true, "Server port. By default, " + defaultPort())
                .addOption("e", OPTION_SERVER_INTERFACE, true, "Server interface. By default, 0.0.0.0 (all interfaces)");

        if (withDatabaseUrl) {
            options.addOption("d", OPTION_DATABASE_URL, true, "Database URL. Required if does not exist in the configuration file");
        }
        if (withAuthCheckEndpoint) {
            options.addOption("a", OPTION_AUTH_CHECK_ENDPOINT, true, "Auth check endpoint. Required if does not exist in the configuration file");
        }
    }

    public void parse(String... args) throws ParseException, IOException {
        commandLine = new DefaultParser().parse(options, args);

        properties = new Properties();
        String configPath = commandLine.getOptionValue(OPTION_CONFIG_PATH, defaultConfigFile());
        try {
            properties.load(new FileReader(configPath));
        } catch (IOException e) {
            LOGGER.error("Invalid configuration file '" + configPath + "'", e);
            throw e;
        }

        if (withAuthCheckEndpoint) {
            if (commandLine.hasOption(OPTION_AUTH_CHECK_ENDPOINT)) {
                properties.setProperty(PROPERTY_AUTH_CHECK_ENDPOINT, commandLine.getOptionValue(OPTION_AUTH_CHECK_ENDPOINT));
            }

            if (properties.getProperty(PROPERTY_AUTH_CHECK_ENDPOINT) == null) {
                ParseException parseException = new ParseException("Auth check endpoint is not set");
                LOGGER.error("Auth check endpoint must be configured with an URL", parseException);
                throw parseException;
            }
        }

        if (withDatabaseUrl) {
            if (commandLine.hasOption(OPTION_DATABASE_URL)) {
                properties.setProperty(PROPERTY_DATABASE_URL, commandLine.getOptionValue(OPTION_DATABASE_URL));
            }

            if (properties.getProperty(PROPERTY_DATABASE_URL) == null) {
                ParseException parseException = new ParseException("Database not configured");
                LOGGER.error("Database must be configured with an URL", parseException);
                throw parseException;
            }
        }

        if (commandLine.hasOption(OPTION_SERVER_PORT)) {
            properties.setProperty(PROPERTY_SERVER_PORT, commandLine.getOptionValue(OPTION_SERVER_PORT));
        }

        if (properties.getProperty(PROPERTY_SERVER_PORT) == null) {
            properties.setProperty(PROPERTY_SERVER_PORT, String.valueOf(defaultPort()));
        }

        if (commandLine.hasOption(OPTION_SERVER_INTERFACE)) {
            properties.setProperty(PROPERTY_SERVER_INTERFACE, commandLine.getOptionValue(OPTION_SERVER_INTERFACE));
        }

        if (properties.getProperty(PROPERTY_SERVER_INTERFACE) == null) {
            properties.setProperty(PROPERTY_SERVER_INTERFACE, "0.0.0.0");
        }
    }

    protected abstract String defaultConfigFile();

    protected abstract int defaultPort();

    public CommandLine getCommandLine() {
        return commandLine;
    }

    public Properties getProperties() {
        return properties;
    }

    public Options getOptions() {
        return options;
    }
}
