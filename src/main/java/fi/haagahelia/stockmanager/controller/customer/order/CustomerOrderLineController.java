package fi.haagahelia.stockmanager.controller.customer.order;


import fi.haagahelia.stockmanager.controller.product.ProductController;
import fi.haagahelia.stockmanager.dto.customer.order.CustomerOrderLineCuDTO;
import fi.haagahelia.stockmanager.dto.customer.order.CustomerOrderLineDTO;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrderLine;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.customer.order.CustomerOrderLineRepository;
import fi.haagahelia.stockmanager.repository.customer.order.CustomerOrderRepository;
import fi.haagahelia.stockmanager.repository.product.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Slf4j
@RestController
@RequestMapping("/api/customers/orders/{orderId}")
public class CustomerOrderLineController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final CustomerOrderRepository coRepository;
    private final CustomerOrderLineRepository lineRepository;
    private final ProductRepository pRepository;

    @Autowired
    public CustomerOrderLineController(CustomerOrderRepository coRepository, CustomerOrderLineRepository lineRepository,
                                       ProductRepository pRepository) {
        this.coRepository = coRepository;
        this.lineRepository = lineRepository;
        this.pRepository = pRepository;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    /**
     * This method is used to create and add the HATEOAS links to a DTO model.
     * @param lineDTO The dto model to which we will add the HATEOAS links.
     * @return The dto model with the HATEOAS links.
     */
    private CustomerOrderLineDTO createHATEOAS(CustomerOrderLineDTO lineDTO) {
        Long orderId = lineDTO.getCustomerOrderDTO().getId();
        Long productId = lineDTO.getProductSimpleDTO().getId();

        Link selfRel = linkTo(CustomerOrderLineController.class)
                .slash(orderId).slash("details").slash(productId).withSelfRel();
        lineDTO.add(selfRel);

        Link orderDetailsLink = linkTo(CustomerOrderLineController.class)
                .slash(orderId).slash("details").withRel("order-details");
        lineDTO.add(orderDetailsLink);

        Link orderLink = linkTo(CustomerOrderController.class).slash(orderId).withRel("order");
        lineDTO.add(orderLink);

        Link productLink = linkTo(ProductController.class).slash(productId).withRel("product");
        lineDTO.add(productLink);
        return lineDTO;
    }

    /**
     * This function is used to convert a List of CustomerOrder into a List of CustomerOrderDTO.
     * It also adds the HATEOAS links on each element of the lsit.
     * @param orderLines Corresponds to the list of CustomerOrder.
     * @return Corresponds to the list of CustomerOrderDTO.
     */
    private List<CustomerOrderLineDTO> convertCustomerOrderLines(List<CustomerOrderLine> orderLines) {
        List<CustomerOrderLineDTO> customerOrderLineDTOS = new ArrayList<>();
        for (CustomerOrderLine cusOrderLine : orderLines) {
            CustomerOrderLineDTO cusOrderLineDTO = CustomerOrderLineDTO.convert(cusOrderLine);
            createHATEOAS(cusOrderLineDTO);
            customerOrderLineDTOS.add(cusOrderLineDTO);
        }
        return customerOrderLineDTOS;
    }


    /* ------------------------------------------------- API METHODS ------------------------------------------------ */

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get all the customer order's line that correspond to an order.
     * Firstly, we verify that an order corresponds to the order id given by the user.
     *      If the customer order does not exist, we return an HttpStatus.NO_CONTENT to the user.
     * Secondly, we find all the lines that correspond to the order.
     * Thirdly, we check that the list returned by the previous step is not empty.
     *      If the list is empty, we return an HttpStatus.NO_CONTENT to the user.
     * Fourthly, we convert all the CustomerOrderLine as CustomerOrderLineDTO, we add the HATEOAS links.
     * Then we can add the new object to the final list.
     * Finally, we return the data to the user with an HttpStatus.Ok.
     * @param orderId Corresponds to the id of the customer order.
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @GetMapping(value = "/details",produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<List<CustomerOrderLineDTO>> getOrderLines(@PathVariable(value = "orderId") Long orderId,
                                                                                  @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting the customer order lines of the order: {}.", user.getUsername(), orderId);
        if (!coRepository.existsById(orderId)) {
            log.info("User {} requested the customer order's lines of the order: {}. ORDER NOT FOUND.",
                    user.getUsername(), orderId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<CustomerOrderLine> customerOrderLines = lineRepository.findByCustomerOrderId(orderId);
        if (customerOrderLines.size() < 1) {
            log.info("User {} requested the customer order's lines of the order: {}. NO LINES FOUND.",
                    user.getUsername(), orderId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<CustomerOrderLineDTO> cusOrderLinesDTOS = new ArrayList<>();

        log.info("User {} requested the customer order's lines of the order: {}. RETURNING DATA.",
                user.getUsername(), orderId);
        return new ResponseEntity<>(cusOrderLinesDTOS, HttpStatus.OK);
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get a specific order line, using the orderId and the productId.
     * Firstly, we check that the order exists by using the id given by the user. We do the same for the product.
     * Secondly, we find the specific CustomerOrderLine by using the both previous id.
     *      We check that the optional returned by the find is empty.
     *      If it is the case, we return an HttpStatu.NO_CONTENT.
     * Finally, we convert the object as a CustomerOrderLineDTO, and we return the DTO object
     * with the HATEOAS links to the user with an HttpStatus.Ok.
     * @param orderId Corresponds to the id of the customer order.
     * @param productId Corresponds to the id of a product.
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @GetMapping(value = "/details/product={productId}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<CustomerOrderLineDTO> getOrderLine(@PathVariable(value = "orderId") Long orderId,
                                                                           @PathVariable(value = "productId") Long productId,
                                                                           @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting the customer order line: orderId: {} ; productId: {}.",
                user.getUsername(), orderId, productId);
        if (!coRepository.existsById(orderId)) {
            log.info("User {} requested the customer order line: orderId: {} ; productId: {}. ORDER NOT FOUND.",
                    user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        if (!pRepository.existsById(productId)) {
            log.info("User {} requested the customer order line: orderId: {} ; productId: {}. PRODUCT NOT FOUND.",
                    user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Optional<CustomerOrderLine> orderLine = lineRepository.findByCustomerOrderIdAndProductId(orderId, productId);
        if (orderLine.isEmpty()) {
            log.info("User {} requested the customer order line: orderId: {} ; productId: {}. NO ORDER LINE FOUND.",
                    user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        CustomerOrderLineDTO cusOrderLineDTO = CustomerOrderLineDTO.convert(orderLine.get());
        log.info("User {} requested the customer order line: orderId: {} ; productId: {}. RETURNING DATA.",
                user.getUsername(), orderId, productId);
        return new ResponseEntity<>(createHATEOAS(cusOrderLineDTO), HttpStatus.OK);
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to create and save a new customer order line.
     * Firstly, we check different constraints about the order: order exists,
     * and order is not sent or the delivery date is not passed.
     * Secondly, we check different constraints about the product: product exists, and the product has enough stock.
     * Thirdly, we check other general constraints: order line does not exist, the quantity set by the user is valid,
     * and the price is bigger than 0 (If not we use the sell price in the product table).
     * Fourthly, we can create the CustomerOrderLine object and set all the values.
     * Fifthly, we can save the object in the database.
     * Finally, we convert the CustomerOrderLine object as a CustomerOrderLineDTO, we add the HATEOAS links, and we can
     * return the data to the user with an HttpStatus.CREATED.
     * @param orderId Corresponds to the order's id to which we will add the order line.
     * @param productId Corresponds to the product that the customer wants to buy.
     * @param cusOrderLineDTO Corresponds to other data about the customer order line.
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @PostMapping(value = "/details/{productId}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<CustomerOrderLineDTO> createCusOrderLine(@PathVariable(value = "orderId") Long orderId,
                                                                                 @PathVariable(value = "productId") Long productId,
                                                                                 @RequestBody CustomerOrderLineCuDTO cusOrderLineDTO,
                                                                                 @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to create a new customer order line: orderId: {} ; productId: {}.",
                user.getUsername(), orderId, productId);
        // --------------- Order verification ---------------
        Optional<CustomerOrder> orderOptional = coRepository.findById(orderId);
        if (orderOptional.isEmpty()) {
            log.info("User {} requested to create a new customer order line: orderId: {} ; productId: {}." +
                            "ORDER NOT FOUND.", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        CustomerOrder customerOrder = orderOptional.get();
        if (customerOrder.getSent() || customerOrder.getDeliveryDate().isAfter(LocalDate.now())) {
            log.info("User {} requested to create a new customer order line: orderId: {} ; productId: {}. " +
                            "ORDER ALREADY SENT OR DELIVERY DATE IS PASSED.", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        // --------------- Product verification ---------------
        Optional<Product> productOptional = pRepository.findById(productId);
        if (productOptional.isEmpty()) {
            log.info("User {} requested to create a new customer order line: orderId: {} ; productId: {}." +
                            "PRODUCT NOT FOUND.", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Product product = productOptional.get();
        if (product.getStock() - cusOrderLineDTO.getQuantity() < 0) {
            log.info("User {} requested to create a new customer order line: orderId: {} ; productId: {}." +
                    "NOT ENOUGH STOCK", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        // --------------- Others verification ---------------
        if (lineRepository.existsByCustomerOrderIdAndProductId(customerOrder.getId(), product.getId())) {
            log.info("User {} requested to create a new customer order line: orderId: {} ; productId: {}." +
                            "ALREADY EXISTS.", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        if (cusOrderLineDTO.getQuantity() < 1) {
            log.info("User {} requested to create a new customer order line: orderId: {} ; productId: {}." +
                    "INVALID QUANTITY", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (cusOrderLineDTO.getSellPrice() <= 0) {
            cusOrderLineDTO.setSellPrice(product.getSalePrice());
        }
        // --------------- CREATING ORDER LINE OBJECT ---------------
        CustomerOrderLine customerOrderLine = new CustomerOrderLine();
        customerOrderLine.setCustomerOrder(customerOrder);
        customerOrderLine.setProduct(product);
        customerOrderLine.setSellPrice(cusOrderLineDTO.getSellPrice());
        customerOrderLine.setQuantity(cusOrderLineDTO.getQuantity());
        // --------------- SAVING DATA ---------------
        log.debug("User {} requested to create a new customer order line: orderId: {} ; productId: {}." +
                        "SAVING CUSTOMER ORDER LINE.", user.getUsername(), orderId, productId);
        CustomerOrderLine savedLine = lineRepository.save(customerOrderLine);
        // --------------- RETURNING DATA ---------------
        CustomerOrderLineDTO savedLineDTO = CustomerOrderLineDTO.convert(savedLine);
        createHATEOAS(savedLineDTO);
        log.info("User {} requested to create a new customer order line: orderId: {} ; productId: {}." +
                        "CUSTOMER ORDER LINE SAVED.", user.getUsername(), orderId, productId);
        return new ResponseEntity<>(savedLineDTO, HttpStatus.CREATED);
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to delete a customer order line.
     * Firstly, we check that the customer order line exists.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Secondly, we check that the customer order is not already sent.
     *      If the order is already sent, we return an HttpStatus.NOT_ACCEPTABLE to the user.
     * Finally, we can delete the customer order line and return an HttpStatu.Accepted to the user.
     * @param orderId Corresponds to the id of the order that we want to delete the order line.
     * @param productId Corresponds to the id of the product that we want to delete the order line.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @DeleteMapping(value = "/details/product={productId}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<CustomerOrderLine> deleteOrderLine(@PathVariable(value = "orderId") Long orderId,
                                                                           @PathVariable(value = "productId") Long productId,
                                                                           @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to delete the customer order line: orderId: {} ; productId: {}.",
                user.getUsername(), orderId, productId);
        if (!lineRepository.existsByCustomerOrderIdAndProductId(orderId, productId)) {
            log.info("User {} requested to delete the customer order line: orderId: {} ; productId: {}. LINE NOT FOUND",
                    user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        if (coRepository.getCustomerOrderSentByCustomerId(orderId)) {
            log.info("User {} requested to delete the customer order line: orderId: {} ; productId: {}. ALREADY SENT",
                    user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        log.debug("User {} requested to delete the customer order line: orderId: {} ; productId: {}. DELETING DATA.",
                user.getUsername(), orderId, productId);
        lineRepository.deleteByCustomerOrderIdAndProductId(orderId, productId);
        log.info("User {} requested to delete the customer order line: orderId: {} ; productId: {}. ORDER LINE DELETED.",
                user.getUsername(), orderId, productId);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
