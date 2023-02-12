package fi.haagahelia.stockmanager.service.order;

import fi.haagahelia.stockmanager.exception.ProductStockException;
import fi.haagahelia.stockmanager.exception.OrderStateException;
import fi.haagahelia.stockmanager.exception.UnknownOrderException;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrderLine;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.repository.customer.order.CustomerOrderManagerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;


@Slf4j
@Repository
public class CustomerOrderManagerImpl implements CustomerOrderManagerRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * This function is used to considerate an order as shipped.
     * Firstly, we check that an order corresponds to the id received in the parameter.
     * Secondly, we check that the order si not already shipped to the customer.
     * Thirdly, we check that the customer order has at least one order line.
     * Fourthly, we change the stock to each product.
     * Finally, we change the status of the order as shipped, and we save the modification.
     * @param orderId Corresponds to the id that we want to ship to the customer
     * @return The saved customer order.
     * @throws UnknownOrderException If the order does not exist.
     * @throws ProductStockException If a problem is related to a product. (Remaining stock is too low).
     * @throws OrderStateException  If the order is already considerate as shipped.
     */
    @Override
    @Transactional(rollbackOn = Exception.class)
    public CustomerOrder customerOrderShipment(Long orderId)
            throws UnknownOrderException, ProductStockException, OrderStateException {
        log.debug("Shipment of the customer order with id: " + orderId);
        CustomerOrder customerOrder = em.find(CustomerOrder.class, orderId);
        if (customerOrder == null) {
            log.debug("Customer order with id: {} was not found.", orderId);
            throw new UnknownOrderException("The customer order with id: " + orderId + " was not found.");
        }
        if (customerOrder.getSent()) {
            log.debug("The customer order: {}, has already been sent.", orderId);
            throw new OrderStateException("The customer order: " + orderId + " is already sent.");
        }
        List<CustomerOrderLine> customerOrderLines = customerOrder.getCustomerOrderLines();
        if (customerOrderLines.size() < 1) {
            log.debug("The customer order: {}, must have at least one order line.", orderId);
            throw new ProductStockException("The customer order " + orderId + ", must have at least one order line.");
        }
        try {
            for (CustomerOrderLine orderLine : customerOrderLines) {
                Product product = orderLine.getProduct();
                if (product.getStock() - orderLine.getQuantity() < 0) {
                    log.debug("Product: " + product.getId() + " does not have enough stock to handle this order.");
                    throw new ProductStockException("Product: " + product.getId() + " cannot have a negative stock.");
                }
                product.setStock(product.getStock() - orderLine.getQuantity());
                em.persist(product);
            }
        } catch (Exception e) {
            log.info("An error has occurred while sending the customer order: " + orderId);
            throw new ProductStockException("An error has occurred while sending the customer order: " + orderId);
        }
        customerOrder.setSent(true);
        em.persist(customerOrder);
        return em.find(CustomerOrder.class, customerOrder.getId());
    }

    /**
     * This function is used to cancel the shipment of a customer order.
     * Firstly, we check that a customer order exists by the given id.
     * Secondly, we check that the founded customer order is already shipped.
     * Thirdly, we can increase the stock of each product.
     * Finally, we can save the modification in the database.
     * @param orderId Corresponds to the id that we want to ship to the customer
     * @return The saved customer order.
     * @throws UnknownOrderException If the order does not exist.
     * @throws OrderStateException If the order is not considerate as shipped.
     * @throws ProductStockException If a problem is related to a product.
     */
    @Override
    @Transactional(rollbackOn = Exception.class)
    public CustomerOrder customerOrderShipmentCancel(Long orderId)
            throws UnknownOrderException, OrderStateException, ProductStockException {
        log.debug("Cancellation of the shipment  the customer order with id: " + orderId);
        CustomerOrder customerOrder = em.find(CustomerOrder.class, orderId);
        if (customerOrder == null) {
            log.debug("Customer order with id: {} was not found.", orderId);
            throw new UnknownOrderException("The customer order with id: " + orderId + " was not found.");
        }
        if (!customerOrder.getSent()) {
            log.debug("The customer order: {}, has not been sent.", orderId);
            throw new OrderStateException("The customer order: " + orderId + " has not been sent.");
        }
        List<CustomerOrderLine> customerOrderLines = customerOrder.getCustomerOrderLines();
        try {
            for (CustomerOrderLine orderLine : customerOrderLines) {
                Product product = orderLine.getProduct();
                product.setStock(product.getStock() + orderLine.getQuantity());
                em.persist(product);
            }
        } catch (Exception e) {
            log.info("An error has occurred while cancelling the shipment of the customer order: " + orderId);
            throw new ProductStockException("An error has occurred while cancelling the shipment the customer order: " + orderId);
        }
        customerOrder.setSent(false);
        em.persist(customerOrder);
        return em.find(CustomerOrder.class, customerOrder.getId());
    }
}
