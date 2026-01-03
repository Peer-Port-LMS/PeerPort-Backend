package peerport.backend.exceptions;

public class FailedToParseFormDataException extends RuntimeException {
    public FailedToParseFormDataException(String message) {
        super(message);
    }
}
