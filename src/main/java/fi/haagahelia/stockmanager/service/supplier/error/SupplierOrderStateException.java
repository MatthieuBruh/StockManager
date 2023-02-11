package fi.haagahelia.stockmanager.service.supplier.error;

public class SupplierOrderStateException extends Exception{

    public SupplierOrderStateException(String message) {
        super(message);
    }

    public SupplierOrderStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
