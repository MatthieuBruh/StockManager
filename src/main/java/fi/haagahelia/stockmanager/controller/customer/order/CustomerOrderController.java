package fi.haagahelia.stockmanager.controller.customer.order;


import fi.haagahelia.stockmanager.controller.customer.CustomerController;
import fi.haagahelia.stockmanager.controller.user.EmployeeController;
import fi.haagahelia.stockmanager.dto.common.ErrorResponse;
import fi.haagahelia.stockmanager.dto.customer.order.CustomerOrderCuDTO;
import fi.haagahelia.stockmanager.dto.customer.order.CustomerOrderDTO;
import fi.haagahelia.stockmanager.exception.OrderStateException;
import fi.haagahelia.stockmanager.exception.ProductStockException;
import fi.haagahelia.stockmanager.exception.UnknownOrderException;
import fi.haagahelia.stockmanager.model.customer.Customer;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.customer.CustomerRepository;
import fi.haagahelia.stockmanager.repository.customer.order.CustomerOrderRepository;
import fi.haagahelia.stockmanager.service.order.CustomerOrderService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
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
@RequestMapping("/api/customers")
public class CustomerOrderController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final CustomerRepository cRepository;
    private final CustomerOrderRepository coRepository;
    private final CustomerOrderService orderManager;

    @Autowired
    public CustomerOrderController(CustomerRepository cRepository, CustomerOrderRepository coRepository, CustomerOrderService orderManager) {
        this.cRepository = cRepository;
        this.coRepository = coRepository;
        this.orderManager = orderManager;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    /**
     * This method is used to create and add the HATEOAS links to a DTO models.
     * @param customerOrderDTO The dto model to which we will add the HATEOAS links.
     * @return The dto model with the HATEOAS links.
     */
    private CustomerOrderDTO createHATEOAS(CustomerOrderDTO customerOrderDTO) {
        Link selfLink = linkTo(CustomerOrderController.class).slash("/orders" + customerOrderDTO.getId()).withSelfRel();
        customerOrderDTO.add(selfLink);

        Link all = linkTo(CustomerOrderController.class).slash("/orders").withRel("customer-orders");
        customerOrderDTO.add(all);
        if (customerOrderDTO.getEmployeeDTO() != null) {
            Link vendor = linkTo(EmployeeController.class).slash(customerOrderDTO.getEmployeeDTO().getId()).withRel("employee");
            customerOrderDTO.add(vendor);
        }
        if (customerOrderDTO.getCustomerDTO() != null) {
            Link customer = linkTo(CustomerController.class).slash(customerOrderDTO.getCustomerDTO().getId()).withRel("customer");
            customerOrderDTO.add(customer);
        }
        if (customerOrderDTO.getCustomerDTO() != null) {
            Long customerId = customerOrderDTO.getId();
            Link orderOfACustomer = linkTo(CustomerOrderController.class).slash("/" + customerId + "/orders").withRel("Orders from this customer");
            customerOrderDTO.add(orderOfACustomer);
        }
        return customerOrderDTO;
    }

    /**
     * This function is used to convert a List of CustomerOrder into a List of CustomerOrderDTO.
     * It also adds the HATEOAS links on each element of the list.
     * @param customerOrders Corresponds to the list of CustomerOrder.
     * @return Corresponds to the list of CustomerOrderDTO.
     */
    private PagedModel<CustomerOrderDTO> convertList(Page<CustomerOrder> customerOrders) {
        List<CustomerOrderDTO> customerOrderDTOS = new ArrayList<>();
        for (CustomerOrder customerOrder : customerOrders) {
            CustomerOrderDTO customerOrderDTO = CustomerOrderDTO.convert(customerOrder);
            createHATEOAS(customerOrderDTO);
            customerOrderDTOS.add(customerOrderDTO);
        }
        PagedModel.PageMetadata pmd = new PagedModel.PageMetadata(customerOrders.getSize(), customerOrders.getNumber(), customerOrders.getTotalElements());
        return PagedModel.of(customerOrderDTOS, pmd);
    }

    /**
     * This function is used to validate the creation of an order.
     * It checked that the date and the delivery date are not null.
     * @param orderCuDTO Corresponds to the order to check
     * @return A ResponseEntity with the related code and string that contains the reason of the decision.
     */
    private Pair<HttpStatus, String> orderValidation(CustomerOrderCuDTO orderCuDTO) {
        if (orderCuDTO.getDate() == null) return Pair.of(HttpStatus.BAD_REQUEST, "CUSTOMER_ORDER_DATE_INVALID.");
        if (orderCuDTO.getDeliveryDate() == null) return Pair.of(HttpStatus.BAD_REQUEST, "CUSTOMER_ORDER_DELIVERY_DATE_INVALID.");
        return Pair.of(HttpStatus.ACCEPTED, "");
    }


    /* ------------------------------------------------- API METHODS ------------------------------------------------ */

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get all the customers orders that are stored in the database.
     * Firstly, we find all the customers orders that are in the database by using the CustomerOrderRepository.
     * Secondly, we check that the list returned by the previous step, has at least one order inside.
     *      If the list is empty, we return an HttpStatus.NO_CONTENT to the user.
     * Thirdly, we have to convert each CustomerOrder object as a CustomerOrderDTO object.
     * Finally, we return the data to the user with an HttpStatus.Ok.
     * 
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing a page model of CustomerOrderDTO objects or a Error Message.
     *      --> HttpStatus.OK if at least one customer order has been found. (Page of CustomerOrderDTO)
     *      --> HttpStatus.NO_CONTENT if no customer order exists. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/orders", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<?> getCustomerOrders(@AuthenticationPrincipal Employee user,
                                                             @RequestParam(required = false) String searchQuery,
                                                             @PageableDefault(size = 10) Pageable pageable,
                                                             @SortDefault.SortDefaults({
                                                                     @SortDefault(sort = "id", direction = Sort.Direction.ASC)}) Sort sort) {
        try {
            log.info("User {} is requesting all the customer orders.", user.getUsername());
            Specification<CustomerOrder> spec = null;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                spec = (root, query, cb) -> cb.like(cb.lower(root.get("id")), "%" + searchQuery.toLowerCase() + "%");
            }
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            Page<CustomerOrder> customerOrders = coRepository.findAll(spec, pageable);
            if (customerOrders.getTotalElements() < 1) {
                log.info("User {} requested all the customer orders. NO DATA FOUND.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_CUSTOMER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            PagedModel<CustomerOrderDTO> customerOrderDTOPage = convertList(customerOrders);
            customerOrderDTOPage.add(linkTo(CustomerOrderController.class).slash("orders").withRel("customers-orders"));
            log.info("User {} requested all the customer orders. RETURNING DATA.", user.getUsername());
            return new ResponseEntity<>(customerOrderDTOPage, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested all the customer orders. UNEXPECTED ERROR!", user.getUsername());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get a customer order by its id.
     * Firstly, we find the customer order by id, using the CustomerOrderRepository.
     * Secondly, we check that the returned Optional contains an order.
     *      If not, we return to the customer an HttpStatus.NO_CONTENT.
     * Thirdly, we convert the CustomerOrder as a CustomerOrderDTO.
     * Finally, we add the HATEOAS links to the CustomerOrderDTO and we return all the data to the user.
     * 
     * @param id Correspond to the id of the customer order that the user want to see.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing a CustomerOrderDTO objects or a Error Message.
     *      --> HttpStatus.OK if the customer order exists. (CustomerOrderDTO)
     *      --> HttpStatus.BAD_REQUEST if no customer order corresponds to the id. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/orders/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<?> getCustomerOrder(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting the customer order with id: '{}'.", user.getUsername(), id);
            Optional<CustomerOrder> orderOptional = coRepository.findById(id);
            if (!orderOptional.isPresent()) {
                log.info("User {} requested the customer order with id: '{}'. NO DATA FOUND.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_CUSTOMER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            CustomerOrderDTO customerOrderDTO = CustomerOrderDTO.convert(orderOptional.get());
            log.info("User {} requested the customer order with id: '{}'. RETURNING DATA.", user.getUsername(), id);
            return new ResponseEntity<>(createHATEOAS(customerOrderDTO), HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested the customer order with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get all the orders of a customer (searching by customer id).
     * Firstly, we check that a customer exists with the id given by the user by using the CustomerRepository.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Secondly, we search all the orders that are related to the customer id, by using the CustomerOrderRepository.
     * Thirdly, we check that the list returned by the previous step, is not empty.
     *      If the list is empty, we return an HttpStatus.NO_CONTENT to the user.
     * Fourthly, we convert all the CustomerOrder objects as CustomerOrderDTO. We add HATEOAS links at the same time.
     * Finally, we return the data to the user with an HttpStatus.Ok.
     * 
     * @param id Corresponds to the id of the customer.
     * @param user authenticated Employee object
     * @param searchQuery the search query, which can be null or an empty string
     * @param pageable pagination information (page number, size, and sorting)
     * @param sort sorting information for the query
     * @return a ResponseEntity containing a page model of CustomerOrderDTO objects or a Error Message.
     *      --> HttpStatus.OK if at least one customer order has been found. (Page of CustomerOrderDTO)
     *      --> HttpStatus.BAD_REQUEST if no customer exists with the given id. (ErrorMessage)
     *      --> HttpStatus.NO_CONTENT if no customer order exists. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/{id}/orders", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<?> getCustomerOrders(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user,
                                                             @RequestParam(required = false) String searchQuery,
                                                             @PageableDefault(size = 10) Pageable pageable,
                                                             @SortDefault.SortDefaults({
                                                                     @SortDefault(sort = "id", direction = Sort.Direction.ASC)}) Sort sort) {
        try {
            log.info("User {} is requesting all the orders of the customer: {}.", user.getUsername(), id);
            if (!cRepository.existsById(id)) {
                log.info("User {} requested all the orders of the customer: {}. NO CUSTOMER FOUND.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_CUSTOMER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            Specification<CustomerOrder> spec = null;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                spec = (root, query, cb) -> cb.like(cb.lower(root.get("id")), "%" + searchQuery.toLowerCase() + "%");
            }
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            Page<CustomerOrder> customerOrders = coRepository.findByCustomerId(id, spec, pageable);
            if (customerOrders.getTotalElements() < 1) {
                log.info("User {} requested all the orders of the customer: {}. NO ORDER FOUND.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_CUSTOMER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            PagedModel<CustomerOrderDTO> customerOrderDTOSPage = convertList(customerOrders);
            customerOrderDTOSPage.add(linkTo(CustomerOrderController.class).slash(id).slash("orders").withRel("customers-orders"));
            log.info("User {} requested all the orders of the customer: {}. RETURNING DATA.", user.getUsername(), id);
            return new ResponseEntity<>(customerOrderDTOSPage, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested the customer order with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get all the orders that have a specific delivery date.
     * Firstly, we check that the date given by the user is valid (not null).
     *      If the date is null, we return to the user an HttpStatus.PRECONDITION_FAILED.
     * Secondly, we find all the orders that have the given date in the database.
     *      We also check that the list returned by this step is not empty,
     *      if the list is empty, we return an HttpStatus.NO_CONTENT to the user.
     * Finally, we convert all the CustomerOrder as a CustomerOrderDTO by using the convertList function.
     * When the list has been converted, we can return data to the user with an HttpStatus.OK.
     * 
     * @param date Corresponds to the delivery date the user wants the sales orders.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing a page model of CustomerOrderDTO objects or a Error Message.
     *      --> HttpStatus.OK if at least one customer order has been found. (Page of CustomerOrderDTO)
     *      --> HttpStatus.BAD_REQUEST if no customer exists with the given id. (ErrorMessage)
     *      --> HttpStatus.NO_CONTENT if no customer order exists. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/orders/delivery={date}", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<?> getCustOrdersDate(@PathVariable(value = "date") LocalDate date, @AuthenticationPrincipal Employee user,
                                                             @RequestParam(required = false) String searchQuery,
                                                             @PageableDefault(size = 10) Pageable pageable,
                                                             @SortDefault.SortDefaults({
                                                                     @SortDefault(sort = "deliveryDate", direction = Sort.Direction.ASC)}) Sort sort) {
        try {
            log.info("User {} is requesting the customer orders with a delivery date of: '{}'.", user.getUsername(), date);
            if (date == null) {
                log.info("User {} requested the customer orders with a delivery date that is null.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(), "URL_DELIVERY_DATE_NULL");
                return new ResponseEntity<>(bm, HttpStatus.UNPROCESSABLE_ENTITY);
            }
            Specification<CustomerOrder> spec = null;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                spec = (root, query, cb) -> cb.like(cb.lower(root.get("deliveryDate")), "%" + searchQuery.toLowerCase() + "%");
            }
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            Page<CustomerOrder> orderList = coRepository.findByDeliveryDate(date, spec, pageable);
            if (orderList.getTotalElements() < 1) {
                log.info("User {} requested the customer orders with a delivery date of: '{}'. NO DATA FOUND.", user.getUsername(), date);
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_CUSTOMER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            PagedModel<CustomerOrderDTO> customerOrderDTOSPage = convertList(orderList);
            customerOrderDTOSPage.add(linkTo(CustomerOrderController.class).slash("orders").slash("delivery=" + date).withRel("customers-orders"));
            log.info("User {} requested the customer orders with a delivery date of: '{}'. RETURNING DATA.",
                    user.getUsername(), date);
            return new ResponseEntity<>(customerOrderDTOSPage, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested the customer orders with a delivery date of: '{}'. UNEXPECTED ERROR!", user.getUsername(), date);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to create a new customer order.
     * Firstly, we check that the information given by the user are correct by using the orderValidation method.
     *      If an information is incorrect/invalid, we return a HttpStatus code.
     * Secondly, we create the new CustomerOrder object, and we set all the new different attributes.
     * Thirdly, after that the CustomerOrder object is created, we can save the data in the database.
     * Finally, we convert the CustomerOrder object as a CustomerOrderDTO object.
     * We can add the HATEOAS links and return the data to the user.
     * 
     * @param user Corresponds to the authenticated user.
     * @param orderCuDTO Corresponds to the new customer order that the user wants to save.
     * @return a ResponseEntity containing a CustomerOrderDTO objects or a Error Message.
     *      --> HttpStatus.CREATED if the customer order has been created. (CustomerOrderDTO)
     *      --> HttpStatus.XX if a criteria has not been validated. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PostMapping(value = "/orders", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<?> createCustomerOrder(@AuthenticationPrincipal Employee user, @RequestBody CustomerOrderCuDTO orderCuDTO) {
        try {
            log.info("User {} is requesting to create a new customer order.", user.getUsername());
            Pair<HttpStatus, String> validation = orderValidation(orderCuDTO);
            if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
                log.info("User {} requested to create a new customer order, date: '{}'. {}", user.getUsername(), orderCuDTO.getDate(), validation.getSecond());
                ErrorResponse bm = new ErrorResponse(validation.getFirst().getReasonPhrase(), validation.getSecond());
                return new ResponseEntity<>(bm, validation.getFirst());
            }
            CustomerOrder customerOrder = new CustomerOrder();
            customerOrder.setEmployee(user); customerOrder.setDate(orderCuDTO.getDate());
            customerOrder.setDeliveryDate(orderCuDTO.getDeliveryDate()); customerOrder.setSent(false);
            if (orderCuDTO.getCustomerId() != null) {
                Optional<Customer> customerOptional = cRepository.findById(orderCuDTO.getCustomerId());
                customerOrder.setCustomer(customerOptional.get());
            } else {
                customerOrder.setCustomer(null);
            }
            log.debug("User {} requested to create a new customer order, date: '{}'. SAVING DATA", user.getUsername(), customerOrder.getDate());
            CustomerOrder savedCustomerOrder = coRepository.save(customerOrder);
            CustomerOrderDTO customerOrderDTO = CustomerOrderDTO.convert(savedCustomerOrder);
            log.info("User {} requested to create a new customer order, date: '{}'. RETURNING DATA.", user.getUsername(), customerOrder.getDate());
            return new ResponseEntity<>(createHATEOAS(customerOrderDTO), HttpStatus.CREATED);
        } catch (Exception e) {
            log.info("User {} requested to create a new customer order. UNEXPECTED ERROR!", user.getUsername());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to modify a customer order by its id.
     * Firstly, we verify that the customer order exists.
     *      If not, we return to the user an HttpStatus.NO_CONTENT.
     * Secondly, we get the order from the Optional object.
     *      Then, we modify the delivery date field if the user doesn't provide null value.
     * Thirdly, we save the modification to the database, and we convert the saved object as a CustomerOrderDTO.
     * Finally, we return the modified data to the user with an HttpStatus.Ok.
     * 
     * @param id Corresponds to the id of the order to update.
     * @param user Corresponds to the authenticated user.
     * @param orderCuDTO Corresponds to the data that the user want to change.
     * @return a ResponseEntity containing a CustomerOrderDTO objects or an Error Message.
     *      --> HttpStatus.OK if the customer order has been updated. (CustomerOrderDTO)
     *      --> HttpStatus.BAD_REQUEST if no customer order corresponds to the given id. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PutMapping(value = "/orders/{id}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<?> updateOrder(@PathVariable(value = "id") Long id,
                                                       @AuthenticationPrincipal Employee user,
                                                       @RequestBody CustomerOrderCuDTO orderCuDTO) {
        try {
            log.info("User {} is requesting to update the customer order with id: '{}'.", user.getUsername(), id);
            Optional<CustomerOrder> orderOptional = coRepository.findById(id);
            if (!orderOptional.isPresent()) {
                log.info("User {} requested to update the customer order with id: '{}'. NO CUSTOMER ORDER FOUND.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_CUSTOMER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            CustomerOrder customerOrder = orderOptional.get();
            if (orderCuDTO.getDeliveryDate() != null) customerOrder.setDeliveryDate(orderCuDTO.getDeliveryDate());
            log.debug("User {} requested to update the customer order with id: '{}'. SAVING MODIFICATION.", user.getUsername(), id);
            CustomerOrder savedOrder = coRepository.save(customerOrder);
            CustomerOrderDTO customerOrderDTO = CustomerOrderDTO.convert(savedOrder);
            log.info("User {} requested to update the customer order with id: '{}'. RETURNING DATA.", user.getUsername(), id);
            return new ResponseEntity<>(createHATEOAS(customerOrderDTO), HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to update the customer order with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to ship a customer order.
     * We use the function customerOrderShipment provided by the CustomerOrderManagerImpl class.
     * Depending on the Exception returned by customerOrderShipment, we return an appropriate HttpStatus.
     *
     * @param orderId Corresponds to the id of the order to update.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing a CustomerOrderDTO objects or an Error Message.
     *      --> HttpStatus.OK if the customer order has been updated. (CustomerOrderDTO)
     *      --> HttpStatus.BAD_REQUEST if no customer order corresponds to the given id. (ErrorMessage)
     *      --> HttpStatus.NOT_MODIFIED if a product of the order had a problem. (ErrorMessage)
     *      --> HttpStatus.CONFLICT if no customer order is already received. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PutMapping(value = "/orders/{id}/send", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<?> sendOrder(@PathVariable(value = "id") Long orderId, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to send the customer order with id: '{}'.", user.getUsername(), orderId);
            CustomerOrder customerOrder = orderManager.customerOrderShipment(orderId);
            CustomerOrderDTO convert = CustomerOrderDTO.convert(customerOrder);
            createHATEOAS(convert);
            return new ResponseEntity<>(convert, HttpStatus.OK);
        } catch (UnknownOrderException e) {
            log.info("User {} requested to send the customer order with id: '{}'. NO CUSTOMER ORDER FOUND.", user.getUsername(), orderId);
            ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_CUSTOMER_ORDER_FOUND");
            return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
        } catch (ProductStockException e) {
            log.info("User {} requested to send the customer order with id: '{}'. NOT ENOUGH STOCK.", user.getUsername(), orderId);
            ErrorResponse bm = new ErrorResponse(HttpStatus.NOT_MODIFIED.getReasonPhrase(), "PRODUCT_STOCK_ERROR");
            return new ResponseEntity<>(bm, HttpStatus.NOT_MODIFIED);
        } catch (OrderStateException e) {
            log.info("User {} requested to send the customer order with id: '{}'. ORDER IS ALREADY SENT.", user.getUsername(), orderId);
            ErrorResponse bm = new ErrorResponse(HttpStatus.CONFLICT.getReasonPhrase(), "CUSTOMER_ORDER_ALREADY_SENT");
            return new ResponseEntity<>(bm, HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.info("User {} requested to send the customer order with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), orderId);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to cancel the shipment of a customer order.
     * We use the function customerOrderShipmentCancel provided by the CustomerOrderManagerImpl class.
     * Depending on the Exception returned by customerOrderShipmentCancel, we return an appropriate HttpStatus.
     *
     * @param orderId Corresponds to the id of the order to update.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing a CustomerOrderDTO objects or an Error Message.
     *      --> HttpStatus.OK if the customer order has been updated. (CustomerOrderDTO)
     *      --> HttpStatus.BAD_REQUEST if no customer order corresponds to the given id. (ErrorMessage)
     *      --> HttpStatus.CONFLICT if no customer order is not already received. (ErrorMessage)
     *      --> HttpStatus.NOT_MODIFIED if a product of the order had a problem. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PutMapping(value = "/orders/{id}/cancel-sending", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<?> cancelSendOrder(@PathVariable(value = "id") Long orderId, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to cancel the shipment of the customer order with id: '{}'.", user.getUsername(), orderId);
            CustomerOrder customerOrder = orderManager.customerOrderShipmentCancel(orderId);
            CustomerOrderDTO convert = CustomerOrderDTO.convert(customerOrder);
            createHATEOAS(convert);
            return new ResponseEntity<>(convert, HttpStatus.OK);
        } catch (UnknownOrderException e) {
            log.info("User {} requested to cancel the shipment of the customer order with id: '{}'. NO CUSTOMER ORDER FOUND.", user.getUsername(), orderId);
            ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_CUSTOMER_ORDER_FOUND");
            return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
        } catch (OrderStateException e) {
            log.info("User {} requested to cancel the shipment the customer order with id: '{}'. ORDER HAS NOT BEEN SENT.", user.getUsername(), orderId);
            ErrorResponse bm = new ErrorResponse(HttpStatus.CONFLICT.getReasonPhrase(), "CUSTOMER_ORDER_NOT_SENT");
            return new ResponseEntity<>(bm, HttpStatus.CONFLICT);
        } catch (ProductStockException e) {
            log.info("User {} requested to send the customer order with id: '{}'. {}.", user.getUsername(), orderId, e.getMessage());
            ErrorResponse bm = new ErrorResponse(HttpStatus.NOT_MODIFIED.getReasonPhrase(), "PRODUCT_STOCK_ERROR");
            return new ResponseEntity<>(bm, HttpStatus.NOT_MODIFIED);
        } catch (Exception e) {
            log.info("User {} requested to send the customer order with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), orderId);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to delete a customer order by its id.
     * Firstly, we check that a customer order exists with the corresponding id.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Secondly, we check that the age of the order is not more than three days old.
     *      If the order has been made more than 3 days ago, we return to the user an HttpStatus.NO_ACCEPTABLE.
     * Secondly, we can delete the object from the database.
     * Finally, we return to the user that the operation worked correctly.
     *
     * @param id Corresponds to the id of the customer order that the user wants to delete.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing an Error Message.
     *      --> HttpStatus.OK if the customer order has been deleted.
     *      --> HttpStatus.BAD_REQUEST if no customer order corresponds to the given id.
     *      --> HttpStatus.PRECONDITION_FAILED if the customer order is too old.
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs.
     */
    @DeleteMapping(value = "/orders/{id}", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<?> deleteOrder(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to delete the customer order with id: '{}'.", user.getUsername(), id);
            Optional<CustomerOrder> customerOrderOptional = coRepository.findById(id);
            if (!customerOrderOptional.isPresent()) {
                log.info("User {} requested to delete the customer order with id: '{}'. NO CUSTOMER ORDER FOUND.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_CUSTOMER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            CustomerOrder customerOrder = customerOrderOptional.get();
            if (customerOrder.getDate().plusDays(3).isAfter(LocalDate.now())) {
                log.info("User {} requested to delete the customer order with id: '{}'. ORDER TOO OLD.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.PRECONDITION_FAILED.getReasonPhrase(), "CUSTOMER_ORDER_TOO_OLD");
                return new ResponseEntity<>(bm, HttpStatus.PRECONDITION_FAILED);
            }
            log.debug("User {} requested to delete the customer order with id: '{}'. DELETING CUSTOMER ORDER.",
                    user.getUsername(), id);
            coRepository.deleteById(id);
            log.info("User {} requested to delete the customer order with id: '{}'. CUSTOMER ORDER DELETED.",
                    user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to delete the customer order with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_ADMIN
     * This function is used to delete a customer order by its id.
     * Firstly, we check that a customer exists with the corresponding id.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Secondly, we can delete the object from the database.
     * Finally, we return to the user that the operation worked correctly.
     *
     * @param id Corresponds to the id of the customer order that the user wants to delete.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing an Error Message.
     *      --> HttpStatus.OK if the customer order has been deleted.
     *      --> HttpStatus.BAD_REQUEST if no customer order corresponds to the given id.
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs.
     */
    @DeleteMapping(value = "/orders/{id}/force", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<?> deleteOrderForce(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to delete the customer order with id: '{}'.", user.getUsername(), id);
            if (!coRepository.existsById(id)) {
                log.info("User {} requested to delete the customer order with id: '{}'. NO CUSTOMER ORDER.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_CUSTOMER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            log.warn("User {} requested to delete the customer order with id: '{}'. DELETING CUSTOMER ORDER.", user.getUsername(), id);
            coRepository.deleteById(id);
            log.info("User {} requested to delete the customer order with id: '{}'. CUSTOMER ORDER DELETED.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to delete the customer order with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
