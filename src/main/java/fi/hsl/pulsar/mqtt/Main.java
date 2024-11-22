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
                username = System.getenv("MQTT_BROKER_USERNAME");
                password = System.getenv("MQTT_BROKER_PASSWORD");
                if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                    log.error("Failed to find credentials");
                    throw new IllegalArgumentException("Failed to find credentials");
                }
            } catch (Exception e) {
                log.error("Failed to read secret files", e);
            }
        }

        final String clientId = config.getString("mqtt-broker.clientId");
        final String broker = config.getString("mqtt-broker.host");
        final int maxInFlight = config.getInt("mqtt-broker.maxInflight");
        final String topic = config.getString("mqtt-broker.topic");
        final boolean retainMessage = config.getBoolean("mqtt-broker.retainMessage");
        final int keepAliveInterval = config.getInt("mqtt-broker.keepAliveInterval");
        log.info("Setting MQTT topic to {} with retain message enabled: {} ", topic, retainMessage);

        MqttConfigBuilder configBuilder = MqttConfig.newBuilder()
                .setBroker(broker)
                .setUsername(username)
                .setPassword(password)
                .setAuthentication(hasAuthentication)
                .setClientId(clientId)
                .setMqttTopic(topic)
                .setMaxInflight(maxInFlight)
                .setRetainMessage(retainMessage)
                .setKeepAliveInterval(keepAliveInterval);

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
