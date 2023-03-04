package fi.haagahelia.stockmanager.controller.customer.order;


import fi.haagahelia.stockmanager.controller.product.ProductController;
import fi.haagahelia.stockmanager.dto.common.ErrorResponse;
import fi.haagahelia.stockmanager.dto.customer.order.CustomerOrderLineCuDTO;
import fi.haagahelia.stockmanager.dto.customer.order.CustomerOrderLineDTO;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrderLine;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.customer.order.CustomerOrderLineRepository;
import fi.haagahelia.stockmanager.repository.customer.order.CustomerOrderRepository;
import fi.haagahelia.stockmanager.repository.product.ProductRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
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

@Log4j2
@RestController
@RequestMapping("/api/customers/orders/{orderId}")
public class CustomerOrderLineController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final CustomerOrderRepository coRepository;
    private final CustomerOrderLineRepository lineRepository;
    private final ProductRepository pRepository;

    @Autowired
    public CustomerOrderLineController(CustomerOrderRepository coRepository, CustomerOrderLineRepository lineRepository, ProductRepository pRepository) {
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

        Link selfRel = linkTo(CustomerOrderLineController.class).slash(orderId).slash("details").slash(productId).withSelfRel();
        lineDTO.add(selfRel);

        Link orderDetailsLink = linkTo(CustomerOrderLineController.class).slash(orderId).slash("details").withRel("order-details");
        lineDTO.add(orderDetailsLink);

        Link orderLink = linkTo(CustomerOrderController.class).slash(orderId).withRel("order");
        lineDTO.add(orderLink);

        Link productLink = linkTo(ProductController.class).slash(productId).withRel("product");
        lineDTO.add(productLink);
        return lineDTO;
    }

    /**
     * This function is used to convert a List of CustomerOrder into a List of CustomerOrderDTO.
     * It also adds the HATEOAS links on each element of the list.
     * @param orderLines Corresponds to the list of CustomerOrder.
     * @return Corresponds to the list of CustomerOrderDTO.
     */
    private PagedModel<CustomerOrderLineDTO> convertCustomerOrderLines(Page<CustomerOrderLine> orderLines) {
        List<CustomerOrderLineDTO> customerOrderLineDTOS = new ArrayList<>();
        for (CustomerOrderLine cusOrderLine : orderLines) {
            CustomerOrderLineDTO cusOrderLineDTO = CustomerOrderLineDTO.convert(cusOrderLine);
            createHATEOAS(cusOrderLineDTO);
            customerOrderLineDTOS.add(cusOrderLineDTO);
        }
        PagedModel.PageMetadata pmd = new PagedModel.PageMetadata(orderLines.getSize(), orderLines.getNumber(), orderLines.getTotalElements());
        return PagedModel.of(customerOrderLineDTOS, pmd);
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
     *
     * @param orderId Corresponds to the id of the customer order.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing a page model of CustomerOrderLineDTO objects or a Error Message.
     *      --> HttpStatus.OK if at least one customer order line has been found. (Page of CustomerOrderLineDTO)
     *      --> HttpStatus.BAD_REQUEST if the customer order does not exits. (ErrorMessage)
     *      --> HttpStatus.NO_CONTENT if no customer order line exists. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/details",produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<?> getOrderLines(@PathVariable(value = "orderId") Long orderId, @AuthenticationPrincipal Employee user,
                                                         @PageableDefault(size = 10) Pageable pageable,
                                                         @SortDefault.SortDefaults({
                                                                 @SortDefault(sort = "quantity", direction = Sort.Direction.ASC)}) Sort sort) {
        try {
            log.info("User {} is requesting the customer order lines of the order: {}.", user.getUsername(), orderId);
            if (!coRepository.existsById(orderId)) {
                log.info("User {} requested the customer order lines of the order: {}. ORDER NOT FOUND.", user.getUsername(), orderId);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_CUSTOMER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            Page<CustomerOrderLine> customerOrderLines = lineRepository.findByCustomerOrderId(orderId, pageable);
            if (customerOrderLines.getTotalElements() < 1) {
                log.info("User {} requested the customer order lines of the order: '{}'. NO LINES FOUND.", user.getUsername(), orderId);
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_CUSTOMER_ORDER_LINES_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            PagedModel<CustomerOrderLineDTO> cusOrderLinesDTOS = convertCustomerOrderLines(customerOrderLines);
            cusOrderLinesDTOS.add(linkTo(CustomerOrderLineRepository.class).slash(orderId).slash("details").withSelfRel());
            log.info("User {} requested the customer order lines of the order: '{}'. RETURNING DATA.", user.getUsername(), orderId);
            return new ResponseEntity<>(cusOrderLinesDTOS, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested the customer order lines of the order: '{}'. UNEXPECTED ERROR!", user.getUsername(), orderId);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
     *
     * @param orderId Corresponds to the id of the customer order.
     * @param productId Corresponds to the id of a product.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing a CustomerOrderLineDTO objects or a Error Message.
     *      --> HttpStatus.OK if the customer order line exists. (CustomerOrderLineDTO)
     *      --> HttpStatus.BAD_REQUEST if the customer order or the product does not exit. (ErrorMessage)
     *      --> HttpStatus.NO_CONTENT if no customer order line corresponds to the id. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/details/product={productId}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<?> getCusOrderLine(@PathVariable(value = "orderId") Long orderId,
                                                           @PathVariable(value = "productId") Long productId,
                                                           @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting the customer order line: orderId: '{}' ; productId: '{}'.", user.getUsername(), orderId, productId);
            if (!coRepository.existsById(orderId)) {
                log.info("User {} requested the customer order line: orderId: '{}' ; productId: '{}'. ORDER NOT FOUND.", user.getUsername(), orderId, productId);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_CUSTOMER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            if (!pRepository.existsById(productId)) {
                log.info("User {} requested the customer order line: orderId: '{}' ; productId: '{}'. PRODUCT NOT FOUND.", user.getUsername(), orderId, productId);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_PRODUCT_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            Optional<CustomerOrderLine> orderLine = lineRepository.findByCustomerOrderIdAndProductId(orderId, productId);
            if (!orderLine.isPresent()) {
                log.info("User {} requested the customer order line: orderId: '{}' ; productId: '{}'. NO ORDER LINE FOUND.", user.getUsername(), orderId, productId);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_CUSTOMER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            CustomerOrderLineDTO cusOrderLineDTO = CustomerOrderLineDTO.convert(orderLine.get());
            log.info("User {} requested the customer order line: orderId: '{}' ; productId: '{}'. RETURNING DATA.", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(createHATEOAS(cusOrderLineDTO), HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested the customer order line: orderId: '{}'; productId: '{}'. UNEXPECTED ERROR!", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
     *
     * @param orderId Corresponds to the order's id to which we will add the order line.
     * @param productId Corresponds to the product that the customer wants to buy.
     * @param cusOrderLineDTO Corresponds to other data about the customer order line.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing a CustomerOrderLineDTO objects or a Error Message.
     *      --> HttpStatus.CREATED if the customer order line has been created. (CustomerOrderLineDTO)
     *      --> HttpStatus.XX if a criteria has not been validated. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PostMapping(value = "/details/{productId}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<?> createCusOrderLine(@PathVariable(value = "orderId") Long orderId, @PathVariable(value = "productId") Long productId,
                                                              @RequestBody CustomerOrderLineCuDTO cusOrderLineDTO, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to create a new customer order line: orderId: '{}' ; productId: '{}'.",
                    user.getUsername(), orderId, productId);
            // --------------- Order verification ---------------
            Optional<CustomerOrder> orderOptional = coRepository.findById(orderId);
            if (!orderOptional.isPresent()) {
                log.info("User {} requested to create a new customer order line: orderId: '{}' ; productId: '{}'. ORDER NOT FOUND.", user.getUsername(), orderId, productId);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_CUSTOMER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            CustomerOrder customerOrder = orderOptional.get();
            if (customerOrder.getSent() || customerOrder.getDeliveryDate().isAfter(LocalDate.now())) {
                log.info("User {} requested to create a new customer order line: orderId: '{}' ; productId: '{}'. ORDER ALREADY SENT OR DELIVERY DATE IS PASSED.", user.getUsername(), orderId, productId);
                ErrorResponse bm = new ErrorResponse(HttpStatus.PRECONDITION_FAILED.getReasonPhrase(), "CUSTOMER_ORDER_ALREADY_SENT_OR_DELIVERY_DATE_PASSED");
                return new ResponseEntity<>(bm, HttpStatus.PRECONDITION_FAILED);
            }
            // --------------- Product verification ---------------
            Optional<Product> productOptional = pRepository.findById(productId);
            if (!productOptional.isPresent()) {
                log.info("User {} requested to create a new customer order line: orderId: '{}' ; productId: '{}'. PRODUCT NOT FOUND.", user.getUsername(), orderId, productId);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_PRODUCT_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            Product product = productOptional.get();
            if (product.getStock() - cusOrderLineDTO.getQuantity() < 0) {
                log.info("User {} requested to create a new customer order line: orderId: '{}' ; productId: '{}'. NOT ENOUGH STOCK", user.getUsername(), orderId, productId);
                ErrorResponse bm = new ErrorResponse(HttpStatus.PRECONDITION_FAILED.getReasonPhrase(), "PRODUCT_STOCK_TOO_LOW");
                return new ResponseEntity<>(bm, HttpStatus.PRECONDITION_FAILED);
            }
            // --------------- Others verification ---------------
            if (lineRepository.existsByCustomerOrderIdAndProductId(customerOrder.getId(), product.getId())) {
                log.info("User {} requested to create a new customer order line: orderId: '{}' ; productId: '{}'. ALREADY EXISTS.", user.getUsername(), orderId, productId);
                ErrorResponse bm = new ErrorResponse(HttpStatus.CONFLICT.getReasonPhrase(), "CUSTOMER_ORDER_LINE_ALREADY_EXIST");
                return new ResponseEntity<>(bm, HttpStatus.CONFLICT);
            }
            if (cusOrderLineDTO.getQuantity() == null || cusOrderLineDTO.getQuantity() < 1) {
                log.info("User {} requested to create a new customer order line: orderId: '{}' ; productId: '{}'. INVALID QUANTITY", user.getUsername(), orderId, productId);
                ErrorResponse bm = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(), "CUSTOMER_ORDER_LINE_INVALID_QUANTITY");
                return new ResponseEntity<>(bm, HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (cusOrderLineDTO.getSellPrice() == null || cusOrderLineDTO.getSellPrice() < 1) {
                cusOrderLineDTO.setSellPrice(product.getSalePrice());
            }
            // --------------- CREATING ORDER LINE OBJECT ---------------
            CustomerOrderLine customerOrderLine = new CustomerOrderLine();
            customerOrderLine.setCustomerOrder(customerOrder);
            customerOrderLine.setProduct(product);
            customerOrderLine.setSellPrice(cusOrderLineDTO.getSellPrice());
            customerOrderLine.setQuantity(cusOrderLineDTO.getQuantity());
            // --------------- SAVING DATA ---------------
            log.debug("User {} requested to create a new customer order line: orderId: '{}' ; productId: '{}'. SAVING CUSTOMER ORDER LINE.", user.getUsername(), orderId, productId);
            CustomerOrderLine savedLine = lineRepository.save(customerOrderLine);
            // --------------- RETURNING DATA ---------------
            CustomerOrderLineDTO savedLineDTO = CustomerOrderLineDTO.convert(savedLine);
            createHATEOAS(savedLineDTO);
            log.info("User {} requested to create a new customer order line: orderId: '{}' ; productId: '{}'. CUSTOMER ORDER LINE SAVED.", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(savedLineDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            log.info("User {} requested to create a new customer order line: orderId: '{}'; productId: '{}'. UNEXPECTED ERROR!", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to delete a customer order line.
     * Firstly, we check that the customer order line exists.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Secondly, we check that the customer order is not already sent.
     *      If the order is already sent, we return an HttpStatus.NOT_ACCEPTABLE to the user.
     * Finally, we can delete the customer order line and return an HttpStatu.Accepted to the user.
     *
     * @param orderId Corresponds to the id of the order that we want to delete the order line.
     * @param productId Corresponds to the id of the product that we want to delete the order line.
     * @return a ResponseEntity containing an Error Message.
     *      --> HttpStatus.OK if the customer order line has been deleted.
     *      --> HttpStatus.BAD_REQUEST if no customer order line corresponds to the given id.
     *      --> HttpStatus.PRECONDITION_FAILED if the customer order is already sent.
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs.
     */
    @DeleteMapping(value = "/details/product={productId}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<?> deleteOrderLine(@PathVariable(value = "orderId") Long orderId, @PathVariable(value = "productId") Long productId,
                                                           @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to delete the customer order line: orderId: '{}' ; productId: '{}'.", user.getUsername(), orderId, productId);
            if (!lineRepository.existsByCustomerOrderIdAndProductId(orderId, productId)) {
                log.info("User {} requested to delete the customer order line: orderId: '{}' ; productId: '{}'. LINE NOT FOUND", user.getUsername(), orderId, productId);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "CUSTOMER_ORDER_LINE_NOT_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            if (coRepository.getCustomerOrderSentByCustomerId(orderId)) {
                log.info("User {} requested to delete the customer order line: orderId: '{}' ; productId: '{}'. ALREADY SENT", user.getUsername(), orderId, productId);
                ErrorResponse bm = new ErrorResponse(HttpStatus.PRECONDITION_FAILED.getReasonPhrase(), "CUSTOMER_ORDER_ALREADY_SENT");
                return new ResponseEntity<>(bm, HttpStatus.PRECONDITION_FAILED);
            }
            log.debug("User {} requested to delete the customer order line: orderId: '{}' ; productId: '{}'. DELETING DATA.", user.getUsername(), orderId, productId);
            lineRepository.deleteByCustomerOrderIdAndProductId(orderId, productId);
            log.info("User {} requested to delete the customer order line: orderId: '{}' ; productId: '{}'. ORDER LINE DELETED.", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to delete the customer order line: orderId: '{}'; productId: '{}'. UNEXPECTED ERROR!", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
