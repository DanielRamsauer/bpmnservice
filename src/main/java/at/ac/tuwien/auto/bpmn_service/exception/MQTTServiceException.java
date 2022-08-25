package at.ac.tuwien.auto.bpmn_service.exception;

/**
 * @author Daniel Ramsauer
 * @since 18.10.2021, Mo.
 */
public class MQTTServiceException extends BPMNServiceException {
    public MQTTServiceException(String message) {
        super(message);
    }

    public MQTTServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public MQTTServiceException(Throwable cause) {
        super(cause);
    }
}
