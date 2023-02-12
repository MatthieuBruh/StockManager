package fi.haagahelia.stockmanager.repository.customer.order;

import fi.haagahelia.stockmanager.exception.ProductStockException;
import fi.haagahelia.stockmanager.exception.OrderStateException;
import fi.haagahelia.stockmanager.exception.UnknownOrderException;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import jakarta.transaction.Transactional;

public interface CustomerOrderManagerRepository {

    @Transactional
    CustomerOrder customerOrderShipment(Long orderId)
            throws UnknownOrderException, ProductStockException, OrderStateException;

    @Transactional
    CustomerOrder customerOrderShipmentCancel(Long orderId) throws UnknownOrderException, OrderStateException, ProductStockException;
}
