package at.ac.tuwien.auto.bpmn_service.service.workers;

import at.ac.tuwien.auto.bpmn_service.exception.MqttPublishException;
import at.ac.tuwien.auto.bpmn_service.util.JSONUtil;
import at.ac.tuwien.auto.bpmn_service.config.ConfigurationManager;
import at.ac.tuwien.auto.bpmn_service.config.MQTTTopicManager;
import at.ac.tuwien.auto.bpmn_service.service.MQTTPublisher;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.zeebe.spring.client.annotation.ZeebeWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Ramsauer
 * @since 07.10.2021, Do.
 */
@Service
public class MQTTWorker {
    private final static Logger LOG = LoggerFactory.getLogger(MQTTWorker.class);

    private final MQTTPublisher mqttPublisher;
    private final MQTTTopicManager mqttTopicManager;
    private final ConfigurationManager cm;

    public MQTTWorker(MQTTPublisher mqttPublisher, MQTTTopicManager mqttTopicManager, ConfigurationManager cm) {
        this.mqttPublisher = mqttPublisher;
        this.mqttTopicManager = mqttTopicManager;
        this.cm = cm;
    }

    @ZeebeWorker(type = "mqttWorker")
    public void worker(final JobClient client, final ActivatedJob job) {
        String topic = job.getCustomHeaders().get("topic");
        Map<String, Object> variables = job.getVariablesAsMap();
        // This is the case, if the services is called after a message start event
        if (!variables.containsKey(cm.getBpmnKeyIdentifier())) {
            variables.put(cm.getBpmnKeyIdentifier(), System.currentTimeMillis());
        }
        String correlationKey = variables.get(cm.getBpmnKeyIdentifier()).toString();
        String input = job.getCustomHeaders().get("input");
        if (input != null) {
            if (!input.equals("")) {
                Map<String, Object> new_variables = new HashMap<>();
                for (String s: input.split(",")){
                    Object obj = variables.get(s.trim());
                    if (obj != null) {
                        new_variables.put(s.trim(), obj);
                    } else {
                        LOG.error("Activity \"" + job.getElementId() + " \" with topic \"" + topic + "\" in process "+
                                job.getBpmnProcessId() + " has an input variable in the header section set which could " +
                                "not be found in the scope of variables of the flow, " +
                                "is it correctly set in the BPMN workflow as input?");
                    }
                }
                variables = new_variables;
            }
        }

        Map<String, Object> finalVariables = variables;
        client.newCompleteCommand(job.getKey())
                .variables(variables)
                .send().handleAsync((completeJobResponse, throwable) -> publishMqttMessage(topic, finalVariables, correlationKey))
                .whenComplete((s, throwable) -> {
                    if (throwable != null) {
                        LOG.error("Error while publishing message to service layer and processDefinitionKey='{}', " +
                                "bpmnProcessId='{}'", job.getProcessDefinitionKey(), job.getBpmnProcessId(), throwable);
                    } else {
                        LOG.info("Completed sending data to service layer processDefinitionKey='{}', " +
                                "bpmnProcessId='{}'", job.getProcessDefinitionKey(), job.getBpmnProcessId());
                    }
                });
    }

    private String publishMqttMessage(String topic, Map<String, Object> variables, String correlationKey) {
//        String correlationKey = variables.remove("correlationKey").toString();
//        JSONObject jsonObject = getJsonObjectOfVariableMap(variables);
        try {
            mqttPublisher.publish(mqttTopicManager.createServiceRequestTopic(topic, correlationKey), JSONUtil.getJsonObjectOfVariableMap(variables));
        } catch (MqttPublishException e) {
            throw new RuntimeException(e);
        }
        return "Published message";
    }
}
