package fi.hsl.pulsar.mqtt;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import fi.hsl.common.config.ConfigParser;
import fi.hsl.common.health.HealthServer;
import fi.hsl.common.pulsar.PulsarApplication;
import fi.hsl.common.pulsar.PulsarApplicationContext;
import fi.hsl.common.transitdata.TransitdataProperties;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.*;

/**
 * End-to-end: Pulsar topic → the application wired as in {@link Main} → a real MQTT broker.
 */
public class PulsarToMqttFlowIT {
    // Matches the pulsar-client version of transitdata-common 2.0.5
    private static final DockerImageName PULSAR_IMAGE = DockerImageName.parse("apachepulsar/pulsar:3.0.7");
    private static final DockerImageName MOSQUITTO_IMAGE = DockerImageName.parse("eclipse-mosquitto:2.0.20");
    private static final int MQTT_PORT = 1883;

    @ClassRule
    public static final PulsarContainer PULSAR = new PulsarContainer(PULSAR_IMAGE);

    @ClassRule
    public static final GenericContainer<?> MOSQUITTO = mosquitto();

    private static PulsarClient client;
    private static PulsarAdmin admin;

    private final List<AutoCloseable> closeables = new ArrayList<>();
    private String pulsarTopic;
    private String subscription;
    private String mqttTopic;
    private int healthPort;

    @SuppressWarnings("resource")
    private static GenericContainer<?> mosquitto() {
        return new GenericContainer<>(MOSQUITTO_IMAGE).withExposedPorts(MQTT_PORT)
                .withCommand("mosquitto", "-c", "/mosquitto-no-auth.conf")
                .waitingFor(Wait.forLogMessage(".*mosquitto version .* running.*", 1));
    }

    private static String brokerUrl(GenericContainer<?> broker) {
        return "tcp://" + broker.getHost() + ":" + broker.getMappedPort(MQTT_PORT);
    }

    @BeforeClass
    public static void connect() throws Exception {
        client = PulsarClient.builder().serviceUrl(PULSAR.getPulsarBrokerUrl()).build();
        admin = PulsarAdmin.builder().serviceHttpUrl(PULSAR.getHttpServiceUrl()).build();
    }

    @AfterClass
    public static void disconnect() throws Exception {
        if (client != null) {
            client.close();
        }
        if (admin != null) {
            admin.close();
        }
    }

    @Before
    public void uniqueNames() throws Exception {
        String id = UUID.randomUUID().toString();
        pulsarTopic = "persistent://public/default/mqtt-gw-" + id;
        subscription = "sub-" + id;
        mqttTopic = "it/" + id;
        try (ServerSocket socket = new ServerSocket(0)) {
            healthPort = socket.getLocalPort();
        }
    }

    @After
    public void closeAll() {
        Collections.reverse(closeables);
        for (AutoCloseable closeable : closeables) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // best effort
            }
        }
    }

    private Config config(String broker, boolean retain) {
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("pulsar.host", PULSAR.getHost());
        overrides.put("pulsar.port", PULSAR.getMappedPort(PulsarContainer.BROKER_PORT));
        overrides.put("pulsar.consumer.topic", pulsarTopic);
        overrides.put("pulsar.consumer.subscription", subscription);
        overrides.put("health.enabled", true);
        overrides.put("health.port", healthPort);
        overrides.put("mqtt-broker.host", broker);
        overrides.put("mqtt-broker.topic", mqttTopic);
        overrides.put("mqtt-broker.clientId", "gateway-" + UUID.randomUUID());
        overrides.put("mqtt-broker.hasAuthentication", false);
        overrides.put("mqtt-broker.retainMessage", retain);
        return ConfigFactory.parseMap(overrides).withFallback(ConfigParser.createConfig()).resolve();
    }

    /** Wires the application exactly as {@link Main#main} does, then waits for its subscription. */
    private MessageProcessor launch(String broker, boolean retain) throws Exception {
        Config config = config(broker, retain);
        MqttConfig sinkConfig = Main.createSinkConfig(config);

        PulsarApplication app = PulsarApplication.newInstance(config);
        closeables.add(app);
        PulsarApplicationContext context = app.getContext();
        MessageProcessor processor = MessageProcessor.newInstance(sinkConfig, context.getConsumer());

        HealthServer healthServer = context.getHealthServer();
        final BooleanSupplier mqttHealthCheck = () -> processor != null && processor.isMqttConnected();
        if (healthServer != null) {
            healthServer.addCheck(mqttHealthCheck);
        }

        Thread thread = new Thread(() -> {
            try {
                app.launchWithHandler(processor);
            } catch (Exception ignored) {
                // closed by the test
            }
        }, "pulsar-mqtt-gateway-it");
        thread.setDaemon(true);
        thread.start();

        await(() -> {
            try {
                return admin.topics().getSubscriptions(pulsarTopic).contains(subscription);
            } catch (Exception e) {
                return false;
            }
        }, "subscription " + subscription);
        return processor;
    }

    private BlockingQueue<Received> subscribe(String broker, String filter) throws Exception {
        BlockingQueue<Received> received = new LinkedBlockingQueue<>();
        MqttClient subscriber = new MqttClient(broker, "it-sub-" + UUID.randomUUID(), new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        subscriber.connect(options);
        subscriber.subscribe(filter, 1, (topic, message) -> received.add(new Received(topic, message)));
        closeables.add(() -> {
            subscriber.disconnectForcibly(1000L);
            subscriber.close(true);
        });
        return received;
    }

    private Producer<byte[]> producer() throws Exception {
        Producer<byte[]> producer = client.newProducer().topic(pulsarTopic).create();
        closeables.add(producer);
        return producer;
    }

    private long backlog() throws Exception {
        return admin.topics().getStats(pulsarTopic).getSubscriptions().get(subscription).getMsgBacklog();
    }

    private int health() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + healthPort + "/health")
                .openConnection();
        try {
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }

    private static void await(BooleanSupplier condition, String what) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                fail("timed out waiting for " + what);
            }
            Thread.sleep(200);
        }
    }

    @Test
    public void forwardsPayloadUnchangedToBaseTopicAndAcks() throws Exception {
        String broker = brokerUrl(MOSQUITTO);
        BlockingQueue<Received> received = subscribe(broker, mqttTopic + "/#");
        BlockingQueue<Received> base = subscribe(broker, mqttTopic);
        launch(broker, false);

        byte[] payload = {0, 1, 2, 3, (byte) 0x80, (byte) 0xff};
        producer().newMessage().value(payload).send();

        Received message = base.poll(30, TimeUnit.SECONDS);
        assertNotNull("no MQTT message on " + mqttTopic, message);
        assertEquals(mqttTopic, message.topic);
        assertArrayEquals(payload, message.message.getPayload());
        assertEquals(1, message.message.getQos());
        assertFalse(message.message.isRetained());
        assertEquals(message.topic, received.poll(30, TimeUnit.SECONDS).topic);

        await(() -> {
            try {
                return backlog() == 0;
            } catch (Exception e) {
                return false;
            }
        }, "the message to be acknowledged");
    }

    @Test
    public void appendsTopicSuffixFromPulsarProperty() throws Exception {
        String broker = brokerUrl(MOSQUITTO);
        BlockingQueue<Received> received = subscribe(broker, mqttTopic + "/#");
        launch(broker, false);

        producer().newMessage().value(new byte[]{42}).property(TransitdataProperties.KEY_MQTT_TOPIC, "1234/5").send();

        Received message = received.poll(30, TimeUnit.SECONDS);
        assertNotNull(message);
        assertEquals(mqttTopic + "/1234/5", message.topic);
        assertArrayEquals(new byte[]{42}, message.message.getPayload());
    }

    @Test
    public void keepsOrderWithinOneTopic() throws Exception {
        String broker = brokerUrl(MOSQUITTO);
        BlockingQueue<Received> received = subscribe(broker, mqttTopic);
        launch(broker, false);

        Producer<byte[]> producer = producer();
        for (int i = 0; i < 50; i++) {
            producer.newMessage().value(new byte[]{(byte) i}).send();
        }
        for (int i = 0; i < 50; i++) {
            Received message = received.poll(30, TimeUnit.SECONDS);
            assertNotNull("message " + i + " missing", message);
            assertArrayEquals(new byte[]{(byte) i}, message.message.getPayload());
        }
    }

    @Test
    public void retainedMessagesReachLateSubscribers() throws Exception {
        String broker = brokerUrl(MOSQUITTO);
        launch(broker, true);

        producer().newMessage().value(new byte[]{7}).send();
        await(() -> {
            try {
                return backlog() == 0;
            } catch (Exception e) {
                return false;
            }
        }, "the message to be delivered");

        BlockingQueue<Received> late = subscribe(broker, mqttTopic);
        Received message = late.poll(30, TimeUnit.SECONDS);
        assertNotNull("retained message not delivered to a late subscriber", message);
        assertTrue(message.message.isRetained());
        assertArrayEquals(new byte[]{7}, message.message.getPayload());
    }

    @Test
    public void healthTurnsUnhealthyWhenTheBrokerGoesAway() throws Exception {
        try (GenericContainer<?> broker = mosquitto()) {
            broker.start();
            MessageProcessor processor = launch(brokerUrl(broker), false);
            assertTrue(processor.isMqttConnected());
            assertEquals(200, health());

            broker.stop();

            await(() -> !processor.isMqttConnected(), "the MQTT connection to be lost");
            assertNotEquals(200, health());
        }
    }

    @Test(expected = Exception.class)
    public void startupFailsWhenTheBrokerIsUnreachable() throws Exception {
        MqttConfig sinkConfig = Main.createSinkConfig(config("tcp://localhost:1", false));
        MessageProcessor.newInstance(sinkConfig, null);
    }

    private static final class Received {
        final String topic;
        final MqttMessage message;

        Received(String topic, MqttMessage message) {
            this.topic = topic;
            this.message = message;
        }
    }
}
