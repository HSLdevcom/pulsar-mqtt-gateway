package fi.hsl.pulsar.mqtt;

import com.typesafe.config.Config;
import fi.hsl.common.config.ConfigParser;
import fi.hsl.common.config.ConfigUtils;
import fi.hsl.common.health.HealthServer;
import fi.hsl.common.pulsar.PulsarApplication;
import fi.hsl.common.pulsar.PulsarApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Scanner;
import java.util.function.BooleanSupplier;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static MqttConfig createSinkConfig(Config config) {
        final boolean hasAuthentication = config.getBoolean("mqtt-broker.hasAuthentication");

        String username = null;
        String password = null;

        if (hasAuthentication) {
            try {
                //Default path is what works with Docker out-of-the-box. Override with a local file if needed
                final String usernamePath = ConfigUtils.getEnv("FILEPATH_USERNAME_SECRET").orElse("/run/secrets/mqtt_broker_username");
                log.debug("Reading username from " + usernamePath);
                username = new Scanner(new File(usernamePath)).useDelimiter("\\Z").next();

                final String passwordPath = ConfigUtils.getEnv("FILEPATH_PASSWORD_SECRET").orElse("/run/secrets/mqtt_broker_password");
                log.debug("Reading password from " + passwordPath);
                password = new Scanner(new File(passwordPath)).useDelimiter("\\Z").next();

            } catch (Exception e) {
                log.error("Failed to read secret files", e);
            }
        }

        final String clientId = config.getString("mqtt-broker.clientId");
        final String broker = config.getString("mqtt-broker.host");
        final int maxInFlight = config.getInt("mqtt-broker.maxInflight");
        final String topic = config.getString("mqtt-broker.topic");
        final boolean retainMessage = config.getBoolean("mqtt-broker.retainMessage");
        log.info("Setting MQTT topic to {} with retain message enabled: {} ", topic, retainMessage);

        MqttConfigBuilder configBuilder = MqttConfig.newBuilder()
                .setBroker(broker)
                .setUsername(username)
                .setPassword(password)
                .setAuthentication(hasAuthentication)
                .setClientId(clientId)
                .setMqttTopic(topic)
                .setMaxInflight(maxInFlight)
                .setRetainMessage(retainMessage);

        return configBuilder.build();
    }


    public static void main(String[] args) {
        log.info("Launching Pulsar-MQTT-Gateway.");

        Config config = ConfigParser.createConfig();
        MqttConfig sinkConfig = createSinkConfig(config);

        log.info("Configurations read, launching Pulsar Application");

        try (PulsarApplication app = PulsarApplication.newInstance(config)) {
            PulsarApplicationContext context = app.getContext();
            MessageProcessor processor = MessageProcessor.newInstance(sinkConfig, context.getConsumer());

            HealthServer healthServer = context.getHealthServer();
            final BooleanSupplier mqttHealthCheck = () -> {
                if (processor != null) {
                    return processor.isMqttConnected();
                }
                return false;
            };
            if (healthServer != null) {
                healthServer.addCheck(mqttHealthCheck);
            }
            
            log.info("Starting to process messages");

            app.launchWithHandler(processor);

        }
        catch (Exception e) {
            log.error("Exception at main", e);
        }

    }
}
