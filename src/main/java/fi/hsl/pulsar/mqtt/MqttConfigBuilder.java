package fi.hsl.pulsar.mqtt;

public class MqttConfigBuilder {

    private String broker;
    private String mqttTopic;
    private String username;
    private String password;
    private String clientId;
    private int maxInflight;

    public MqttConfigBuilder() {
        this.broker = null;
        this.mqttTopic = "gtfsrt/dev/fi/hsl/test";
        this.username = null;
        this.password = null;
        this.clientId = null;
        this.maxInflight = 2000;
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

    public MqttConfigBuilder setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public MqttConfigBuilder setMaxInflight(int maxInflight) {
        this.maxInflight = maxInflight;
        return this;
    }

    public MqttConfig build() {

        if (broker == null  || mqttTopic == null || username == null || password == null || clientId == null) {
            throw new IllegalArgumentException("Required field not set for MqttSinkConfid");
        }

        return new MqttConfig(broker, mqttTopic, username, password, clientId, maxInflight);
    }


}
