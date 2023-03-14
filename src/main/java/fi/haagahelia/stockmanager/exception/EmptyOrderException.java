package fi.haagahelia.stockmanager.exception;

public class EmptyOrderException extends Exception {

    public EmptyOrderException(String message) {
        super(message);
    }

    public EmptyOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
