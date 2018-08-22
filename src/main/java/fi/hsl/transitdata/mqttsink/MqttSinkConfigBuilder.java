package fi.hsl.transitdata.mqttsink;

public class MqttSinkConfigBuilder {

    private String broker;
    private String mqttTopic;
    private String username;
    private String password;
    private String clientId;
    private int maxInflight;

    public MqttSinkConfigBuilder() {
        this.broker = null;
        this.mqttTopic = "gtfsrt/dev/fi/hsl/test";
        this.username = null;
        this.password = null;
        this.clientId = null;
        this.maxInflight = 2000;
    }

    public MqttSinkConfigBuilder setBroker(String broker) {
        this.broker = broker;
        return this;
    }

    public MqttSinkConfigBuilder setMqttTopic(String mqttTopic) {
        this.mqttTopic = mqttTopic;
        return this;
    }

    public MqttSinkConfigBuilder setUsername(String username) {
        this.username = username;
        return this;
    }

    public MqttSinkConfigBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public MqttSinkConfigBuilder setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public MqttSinkConfigBuilder setMaxInflight(int maxInflight) {
        this.maxInflight = maxInflight;
        return this;
    }

    public MqttSinkConfig build() {

        if (broker == null  || mqttTopic == null || username == null || password == null || clientId == null) {
            throw new IllegalArgumentException("Required field not set for MqttSinkConfid");
        }

        return new MqttSinkConfig(broker, mqttTopic, username, password, clientId, maxInflight);
    }


}
