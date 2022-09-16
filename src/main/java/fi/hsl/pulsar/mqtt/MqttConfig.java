package fi.hsl.pulsar.mqtt;

public class MqttConfig {

    private final String broker;
    private final String mqttTopic;
    private final String username;
    private final String password;

    private final boolean authentication;
    private final String clientId;
    private final int maxInflight;
    private final boolean retainMessage;
    private final int keepAliveInterval;

    public MqttConfig(String broker, String mqttTopic, String username, String password, boolean authentication,
                      String clientId, int maxInflight, boolean retainMessage, int keepAliveInterval) {

        this.broker = broker;
        this.mqttTopic = mqttTopic;
        this.username = username;
        this.password = password;
        this.authentication = authentication;
        this.clientId = clientId;
        this.maxInflight = maxInflight;
        this.retainMessage = retainMessage;
        this.keepAliveInterval = keepAliveInterval;
    }

    public String getBroker() {
        return this.broker;
    }

    public String getMqttTopic() {
        return mqttTopic;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean hasAuthentication() {
        return authentication;
    }

    public String getClientId() {
        return clientId;
    }

    public int getMaxInflight() {
        return maxInflight;
    }

    public boolean getRetainMessage() {
        return retainMessage;
    }

    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public static MqttConfigBuilder newBuilder() {
        return new MqttConfigBuilder();
    }
}
