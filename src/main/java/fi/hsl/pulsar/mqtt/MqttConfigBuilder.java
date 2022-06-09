package fi.hsl.pulsar.mqtt;

public class MqttConfigBuilder {

    private String broker;
    private String mqttTopic;
    private String username;
    private String password;
    private boolean authentication;
    private String clientId;
    private int maxInflight;
    private boolean retainMessage;

    public MqttConfigBuilder() {
    }

    public MqttConfigBuilder setBroker(String broker) {
        this.broker = broker;
        return this;
    }

    public MqttConfigBuilder setMqttTopic(String mqttTopic) {
        this.mqttTopic = mqttTopic;
        return this;
    }

    public MqttConfigBuilder setUsername(String username) {
        this.username = username;
        return this;
    }

    public MqttConfigBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public MqttConfigBuilder setAuthentication(boolean authentication) {
        this.authentication = authentication;
        return this;
    }

    public MqttConfigBuilder setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public MqttConfigBuilder setMaxInflight(int maxInflight) {
        this.maxInflight = maxInflight;
        return this;
    }

    public MqttConfigBuilder setRetainMessage(boolean retain) {
        this.retainMessage = retain;
        return this;
    }

    public MqttConfig build() {

        if (broker == null  || mqttTopic == null || (authentication && (username == null || password == null)) || clientId == null) {
            throw new IllegalArgumentException("Required field not set for MqttConfig");
        }

        return new MqttConfig(broker, mqttTopic, username, password, authentication, clientId, maxInflight, retainMessage);
    }


}
