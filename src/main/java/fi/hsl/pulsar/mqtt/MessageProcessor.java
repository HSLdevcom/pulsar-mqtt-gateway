package fi.hsl.pulsar.mqtt;

import fi.hsl.common.pulsar.IMessageHandler;
import fi.hsl.common.transitdata.TransitdataProperties;
import org.apache.pulsar.client.api.*;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class MessageProcessor implements IMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MessageProcessor.class);

    private final Consumer<byte[]> consumer;
    private final MqttAsyncClient mqttClient;
    private final String mqttTopic;
    private final boolean retainMessage;

    private MessageProcessor(Consumer<byte[]> consumer, MqttAsyncClient mqtt, String topic, boolean retain) {
        this.consumer = consumer;
        this.mqttClient = mqtt;
        this.mqttTopic = topic;
        this.retainMessage = retain;
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.error("Connection to mqtt broker lost", cause);
                closeMqttClient();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {}

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });

    }

    public static MessageProcessor newInstance(MqttConfig config, Consumer<byte[]> consumer) throws MqttException {
        MqttAsyncClient mqttClient = null;
        try {
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(false);
            connectOptions.setMaxInflight(config.getMaxInflight());
            connectOptions.setAutomaticReconnect(false); //Let's abort on connection errors
            connectOptions.setKeepAliveInterval(config.getKeepAliveInterval());

            if (config.hasAuthentication()) {
                connectOptions.setUserName(config.getUsername());
                connectOptions.setPassword(config.getPassword().toCharArray());
            }

            //Let's use memory persistance to optimize throughput.
            MemoryPersistence memoryPersistence = new MemoryPersistence();

            mqttClient = new MqttAsyncClient(config.getBroker(), config.getClientId(), memoryPersistence);

            log.info(String.format("Connecting to mqtt broker %s", config.getBroker()));
            IMqttToken token = mqttClient.connect(connectOptions, null, new IMqttActionListener() {
                public void onSuccess(IMqttToken asyncActionToken) {
                    log.info("Connected");
                }

                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    log.error("Connection failed: ", exception);
                }
            });
            token.waitForCompletion();
            log.info("Connection to MQTT finished");
        }
        catch (Exception e) {
            log.error("Error connecting to MQTT", e);
            if (mqttClient != null) {
                //Paho doesn't close the connection threads unless we force-close it.
                mqttClient.close(true);
            }
            throw e;
        }

        return new MessageProcessor(consumer, mqttClient, config.getMqttTopic(), config.getRetainMessage());
    }

    @Override
    public void handleMessage(final Message msg) throws Exception {
        try {
            final MqttMessage mqttMsg = new MqttMessage();
            mqttMsg.setQos(1);
            mqttMsg.setRetained(retainMessage);
            mqttMsg.setPayload(msg.getData());

            final String topicSuffix = msg.getProperty(TransitdataProperties.KEY_MQTT_TOPIC);

            mqttClient.publish(topicSuffix == null ? mqttTopic : mqttTopic + "/" + topicSuffix, mqttMsg, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //Ack Pulsar message
                    consumer.acknowledgeAsync(msg).thenRun(() -> {
                        log.debug("Mqtt message delivered");
                    });
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    List<String> topics = Arrays.asList(asyncActionToken.getTopics());
                    String msg = "Failed to send message [" + asyncActionToken.getMessageId() + "] to topics " + String.join(", ", topics);
                    log.error(msg, exception);
                }
            });
        }
        catch (Exception e) {
            log.error("Error publishing MQTT message, existing app", e);
            closeMqttClient();
            throw e;
        }
    }

    private void closeMqttClient() {
        try {
            //Paho doesn't close the connection threads unless we force-close it.
            mqttClient.disconnectForcibly(1000L, 1000L);
            mqttClient.close(true);
        }
        catch (Exception e) {
            log.error("Failed to close MQTT client connection", e);
        }
    }

    public boolean isMqttConnected() {
        if (mqttClient != null) {
            return mqttClient.isConnected();
        }
        return false;
    }
}
