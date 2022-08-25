package at.ac.tuwien.auto.bpmn_service.config;

import at.ac.tuwien.auto.bpmn_service.util.SslUtil;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Objects;

/**
 * @author Daniel Ramsauer
 * @since 23.09.2021, Do.
 */
@Component
public class ConfigurationManager {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManager.class);
    private final Environment env;
    private final ResourceLoader resourceLoader;

    @Value("${mqtt.broker.ip}") private String mqttBrokerIp;
    @Value("${mqtt.broker.port}") private int mqttBrokerPort;
    @Value("${mqtt.broker.qos}") @Getter private int mqttQos;
    @Value("${mqtt.broker.timeout}")  @Getter private int mqttTimeout;
    @Value("${mqtt.broker.username}") private String mqttUserName;
    @Value("${mqtt.broker.password}") private String mqttPassword;
    private Boolean mqttUsernamePasswordFlag = false;
    private Boolean mqttCertificateFlag = false;
    @Getter private boolean mqttDebugging;

    @Value("${bpmn.key_identifier}")@Getter private String bpmnKeyIdentifier;

    @Getter @Setter private String[] serviceNames;
    @Getter @Setter private String[] bpmnNewInstanceNames;

    private Path caFile;
    private Path clientCrt;
    private Path clientKey;

    public ConfigurationManager(Environment env, ResourceLoader resourceLoader) {
        this.env = env;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    private void initConfig(){
        mqttUsernamePasswordFlag = parseBooleanValueFromConfigPath("mqtt.broker.useUsernamePassword");
        mqttCertificateFlag = parseBooleanValueFromConfigPath("mqtt.broker.useCertificates");
        mqttDebugging = parseBooleanValueFromConfigPath("mqtt.debugging");
        try {
            readMQTTCertificates();
        } catch (IOException e) {
            LOG.error("Could not read certificate files for configuring MQTT ssl connection", e);
            System.exit(1);
        }
    }

    private boolean parseBooleanValueFromConfigPath(String path) {
        return Boolean.parseBoolean(Objects.requireNonNull(env.getProperty(path)));
    }

    private void readMQTTCertificates() throws IOException {
        if (mqttCertificateFlag) {
            caFile = getResourcePath("classpath:config/certs/ca.crt");
            clientCrt = getResourcePath("classpath:config/certs/client.crt");
            clientKey = getResourcePath("classpath:config/certs/client.key");
        }
    }

    private Path getResourcePath(String path) throws IOException {
        return resourceLoader.getResource(path).getFile().toPath();
    }

    /**
     * tcp:// for non encrypted
     * ssl:// for encrypted communication
     * @return constructed url for client to connect to (e.g. tcp://127.0.0.1:1883)
     */
    public String getMqttBrokerUrl(){
        if (mqttCertificateFlag) {
            return "ssl://" + this.mqttBrokerIp + ":" + this.mqttBrokerPort;
        } else {
            return "tcp://" + this.mqttBrokerIp + ":" + this.mqttBrokerPort;
        }
    }

    /**
     * Configures the mqtt connection with username/password, automatic reconnect, timeout and tls support
     * @return the options set as configured
     * @throws IOException, GeneralSecurityException if an error while setting up the options occurs
     */
    public MqttConnectOptions getMqttConnectionOptions() throws IOException, GeneralSecurityException {
        MqttConnectOptions options = new MqttConnectOptions();
        if (mqttUsernamePasswordFlag) {
            options.setUserName(mqttUserName);
            options.setPassword(mqttPassword.toCharArray());
        }
        if(mqttCertificateFlag) options.setSocketFactory(SslUtil.getSocketFactory(caFile, clientCrt, clientKey, ""));
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(mqttTimeout);
        return options;
    }
}
