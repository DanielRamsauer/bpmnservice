package at.ac.tuwien.auto.bpmn_service.service;

import at.ac.tuwien.auto.bpmn_service.config.ConfigurationManager;
import at.ac.tuwien.auto.bpmn_service.exception.MqttPublishException;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Random;

/**
 * @author Daniel Ramsauer
 * @since 23.09.2021, Do.
 */
@Service
public class MQTTPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(MQTTPublisher.class);
    private MqttAsyncClient mqttAsyncClient;
    private final MemoryPersistence memoryPersistence = new MemoryPersistence();
    private final ConfigurationManager cm;

    public MQTTPublisher(ConfigurationManager cm){this.cm = cm;}

    @EventListener(ApplicationReadyEvent.class)
    private void initManager() {
        try {
            createAndConnectClient();
        } catch (MqttException | IOException | GeneralSecurityException | InterruptedException e) {
            LOG.error("Could not establish MQTT connection", e);
            System.exit(1);
        }
    }

    private void createAndConnectClient() throws MqttException, IOException, GeneralSecurityException, InterruptedException {
        mqttAsyncClient = new MqttAsyncClient(cm.getMqttBrokerUrl(), "BPMN-Services-Publisher" + (new Random()).nextInt(500), memoryPersistence);
        MqttConnectOptions options = cm.getMqttConnectionOptions();

        LOG.info("(MQTT Publisher) Connecting to: " + cm.getMqttBrokerUrl());
        mqttAsyncClient.connect(options);
        while (!mqttAsyncClient.isConnected()) {
            LOG.info("(MQTT Subscriber) connecting ...");
            Thread.sleep(200);
        }
    }

    public synchronized void publish(String topic) throws MqttPublishException {
        try {
            if(cm.isMqttDebugging()) LOG.info("Publishing empty MQTT message on topic: " + topic);
            mqttAsyncClient.publish(topic, new MqttMessage("".getBytes()));
        } catch (MqttException e) {
            throw new MqttPublishException(e.getMessage(), e);
        }
    }

    public synchronized void publish(String topic, JSONObject message) throws MqttPublishException {
        try {
            if(cm.isMqttDebugging()) LOG.info("Publishing MQTT message \"" + message.toString() + "\" on topic: " + topic);
            mqttAsyncClient.publish(topic, new MqttMessage(message.toString().getBytes()));
        } catch (MqttException e) {
            throw new MqttPublishException(e.getMessage(), e);
        }
    }

    public synchronized void publish(String topic, String message) throws MqttPublishException {
        try {
            if(cm.isMqttDebugging()) LOG.info("Publishing MQTT message \"" + message + "\" on topic: " + topic);
            mqttAsyncClient.publish(topic, new MqttMessage(message.getBytes()));
        } catch (MqttException e) {
            throw new MqttPublishException(e.getMessage(), e);
        }
    }
}
