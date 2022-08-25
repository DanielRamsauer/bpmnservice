package at.ac.tuwien.auto.bpmn_service.service;

import at.ac.tuwien.auto.bpmn_service.config.ConfigurationManager;
import at.ac.tuwien.auto.bpmn_service.config.MQTTTopicManager;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
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
public class MQTTSubscriber implements MqttCallback{
    private static final Logger LOG = LoggerFactory.getLogger(MQTTSubscriber.class);

    private final ConfigurationManager cm;
    private final MQTTService mqttService;
    private final MQTTTopicManager mqttTopicManager;
    private MemoryPersistence memoryPersistence = new MemoryPersistence();
    private MqttAsyncClient mqttAsyncClient;

    public MQTTSubscriber(ConfigurationManager cm, MQTTService mqttService, MQTTTopicManager mqttTopicManager) {
        this.cm = cm;
        this.mqttService = mqttService;
        this.mqttTopicManager = mqttTopicManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    private void initManager(){
        connectSubscriber();
    }

    private void connectSubscriber() {
        try {
            createAndConnectMqttClient();
            mqttAsyncClient.setCallback(this);
            subscribeToTopics();
        } catch (MqttException | IOException | GeneralSecurityException | InterruptedException e){
            LOG.error("Could not establish MQTT connection", e);
            System.exit(1);
        }
    }

    private void createAndConnectMqttClient() throws MqttException, IOException, GeneralSecurityException, InterruptedException {
        mqttAsyncClient = new MqttAsyncClient(cm.getMqttBrokerUrl(), "BPMN-Service-Subscriber" + (new Random()).nextInt(500), memoryPersistence);
        MqttConnectOptions options = cm.getMqttConnectionOptions();
        LOG.info("(MQTT Subscriber) Connecting to: " + cm.getMqttBrokerUrl());
        mqttAsyncClient.connect(options);
        while (!mqttAsyncClient.isConnected()) {
            LOG.info("(MQTT Subscriber) connecting ...");
            Thread.sleep(200);
        }
    }

    private void subscribeToTopics() throws MqttException {
        for (String s : cm.getServiceNames()) {
            mqttAsyncClient.subscribe(mqttTopicManager.createServiceResponseTopic(s), cm.getMqttQos());
            LOG.info("subscribed to topic: " + mqttTopicManager.createServiceResponseTopic(s));
        }
        for (String s : cm.getBpmnNewInstanceNames()) {
            mqttAsyncClient.subscribe(mqttTopicManager.createMessageStartEventTopic(s), cm.getMqttQos());
            LOG.info("subscribed to topic: " + mqttTopicManager.createMessageStartEventTopic(s));
        }
//        mqttAsyncClient.subscribe(mqttTopicManager.getTopicSensors(), cm.getMqttQos());
//        LOG.info("subscribed to topic: " + mqttTopicManager.getTopicSensors());
//        mqttAsyncClient.subscribe(mqttTopicManager.getPrefixTopicMessage()+"/#", cm.getMqttQos());
//        LOG.info("subscribed to topic: " + mqttTopicManager.getTopicSensors()+"/#");
    }

    @Override
    public void connectionLost(Throwable throwable) {
        LOG.error("Connection to MQTT-Broker lost", throwable);
        try {
            this.createAndConnectMqttClient();
        } catch (MqttException | IOException | GeneralSecurityException | InterruptedException e) {
            LOG.error("Could not establish connection with MQTT-Broker again", e);
        }
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        mqttService.handleNewMqttMessage(s, new String(mqttMessage.getPayload()));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
}
