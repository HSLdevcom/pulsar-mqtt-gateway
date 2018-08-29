package fi.hsl.pulsar.mqtt;

public class MqttConfig {

    private String broker;
    private String mqttTopic;
    private String username;
    private String password;
    private String clientId;
    private int  maxInflight;

    public MqttConfig(String broker, String mqttTopic, String username, String password,
                      String clientId, int maxInflight) {

        this.broker = broker;
        this.mqttTopic = mqttTopic;
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.maxInflight = maxInflight;
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

    public String getClientId() {
        return clientId;
    }

    public int getMaxInflight() {
        return maxInflight;
    }

    public static MqttConfigBuilder newBuilder() {
        return new MqttConfigBuilder();
    }
}
