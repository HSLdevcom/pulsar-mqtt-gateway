package fi.hsl.pulsar.mqtt;

import fi.hsl.common.transitdata.TransitdataProperties;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pulsar message → MQTT publish, with the MQTT client mocked.
 */
@SuppressWarnings("unchecked")
public class MessageProcessorTest {

    private static final String BASE_TOPIC = "gtfsrt/dev/fi/hsl/tu";

    private Consumer<byte[]> consumer;
    private MqttAsyncClient mqtt;

    @Before
    public void mocks() {
        consumer = mock(Consumer.class);
        when(consumer.acknowledgeAsync(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));
        mqtt = mock(MqttAsyncClient.class);
    }

    private static Message<byte[]> message(byte[] data, String topicSuffix) {
        Message<byte[]> msg = mock(Message.class);
        when(msg.getData()).thenReturn(data);
        when(msg.getProperty(TransitdataProperties.KEY_MQTT_TOPIC)).thenReturn(topicSuffix);
        return msg;
    }

    private IMqttActionListener publishAndCapture(MessageProcessor processor, Message<byte[]> msg, String expectedTopic,
            ArgumentCaptor<MqttMessage> mqttMessage) throws Exception {
        processor.handleMessage(msg);
        ArgumentCaptor<IMqttActionListener> listener = ArgumentCaptor.forClass(IMqttActionListener.class);
        verify(mqtt).publish(eq(expectedTopic), mqttMessage.capture(), isNull(), listener.capture());
        return listener.getValue();
    }

    @Test
    public void publishesPayloadAsIsWithQos1ToBaseTopic() throws Exception {
        byte[] payload = {0, 1, 2, (byte) 0xff};
        MessageProcessor processor = new MessageProcessor(consumer, mqtt, BASE_TOPIC, false);
        ArgumentCaptor<MqttMessage> sent = ArgumentCaptor.forClass(MqttMessage.class);

        publishAndCapture(processor, message(payload, null), BASE_TOPIC, sent);

        assertArrayEquals(payload, sent.getValue().getPayload());
        assertEquals(1, sent.getValue().getQos());
        assertFalse(sent.getValue().isRetained());
    }

    @Test
    public void appendsTopicSuffixFromMessageProperty() throws Exception {
        MessageProcessor processor = new MessageProcessor(consumer, mqtt, BASE_TOPIC, false);

        publishAndCapture(processor, message(new byte[]{1}, "1234/5"), BASE_TOPIC + "/1234/5",
                ArgumentCaptor.forClass(MqttMessage.class));
    }

    @Test
    public void emptyTopicSuffixStillAddsSlash() throws Exception {
        MessageProcessor processor = new MessageProcessor(consumer, mqtt, BASE_TOPIC, false);

        publishAndCapture(processor, message(new byte[]{1}, ""), BASE_TOPIC + "/",
                ArgumentCaptor.forClass(MqttMessage.class));
    }

    @Test
    public void retainFlagFollowsConfig() throws Exception {
        MessageProcessor processor = new MessageProcessor(consumer, mqtt, BASE_TOPIC, true);
        ArgumentCaptor<MqttMessage> sent = ArgumentCaptor.forClass(MqttMessage.class);

        publishAndCapture(processor, message(new byte[]{1}, null), BASE_TOPIC, sent);

        assertTrue(sent.getValue().isRetained());
    }

    @Test
    public void acknowledgesOnlyAfterMqttDelivery() throws Exception {
        Message<byte[]> msg = message(new byte[]{1}, null);
        MessageProcessor processor = new MessageProcessor(consumer, mqtt, BASE_TOPIC, false);

        IMqttActionListener listener = publishAndCapture(processor, msg, BASE_TOPIC,
                ArgumentCaptor.forClass(MqttMessage.class));
        verify(consumer, never()).acknowledgeAsync(any(Message.class));

        listener.onSuccess(mock(IMqttToken.class));
        verify(consumer).acknowledgeAsync(msg);
    }

    @Test
    public void failedDeliveryIsNeitherAckedNorNackedAndKeepsTheClientOpen() throws Exception {
        Message<byte[]> msg = message(new byte[]{1}, null);
        MessageProcessor processor = new MessageProcessor(consumer, mqtt, BASE_TOPIC, false);
        IMqttActionListener listener = publishAndCapture(processor, msg, BASE_TOPIC,
                ArgumentCaptor.forClass(MqttMessage.class));

        IMqttToken token = mock(IMqttToken.class);
        when(token.getTopics()).thenReturn(new String[]{BASE_TOPIC});
        listener.onFailure(token, new MqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT));

        verify(consumer, never()).acknowledgeAsync(any(Message.class));
        verify(consumer, never()).negativeAcknowledge(any(Message.class));
        verify(mqtt, never()).close(anyBoolean());
    }

    @Test
    public void publishErrorClosesTheClientAndRethrows() throws Exception {
        MqttException error = new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        when(mqtt.publish(anyString(), any(MqttMessage.class), any(), any(IMqttActionListener.class))).thenThrow(error);
        MessageProcessor processor = new MessageProcessor(consumer, mqtt, BASE_TOPIC, false);

        try {
            processor.handleMessage(message(new byte[]{1}, null));
            fail("expected the publish exception");
        } catch (MqttException e) {
            assertSame(error, e);
        }
        verify(mqtt).disconnectForcibly(1000L, 1000L);
        verify(mqtt).close(true);
        verify(consumer, never()).acknowledgeAsync(any(Message.class));
    }

    @Test
    public void closeFailuresAreSwallowed() throws Exception {
        when(mqtt.publish(anyString(), any(MqttMessage.class), any(), any(IMqttActionListener.class)))
                .thenThrow(new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED));
        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION)).when(mqtt).disconnectForcibly(anyLong(),
                anyLong());
        MessageProcessor processor = new MessageProcessor(consumer, mqtt, BASE_TOPIC, false);

        try {
            processor.handleMessage(message(new byte[] { 1 }, null));
            fail("expected the publish exception");
        } catch (MqttException e) {
            assertEquals(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED, e.getReasonCode());
        }
        verify(mqtt, never()).close(anyBoolean());
    }

    @Test
    public void connectionLostClosesTheClient() throws Exception {
        new MessageProcessor(consumer, mqtt, BASE_TOPIC, false);
        ArgumentCaptor<MqttCallback> callback = ArgumentCaptor.forClass(MqttCallback.class);
        verify(mqtt).setCallback(callback.capture());

        callback.getValue().connectionLost(new RuntimeException("broker gone"));

        verify(mqtt).disconnectForcibly(1000L, 1000L);
        verify(mqtt).close(true);
    }

    @Test
    public void healthFollowsMqttConnection() {
        MessageProcessor processor = new MessageProcessor(consumer, mqtt, BASE_TOPIC, false);

        when(mqtt.isConnected()).thenReturn(true);
        assertTrue(processor.isMqttConnected());
        when(mqtt.isConnected()).thenReturn(false);
        assertFalse(processor.isMqttConnected());
    }
}
