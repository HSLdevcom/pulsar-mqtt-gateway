package fi.hsl.transitdata.mqttsink;

import com.typesafe.config.Config;
import fi.hsl.common.ConfigParser;
import fi.hsl.common.pulsar.PulsarApplication;
import fi.hsl.common.pulsar.PulsarApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;
import java.util.Scanner;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    //////////////////////////////////////////////////////////
    // TODO move these to some utility class:

    private static String getEnvOrThrow(String name) throws IllegalArgumentException {
        return Optional.ofNullable(System.getenv(name))
                .orElseThrow(() -> new IllegalArgumentException("Missing required env variable " + name));
    }

    private static Optional<String> getEnv(String name) {
        return Optional.ofNullable(System.getenv(name));
    }

    private static Optional<Integer> safeParseInt(String value) {
        try {
            int n = Integer.parseInt(value);
            return Optional.of(n);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static Optional<Integer> getIntEnv(String name) {
        return Optional.ofNullable(System.getenv(name)).flatMap(Main::safeParseInt);
    }

    //////////////////////////////////////////////////////////

    private static MqttSinkConfig createSinkConfig(Config config) {
        String username = "";
        String password = "";
        try {
            //Default path is what works with Docker out-of-the-box. Override with a local file if needed
            final String usernamePath = getEnv("FILEPATH_USERNAME_SECRET").orElse("/run/secrets/mqtt_broker_username");
            username = new Scanner(new File(usernamePath)).useDelimiter("\\Z").next();

            final String passwordPath = getEnv("FILEPATH_PASSWORD_SECRET").orElse("/run/secrets/mqtt_broker_password");
            password = new Scanner(new File(passwordPath)).useDelimiter("\\Z").next();
        } catch (Exception e) {
            log.error("Failed to read secret files", e);
        }

        final String clientId = config.getString("mqtt-broker.clientId");
        final String broker = config.getString("mqtt-broker.host");

        MqttSinkConfigBuilder configBuilder = MqttSinkConfig.newBuilder()
                .setBroker(broker)
                .setUsername(username)
                .setPassword(password)
                .setClientId(clientId);

        if (config.hasPath("mqtt-broker.topic")) {
            String topic = config.getString("mqtt-broker.topic");
            log.info("Setting MQTT TOPIC to " + topic);
            configBuilder.setMqttTopic(topic);
        }

        if (config.hasPath("mqtt-broker.maxInflight")) {
            int max = config.getInt("mqtt-broker.maxInflight");
            log.info("Setting MAX INFLIGHT to " + max);
            configBuilder.setMaxInflight(max);
        }
        return configBuilder.build();
    }


    public static void main(String[] args) {
        Config config = ConfigParser.createConfig();
        MqttSinkConfig sinkConfig = createSinkConfig(config);
        try (PulsarApplication app = PulsarApplication.newInstance(config)) {
            PulsarApplicationContext context = app.getContext();

            MessageProcessor processor = new MessageProcessor(sinkConfig, context.getConsumer());
            Thread.sleep(1000L); //Wait for connection to complete
            app.launchWithHandler(processor);

        }
        catch (Exception e) {
            log.error("Exception at main", e);
        }

    }
}
