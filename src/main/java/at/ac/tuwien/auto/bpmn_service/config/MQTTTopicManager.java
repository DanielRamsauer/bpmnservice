package at.ac.tuwien.auto.bpmn_service.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Daniel Ramsauer
 * @since 09.10.2021, Sa.
 */
@Component
public class MQTTTopicManager {
    @Value("${mqtt.topics.prefix.services}") @Getter private String prefixServices;
    @Value("${mqtt.topics.prefix.message}") @Getter private String prefixMessage;

    private String createServiceTopic(String postFix) {
        return prefixServices + "/" + postFix;
    }

    public String createServiceResponseTopic(String s) {
        return createServiceTopic(s) + "/response/#";
    }

    public String createServiceRequestTopic(String serviceName, String correlationKey) {
        return createServiceTopic(serviceName) + "/request/" + correlationKey;
    }

    public String createMessageTopic(String messageName) {
        return prefixMessage + "/" + messageName;
    }

    public String createMessageStartEventTopic(String serviceName) {
        return createServiceTopic(serviceName);
    }
}