package fi.haagahelia.stockmanager.exception;

public class ProductStockException extends Exception {

    public ProductStockException(String message) {
        super(message);
    }

    public ProductStockException(String message, Throwable cause) {
        super(message, cause);
    }
}
