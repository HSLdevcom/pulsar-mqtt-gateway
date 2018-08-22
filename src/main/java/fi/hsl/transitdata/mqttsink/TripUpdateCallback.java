package fi.hsl.transitdata.mqttsink;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TripUpdateCallback implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(TripUpdateCallback.class);

    public TripUpdateCallback() {
    }

    @Override
    public void connectionLost(Throwable throwable) {
        log.error("Connection to mqtt broker lost: " + throwable.getMessage());
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
}
