package fi.haagahelia.stockmanager.service.supplier;

import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrderLine;
import fi.haagahelia.stockmanager.repository.supplier.order.SupplierOrderManagerRepository;
import fi.haagahelia.stockmanager.service.supplier.error.ProductStockChangeException;
import fi.haagahelia.stockmanager.service.supplier.error.SupplierOrderStateException;
import fi.haagahelia.stockmanager.service.supplier.error.UnknownSupplierOrderException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;


@Slf4j
@Repository
public class SupplierOrderManagerImpl implements SupplierOrderManagerRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * This function is used to save the fact that a supplier order has been sent.
     * Firstly, we check that a supplierOrder corresponds to the given id.
     * Secondly, we check that the supplierOrder has at least one order line.
     * Thirdly, we can change the "orderIsSent" value as true.
     * @param orderId Corresponds to the id that we want to save as sent.
     * @return The saved supplier order.
     * @throws UnknownSupplierOrderException If any order has been found with the given id.
     * @throws SupplierOrderStateException If the supplier order does not have any order line.
     */
    @Override
    @Transactional(rollbackOn = Exception.class)
    public SupplierOrder sendOrderById(Long orderId) throws UnknownSupplierOrderException, SupplierOrderStateException {
        log.debug("Sending the supplier order with id: " + orderId);
        SupplierOrder supplierOrder = em.find(SupplierOrder.class, orderId);
        if (supplierOrder == null) {
            log.debug("Supplier order with id: {} was not found.", orderId);
            throw new UnknownSupplierOrderException("The supplier order with id: " + orderId + " was not found.");
        }
        if (supplierOrder.getSupplierOrderLines().size() < 1) {
            log.debug("Supplier order with id: {} has no order lines", orderId);
            throw new SupplierOrderStateException("The supplier order with id: " + orderId + " has no order lines.");
        }
        supplierOrder.setOrderIsSent(true);
        em.persist(supplierOrder);
        return supplierOrder;
    }

    /**
     * This function is used to save the reception of a customer order.
     * Firstly, we check that the order exists and that the order has not already been received.
     * Secondly, we also check that the order has been sent to the supplier and that the order contains at least one line.
     * Thirdly, we can change the "received" state as true and increase the stock of each product.
     * Finally, we save the modification in the database, and we return the saved supplierOrder.
     * @param orderId Corresponds to the id that we want to save as received.
     * @return The saved supplier order.
     * @throws UnknownSupplierOrderException If any order has been found with the given id.
     * @throws ProductStockChangeException If the order is not sent, already received.
     * @throws SupplierOrderStateException If the order has no lines, or an error occurs when we save the product.
     */
    @Override
    @Transactional(rollbackOn = Exception.class)
    public SupplierOrder receiveOrderById(Long orderId)
            throws UnknownSupplierOrderException, ProductStockChangeException, SupplierOrderStateException {
        log.debug("Receiving the supplier order with id: " + orderId);
        SupplierOrder supplierOrder = em.find(SupplierOrder.class, orderId);
        if (supplierOrder == null) {
            log.debug("Supplier order with id: {} was not found.", orderId);
            throw new UnknownSupplierOrderException("The supplier order with id: " + orderId + " was not found.");
        }
        if (supplierOrder.getReceived()) {
            log.debug("The supplier order: {}, has already been received.", orderId);
            throw new SupplierOrderStateException("The supplier order: " + orderId + " is already received.");
        }
        if (!supplierOrder.getOrderIsSent()) {
            log.debug("The supplier order: {}, has not been sent.", orderId);
            throw new SupplierOrderStateException("The supplier order: " + orderId + " has not been sent.");
        }
        supplierOrder.setReceived(true);
        List<SupplierOrderLine> orderLines = supplierOrder.getSupplierOrderLines();
        if (orderLines.size() < 1) {
            throw new ProductStockChangeException("The supplier order " + orderId + ", must have at least one order line.");
        }
        try {
            for (SupplierOrderLine line : orderLines) {
                Product product = line.getProduct();
                Integer stockIncrement = line.getQuantity() * product.getBatchSize();
                product.setStock(stockIncrement + product.getStock());
                em.persist(product);
            }
        } catch (Exception e) {
            throw new ProductStockChangeException("An error has occurred while receiving the supplier order: " + orderId);
        }
        em.persist(supplierOrder);
        return supplierOrder;
    }
}
