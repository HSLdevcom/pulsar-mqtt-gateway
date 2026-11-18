package fi.hsl.pulsar.mqtt;

import org.junit.Test;

import static org.junit.Assert.*;

public class MqttConfigBuilderTest {

    private static MqttConfigBuilder complete() {
        return MqttConfig.newBuilder().setBroker("tcp://broker:1883").setMqttTopic("gtfsrt/dev/fi/hsl/tu")
                .setClientId("client").setMaxInflight(100).setRetainMessage(true).setKeepAliveInterval(15);
    }

    @Test
    public void buildsAllFields() {
        MqttConfig config = complete().setAuthentication(true).setUsername("user").setPassword("secret").build();

        assertEquals("tcp://broker:1883", config.getBroker());
        assertEquals("gtfsrt/dev/fi/hsl/tu", config.getMqttTopic());
        assertEquals("client", config.getClientId());
        assertEquals(100, config.getMaxInflight());
        assertTrue(config.getRetainMessage());
        assertEquals(15, config.getKeepAliveInterval());
        assertTrue(config.hasAuthentication());
        assertEquals("user", config.getUsername());
        assertEquals("secret", config.getPassword());
    }

    @Test
    public void withoutAuthenticationCredentialsAreOptional() {
        MqttConfig config = complete().build();

        assertFalse(config.hasAuthentication());
        assertNull(config.getUsername());
        assertNull(config.getPassword());
    }

    @Test
    public void unsetNumericAndFlagFieldsDefaultToZeroAndFalse() {
        MqttConfig config = MqttConfig.newBuilder().setBroker("b").setMqttTopic("t").setClientId("c").build();

        assertEquals(0, config.getMaxInflight());
        assertEquals(0, config.getKeepAliveInterval());
        assertFalse(config.getRetainMessage());
    }

    @Test
    public void emptyClientIdIsAccepted() {
        // Only null is rejected; environment.conf defaults clientId to ""
        assertEquals("", complete().setClientId("").build().getClientId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingBrokerIsRejected() {
        complete().setBroker(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingTopicIsRejected() {
        complete().setMqttTopic(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingClientIdIsRejected() {
        complete().setClientId(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void authenticationWithoutUsernameIsRejected() {
        complete().setAuthentication(true).setPassword("secret").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void authenticationWithoutPasswordIsRejected() {
        complete().setAuthentication(true).setUsername("user").build();
    }

    @Test
    public void credentialsWithoutAuthenticationAreKept() {
        MqttConfig config = complete().setUsername("user").setPassword("secret").build();

        assertFalse(config.hasAuthentication());
        assertEquals("user", config.getUsername());
    }
}
