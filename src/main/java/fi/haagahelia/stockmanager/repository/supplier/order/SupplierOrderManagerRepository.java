package fi.haagahelia.stockmanager.repository.supplier.order;

import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import fi.haagahelia.stockmanager.service.supplier.error.ProductStockChangeException;
import fi.haagahelia.stockmanager.service.supplier.error.SupplierOrderStateException;
import fi.haagahelia.stockmanager.service.supplier.error.UnknownSupplierOrderException;
import jakarta.transaction.Transactional;

public interface SupplierOrderManagerRepository {
    @Transactional
    SupplierOrder sendOrderById(Long orderId) throws UnknownSupplierOrderException, SupplierOrderStateException;

    @Transactional
    SupplierOrder receiveOrderById(Long orderId)
            throws UnknownSupplierOrderException, ProductStockChangeException, SupplierOrderStateException;
}
