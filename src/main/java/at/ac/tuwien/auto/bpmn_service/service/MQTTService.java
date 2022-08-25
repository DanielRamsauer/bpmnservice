package at.ac.tuwien.auto.bpmn_service.service;

import at.ac.tuwien.auto.bpmn_service.config.ConfigurationManager;
import at.ac.tuwien.auto.bpmn_service.config.MQTTTopicManager;
import at.ac.tuwien.auto.bpmn_service.exception.MQTTServiceException;
import at.ac.tuwien.auto.bpmn_service.util.JSONUtil;
import io.zeebe.spring.client.ZeebeClientLifecycle;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Ramsauer
 * @since 23.09.2021, Do.
 */
@Service
public class MQTTService {
    private static final Logger LOG = LoggerFactory.getLogger(MQTTService.class);
    private final ZeebeClientLifecycle client;
    private final ConfigurationManager cm;
    private final MQTTTopicManager mqttTopicManager;

    public MQTTService(ZeebeClientLifecycle client, ConfigurationManager cm, MQTTTopicManager mqttTopicManager) throws MQTTServiceException {
        this.client = client;
        this.cm = cm;
        this.mqttTopicManager = mqttTopicManager;
    }

    /**
     * Handles a new message from the MQTT broker and differentiates between messages for services and sensors
     *
     * @param topic   topic of the received MQTT message
     * @param payload the payload of the MQTT message
     */
    public void handleNewMqttMessage(String topic, String payload) {
        if (topic.contains(mqttTopicManager.getPrefixServices())) {
            // from services
            handleMessageForService(topic, payload);
        } else if (topic.contains(mqttTopicManager.getPrefixMessage())) {
            // from sendMessageWorker to start any other process
            handleNewWorkflowMessage(topic, payload);
        }
    }

    private void handleNewWorkflowMessage(String topic, String payload) {
        String globalMessageName = topic.substring(mqttTopicManager.getPrefixMessage().length() + 1);
        Map<String, Object> variables = getVariableMapWithCurrentTimestampAsCorrelationKey();
        variables.putAll(getPayloadAsStringObjectMap(new JSONObject(payload)));
//        createNewInstanceByGlobalMessageName(message, variables);
        publishNewMessageToZeebe(globalMessageName, variables);
    }

    private Map<String, Object> getVariableMapWithCurrentTimestampAsCorrelationKey() {
        Map<String, Object> variables = new HashMap<>();
        variables.put(cm.getBpmnKeyIdentifier(), System.currentTimeMillis());
        return variables;
    }

    private void handleMessageForService(String topic, String payload) {
        try {
            payload = payload.trim().replaceAll("\r", "").replaceAll("\n", "");
            boolean validJSON = JSONUtil.isJSONValid(payload);

            // start a new instance
            String globalMessageName = getServiceNameFromTopic(topic);
            if (Arrays.asList(cm.getBpmnNewInstanceNames()).contains(globalMessageName)) {
                Map<String, Object> variables = getVariableMapWithCurrentTimestampAsCorrelationKey();
                if (!payload.isEmpty()) {
                    if (validJSON) {
                        variables.putAll(getPayloadAsStringObjectMap(new JSONObject(payload)));
                    } else {
                        variables.put("value", payload);
                    }
                }
                publishNewMessageToZeebe(globalMessageName, variables);
            } else if (Arrays.asList(cm.getServiceNames()).contains(globalMessageName)) {
                String correlationKey = getCorrelationKey(topic);
                Map<String, Object> variables = new HashMap<>();
                variables.put(cm.getBpmnKeyIdentifier(), correlationKey);
                if (!payload.isEmpty()) {
                    if (validJSON) {
                        variables.putAll(getPayloadAsStringObjectMap(new JSONObject(payload)));
                    } else {
                        variables.put("value", payload);
                    }
                }
                publishNewMessageToZeebe(globalMessageName, variables, correlationKey);
            }

        } catch (JSONException e) {
            LOG.error("Error while parsing mqtt message to json", e);
        } catch (MQTTServiceException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private String getCorrelationKey(String topic) {
        return topic.substring(topic.lastIndexOf("/") + 1);
    }

    private String getServiceNameFromTopic(String topic) throws MQTTServiceException {
        String topicWithoutPrefix = topic.substring(mqttTopicManager.getPrefixServices().length() + 1);
        if (Arrays.asList(cm.getBpmnNewInstanceNames()).contains(topicWithoutPrefix)) {
            return topicWithoutPrefix;
        } else {
            String serviceName = getStringUntilFirstSlash(topicWithoutPrefix);
            if (!Arrays.asList(cm.getServiceNames()).contains(serviceName)) {
                throw new MQTTServiceException("Service of topic \"" + topic + "\" does not match any service defined within the BPMN-Service configuration");
            }
            return serviceName;
        }
    }

    private String getStringUntilFirstSlash(String topicWithoutPrefix) {
        return topicWithoutPrefix.substring(0, topicWithoutPrefix.indexOf("/"));
    }

    private Map<String, Object> getPayloadAsStringObjectMap(JSONObject jsonObject) {
        Map<String, Object> variables = new HashMap<>();
        for (String s : jsonObject.keySet()) {
            variables.put(s, jsonObject.get(s));
        }
        return variables;
    }

    private void publishNewMessageToZeebe(String globalMessageName, Map<String, Object> variables) {
        publishNewMessageToZeebe(globalMessageName, variables, variables.get(cm.getBpmnKeyIdentifier()).toString());
    }

    private void publishNewMessageToZeebe(String globalMessageName, Map<String, Object> variables, String correlationKey) {
        client.newPublishMessageCommand()
                .messageName(globalMessageName)
                .correlationKey(correlationKey)
                .variables(variables)
                .send().join();
    }
}