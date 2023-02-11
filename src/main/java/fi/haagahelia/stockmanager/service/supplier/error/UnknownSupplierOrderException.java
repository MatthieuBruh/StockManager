package fi.haagahelia.stockmanager.service.supplier.error;

public class UnknownSupplierOrderException extends Exception{

    public UnknownSupplierOrderException(String message) {
        super(message);
    }

    public UnknownSupplierOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
