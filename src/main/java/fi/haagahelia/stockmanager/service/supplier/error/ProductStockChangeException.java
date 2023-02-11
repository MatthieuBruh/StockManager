package fi.haagahelia.stockmanager.service.supplier.error;

public class ProductStockChangeException extends Exception {

    public ProductStockChangeException(String message) {
        super(message);
    }

    public ProductStockChangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
