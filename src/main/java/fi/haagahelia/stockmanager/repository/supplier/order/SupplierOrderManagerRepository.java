package fi.haagahelia.stockmanager.repository.supplier.order;

import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import fi.haagahelia.stockmanager.exception.ProductStockException;
import fi.haagahelia.stockmanager.exception.OrderStateException;
import fi.haagahelia.stockmanager.exception.UnknownOrderException;
import jakarta.transaction.Transactional;

public interface SupplierOrderManagerRepository {
    @Transactional
    SupplierOrder sendOrderById(Long orderId) throws UnknownOrderException, OrderStateException;

    @Transactional
    SupplierOrder receiveOrderById(Long orderId)
            throws UnknownOrderException, ProductStockException, OrderStateException;

    @Transactional
    SupplierOrder cancelReceiveOrder(Long orderId)
            throws UnknownOrderException, ProductStockException, OrderStateException;
}
