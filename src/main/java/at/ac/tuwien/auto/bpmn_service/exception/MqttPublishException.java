package at.ac.tuwien.auto.bpmn_service.exception;

/**
 * @author Daniel Ramsauer
 * @since 23.09.2021, Do.
 */
public class MqttPublishException extends BPMNServiceException {
    public MqttPublishException(String message) {
        super(message);
    }

    public MqttPublishException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqttPublishException(Throwable cause) {
        super(cause);
    }
}
