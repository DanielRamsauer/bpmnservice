package at.ac.tuwien.auto.bpmn_service.exception;

/**
 * @author Daniel Ramsauer
 * @since 23.09.2021, Do.
 */
public class BPMNServiceException extends Exception{
    public BPMNServiceException(String message) {
        super(message);
    }

    public BPMNServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public BPMNServiceException(Throwable cause) {
        super(cause);
    }
}
