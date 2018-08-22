package fi.hsl.transitdata.mqttsink;

import fi.hsl.common.pulsar.IMessageHandler;
import org.apache.pulsar.client.api.*;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        mqttClient.setCallback(new TripUpdateCallback());

        log.info(String.format("Connecting to mqtt broker %s", config.getBroker()));
        mqttClient.connect(connectOptions);

        this.mqttTopic = config.getMqttTopic();
    }

    @Override
    public void handleMessage(Message msg) throws Exception {
        try {
            MqttMessage mqttMsg = new MqttMessage();
            mqttMsg.setQos(1);
            mqttMsg.setPayload(msg.getData());
            mqttClient.publish(mqttTopic, mqttMsg, null, new TripUpdateSendListener(consumer, msg));
        }
        catch (MqttException e) {
            log.error("Error publishing MQTT message", e);
            //TODO should we throw?
        }
    }
}
