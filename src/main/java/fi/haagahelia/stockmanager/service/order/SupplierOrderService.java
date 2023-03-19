package fi.haagahelia.stockmanager.service.order;

import fi.haagahelia.stockmanager.exception.OrderStateException;
import fi.haagahelia.stockmanager.exception.ProductStockException;
import fi.haagahelia.stockmanager.exception.UnknownOrderException;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrderLine;
import fi.haagahelia.stockmanager.repository.supplier.order.SupplierOrderManagerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;


@Log4j2
@Service
public class SupplierOrderService implements SupplierOrderManagerRepository {


    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager em;

    public void setEm(EntityManager em) {
        this.em = em;
    }

    /**
     * This function is used to save the fact that a supplier order has been sent.
     * Firstly, we check that a supplierOrder corresponds to the given id.
     * Secondly, we check that the supplierOrder has at least one order line.
     * Thirdly, we can change the "orderIsSent" value as true.
     * @param orderId Corresponds to the id that we want to save as sent.
     * @return The saved supplier order.
     * @throws UnknownOrderException If any order has been found with the given id.
     * @throws OrderStateException If the supplier order does not have any order line.
     */
    @Override
    @Transactional(rollbackOn = Exception.class)
    public SupplierOrder sendOrderById(Long orderId) throws UnknownOrderException, OrderStateException {
        log.debug("Sending the supplier order with id: " + orderId);
        SupplierOrder supplierOrder = em.find(SupplierOrder.class, orderId);
        if (supplierOrder == null) {
            log.debug("Supplier order with id: {} was not found.", orderId);
            throw new UnknownOrderException("The supplier order with id: " + orderId + ", was not found.");
        }
        if (supplierOrder.getOrderIsSent()) {
            log.debug("Supplier order with id: {} , is already sent", orderId);
            throw new OrderStateException("The supplier order with id: " + orderId + ", is already sent.");
        }
        Query query = em.createQuery("SELECT line FROM SupplierOrderLine line where line.supplierOrder.id = ?1").setParameter(1, orderId);
        if (query.getResultList().size() < 1) {
            log.debug("Supplier order with id: {} has no order lines", orderId);
            throw new OrderStateException("The supplier order with id: " + orderId + ", has no order lines.");
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
     * @throws UnknownOrderException If any order has been found with the given id.
     * @throws ProductStockException If the order is not sent, already received.
     * @throws OrderStateException If the order has no lines, or an error occurs when we save the product.
     */
    @Override
    @Transactional(rollbackOn = Exception.class)
    public SupplierOrder receiveOrderById(Long orderId)
            throws UnknownOrderException, ProductStockException, OrderStateException {
        log.debug("Receiving the supplier order with id: " + orderId);
        SupplierOrder supplierOrder = em.find(SupplierOrder.class, orderId);
        if (supplierOrder == null) {
            log.debug("Supplier order with id: {} was not found.", orderId);
            throw new UnknownOrderException("The supplier order with id: " + orderId + " was not found.");
        }
        if (supplierOrder.getReceived()) {
            log.debug("The supplier order: {}, has already been received.", orderId);
            throw new OrderStateException("The supplier order: " + orderId + " is already received.");
        }
        if (!supplierOrder.getOrderIsSent()) {
            log.debug("The supplier order: {}, has not been sent.", orderId);
            throw new OrderStateException("The supplier order: " + orderId + " has not been sent.");
        }
        supplierOrder.setReceived(true);
        Query query = em.createQuery("SELECT line FROM SupplierOrderLine line where line.supplierOrder.id = ?1").setParameter(1, orderId);
        List<SupplierOrderLine> orderLines = query.getResultList();
        if (orderLines.size() < 1) {
            throw new ProductStockException("The supplier order " + orderId + ", must have at least one order line.");
        }
        try {
            for (SupplierOrderLine line : orderLines) {
                Product product = line.getProduct();
                Integer stockIncrement = line.getQuantity() * product.getBatchSize();
                product.setStock(stockIncrement + product.getStock());
                em.persist(product);
            }
        } catch (Exception e) {
            log.info("An error has occurred while receiving the supplier order: " + orderId);
            throw new ProductStockException("An error has occurred while receiving the supplier order: " + orderId);
        }
        em.persist(supplierOrder);
        return em.find(SupplierOrder.class, supplierOrder.getId());
    }

    /**
     * This function is used to cancel the reception of a supplier order.
     * Firstly, we check that the order exists and that the order is considerate as received.
     * Secondly, we decrease the stock of each product that are concerned by the order.
     * @param orderId Corresponds to the id that we want to save as received.
     * @return The saved supplier order.
     * @throws UnknownOrderException If the order does not exist.
     * @throws ProductStockException If the order is already not considerate as received.
     * @throws OrderStateException If a problem is related to a product. (Remaining stock is too low).
     */
    @Override
    @Transactional(rollbackOn = Exception.class)
    public SupplierOrder cancelReceiveOrder(Long orderId) throws UnknownOrderException, ProductStockException, OrderStateException {
        log.debug("Cancelling of the reception of the supplier order with id: " + orderId);
        SupplierOrder supplierOrder = em.find(SupplierOrder.class, orderId);
        if (supplierOrder == null) {
            log.debug("Cancelling of the reception of supplier order with id: {} was not found.", orderId);
            throw new UnknownOrderException("The supplier order with id: " + orderId + " was not found.");
        }
        if (!supplierOrder.getReceived()) {
            log.debug("The supplier order: {}, has not been received.", orderId);
            throw new OrderStateException("The supplier order: " + orderId + " is not received.");
        }
        supplierOrder.setReceived(false);
        Query query = em.createQuery("SELECT line FROM SupplierOrderLine line where line.supplierOrder.id = ?1").setParameter(1, orderId);
        List<SupplierOrderLine> orderLines = query.getResultList();
        try {
            for (SupplierOrderLine line : orderLines) {
                Product product = line.getProduct();
                System.out.println("Product:" + product);
                Integer stockDecrement = line.getQuantity() * product.getBatchSize();
                System.out.println("Stock decrement: " + stockDecrement);
                System.out.println("Remaining: " + (product.getStock() - stockDecrement));
                if (product.getStock() - stockDecrement < 0) {
                    throw new ProductStockException("Product " + product.getId() + " cannot have a negative stock.");
                }
                product.setStock(product.getStock() - stockDecrement);
                em.persist(product);
            }
        } catch (Exception e) {
            throw new ProductStockException("An error has occurred while cancelling the reception the supplier order: " + orderId);
        }
        em.persist(supplierOrder);
        return em.find(SupplierOrder.class, supplierOrder.getId());
    }
}
