package fi.haagahelia.stockmanager.exception;

public class UnknownOrderException extends Exception{

    public UnknownOrderException(String message) {
        super(message);
    }

    public UnknownOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
