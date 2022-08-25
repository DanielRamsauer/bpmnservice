package at.ac.tuwien.auto.bpmn_service.service.workers;

import at.ac.tuwien.auto.bpmn_service.exception.MqttPublishException;
import at.ac.tuwien.auto.bpmn_service.util.JSONUtil;
import at.ac.tuwien.auto.bpmn_service.config.MQTTTopicManager;
import at.ac.tuwien.auto.bpmn_service.service.MQTTPublisher;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.zeebe.spring.client.annotation.ZeebeWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Ramsauer
 * @since 11.11.2021, Do.
 */
public class SendMessageWorker {
    private final static Logger LOG = LoggerFactory.getLogger(SendMessageWorker.class);

    private final MQTTPublisher mqttPublisher;
    private final MQTTTopicManager mqttTopicManager;

    public SendMessageWorker(MQTTPublisher mqttPublisher, MQTTTopicManager mqttTopicManager) {
        this.mqttPublisher = mqttPublisher;
        this.mqttTopicManager = mqttTopicManager;
    }

    @ZeebeWorker(type = "sendMessageWorker")
    public void worker(final JobClient client, final ActivatedJob job) {
        String toSendMessage = job.getCustomHeaders().get("topic");
        Map<String, Object> variables = job.getVariablesAsMap();
        String input = job.getCustomHeaders().get("input");
        if (input != null) {
            if (!input.equals("")) {
                Map<String, Object> new_variables = new HashMap<>();
                for (String s: input.split(",")){
                    Object obj = variables.get(s.trim());
                    if (obj != null) {
                        new_variables.put(s.trim(), obj);
                    } else {
                        LOG.error("Activity \"" + job.getElementId() + " \" with topic \"" + toSendMessage + "\" in process "+
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
                .send().handleAsync((completeJobResponse, throwable) -> publishMqttMessage(toSendMessage, finalVariables))
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

    private String publishMqttMessage(String toSendMessage, Map<String, Object> variables) {
        try {
            mqttPublisher.publish(mqttTopicManager.createMessageTopic(toSendMessage), JSONUtil.getJsonObjectOfVariableMap(variables));
        } catch (MqttPublishException e) {
            throw new RuntimeException(e);
        }
        return "Published message";
    }
}
