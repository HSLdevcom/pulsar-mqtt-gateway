package fi.hsl.transitdata.mqttsink;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TripUpdateSendListener implements IMqttActionListener {

    private static final Logger log = LoggerFactory.getLogger(TripUpdateSendListener.class);

    private Consumer<byte[]> consumer;
    private Message<byte[]> pulsarMessage;

    public TripUpdateSendListener(Consumer<byte[]> consumer, Message<byte[]> pulsarMessage) {
       this.consumer = consumer;
       this.pulsarMessage = pulsarMessage;
    }

    @Override
    public void onSuccess(IMqttToken iMqttToken) {

        consumer.acknowledgeAsync(pulsarMessage).thenRun(() -> {log.info("Mqtt message delivered");});

    }

    @Override
    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {

        log.error("Failure sending message: " + throwable.getMessage());

    }
}
