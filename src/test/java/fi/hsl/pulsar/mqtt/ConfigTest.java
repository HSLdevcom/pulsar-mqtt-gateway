package fi.hsl.pulsar.mqtt;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import fi.hsl.common.config.ConfigParser;
import org.junit.Assume;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * environment.conf defaults and how {@link Main#createSinkConfig} turns them into an {@link MqttConfig}.
 */
public class ConfigTest {

    private static Config config(Map<String, Object> overrides) {
        return ConfigFactory.parseMap(overrides).withFallback(ConfigParser.createConfig()).resolve();
    }

    private static Map<String, Object> mqtt(boolean auth) {
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("mqtt-broker.topic", "base/topic");
        overrides.put("mqtt-broker.clientId", "client");
        overrides.put("mqtt-broker.hasAuthentication", auth);
        return overrides;
    }

    @Test
    public void environmentConfDefaults() {
        Assume.assumeTrue("MQTT_* / PULSAR_* variables are set in this environment",
                System.getenv().keySet().stream().noneMatch(k -> k.startsWith("MQTT_") || k.startsWith("PULSAR_")));
        Config config = ConfigParser.createConfig();

        assertEquals("ssl://mqtt.hsl.fi:443", config.getString("mqtt-broker.host"));
        assertFalse("MQTT_TOPIC has no default", config.hasPath("mqtt-broker.topic"));
        assertEquals(10000, config.getInt("mqtt-broker.maxInflight"));
        assertEquals("", config.getString("mqtt-broker.clientId"));
        assertFalse(config.getBoolean("mqtt-broker.retainMessage"));
        assertEquals(30, config.getInt("mqtt-broker.keepAliveInterval"));
        assertTrue(config.getBoolean("mqtt-broker.hasAuthentication"));

        assertTrue(config.getBoolean("pulsar.consumer.enabled"));
        assertEquals("trip-update", config.getString("pulsar.consumer.topic"));
        assertEquals("mqtt-gateway", config.getString("pulsar.consumer.subscription"));
        assertEquals("Exclusive", config.getString("pulsar.consumer.subscriptionType"));
        assertTrue(config.getBoolean("pulsar.consumer.cursor.resetToLatest"));
        assertFalse(config.getBoolean("pulsar.producer.enabled"));
    }

    @Test
    public void sinkConfigWithoutAuthentication() {
        Map<String, Object> overrides = mqtt(false);
        overrides.put("mqtt-broker.host", "tcp://broker:1883");
        overrides.put("mqtt-broker.maxInflight", 5);
        overrides.put("mqtt-broker.retainMessage", true);
        overrides.put("mqtt-broker.keepAliveInterval", 7);

        MqttConfig sink = Main.createSinkConfig(config(overrides));

        assertEquals("tcp://broker:1883", sink.getBroker());
        assertEquals("base/topic", sink.getMqttTopic());
        assertEquals("client", sink.getClientId());
        assertEquals(5, sink.getMaxInflight());
        assertTrue(sink.getRetainMessage());
        assertEquals(7, sink.getKeepAliveInterval());
        assertFalse(sink.hasAuthentication());
        assertNull(sink.getUsername());
        assertNull(sink.getPassword());
    }

    @Test
    public void authenticationWithoutCredentialVariablesFailsInTheBuilder() {
        Assume.assumeTrue(System.getenv("MQTT_BROKER_USERNAME") == null);
        // The "Failed to find credentials" exception is caught and only logged inside createSinkConfig;
        // the null credentials are then rejected by the builder.
        try {
            Main.createSinkConfig(config(mqtt(true)));
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Required field not set for MqttConfig", e.getMessage());
        }
    }

    @Test(expected = ConfigException.Missing.class)
    public void missingTopicFailsAtStartup() {
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("mqtt-broker.hasAuthentication", false);
        Config config = ConfigFactory.parseMap(overrides).withFallback(ConfigParser.createConfig()).resolve();
        Assume.assumeFalse(config.hasPath("mqtt-broker.topic"));

        Main.createSinkConfig(config);
    }
}
