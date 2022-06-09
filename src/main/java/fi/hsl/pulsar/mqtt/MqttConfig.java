package fi.hsl.pulsar.mqtt;

public class MqttConfig {

    private String broker;
    private String mqttTopic;
    private String username;
    private String password;

    private boolean authentication;
    private String clientId;
    private int maxInflight;
    private boolean retainMessage;

    public MqttConfig(String broker, String mqttTopic, String username, String password, boolean authentication,
                      String clientId, int maxInflight, boolean retainMessage) {

        this.broker = broker;
        this.mqttTopic = mqttTopic;
        this.username = username;
        this.password = password;
        this.authentication = authentication;
        this.clientId = clientId;
        this.maxInflight = maxInflight;
        this.retainMessage = retainMessage;
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

    public static MqttConfigBuilder newBuilder() {
        return new MqttConfigBuilder();
    }
}
