package fi.hsl.pulsar.mqtt;

import fi.hsl.common.pulsar.IMessageHandler;
import org.apache.pulsar.client.api.*;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class MessageProcessor implements IMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MessageProcessor.class);

    private Consumer<byte[]> consumer;
    private MqttAsyncClient mqttClient;
    private String mqttTopic;

    public MessageProcessor(MqttSinkConfig config, Consumer<byte[]> consumer) throws MqttException {

        this.consumer = consumer;

        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setCleanSession(false);
        connectOptions.setMaxInflight(config.getMaxInflight());
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setUserName(config.getUsername());
        connectOptions.setPassword(config.getPassword().toCharArray());

        MemoryPersistence memoryPersistence = new MemoryPersistence();

        this.mqttClient = new MqttAsyncClient(config.getBroker(), config.getClientId(), memoryPersistence);
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.error("Connection to mqtt broker lost: " + cause.getMessage(), cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {}

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });

        log.info(String.format("Connecting to mqtt broker %s", config.getBroker()));
        mqttClient.connect(connectOptions);

        this.mqttTopic = config.getMqttTopic();
    }

    @Override
    public void handleMessage(final Message msg) throws Exception {
        try {
            final MqttMessage mqttMsg = new MqttMessage();
            mqttMsg.setQos(1);
            mqttMsg.setPayload(msg.getData());
            mqttClient.publish(mqttTopic, mqttMsg, null, new IMqttActionListener() {
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
        catch (MqttException e) {
            log.error("Error publishing MQTT message", e);
        }
    }
}
