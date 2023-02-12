package fi.haagahelia.stockmanager.controller.customer.order;


import fi.haagahelia.stockmanager.controller.customer.CustomerController;
import fi.haagahelia.stockmanager.controller.user.EmployeeController;
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
import fi.haagahelia.stockmanager.service.order.CustomerOrderManagerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
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
@RequestMapping("/api/customers")
public class CustomerOrderController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final CustomerRepository cRepository;
    private final CustomerOrderRepository coRepository;
    private final CustomerOrderManagerImpl orderManager;

    @Autowired
    public CustomerOrderController(CustomerRepository cRepository, CustomerOrderRepository coRepository,
                                   CustomerOrderManagerImpl orderManager) {
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
        Link selfLink = linkTo(CustomerOrderController.class)
                .slash("/orders" + customerOrderDTO.getId()).withSelfRel();
        customerOrderDTO.add(selfLink);

        Link all = linkTo(CustomerOrderController.class).slash("/orders").withRel("customer-orders");
        customerOrderDTO.add(all);

        if (customerOrderDTO.getEmployeeDTO() != null) {
            Link vendor = linkTo(EmployeeController.class)
                    .slash(customerOrderDTO.getEmployeeDTO().getId()).withRel("employee");
            customerOrderDTO.add(vendor);
        }

        if (customerOrderDTO.getCustomerDTO() != null) {
            Link customer = linkTo(CustomerController.class)
                    .slash(customerOrderDTO.getCustomerDTO().getId()).withRel("customer");
            customerOrderDTO.add(customer);
        }

        if (customerOrderDTO.getCustomerDTO() != null) {
            Long customerId = customerOrderDTO.getId();
            Link orderOfACustomer = linkTo(CustomerOrderController.class)
                    .slash("/" + customerId + "/orders").withRel("Orders from this customer");
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
    private List<CustomerOrderDTO> convertList(List<CustomerOrder> customerOrders) {
        List<CustomerOrderDTO> customerOrderDTOS = new ArrayList<>();
        for (CustomerOrder customerOrder : customerOrders) {
            CustomerOrderDTO customerOrderDTO = CustomerOrderDTO.convert(customerOrder);
            createHATEOAS(customerOrderDTO);
            customerOrderDTOS.add(customerOrderDTO);
        }
        return customerOrderDTOS;
    }

    /**
     * This function is used to validate the creation of an order.
     * It checked that the date and the delivery date are not null.
     * @param orderCuDTO Corresponds to the order to check
     * @return A ResponseEntity with the related code and string that contains the reason of the decision.
     */
    private Pair<HttpStatus, String> orderValidation(CustomerOrderCuDTO orderCuDTO) {
        if (orderCuDTO.getDate() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "DATE INVALID.");
        if (orderCuDTO.getDeliveryDate() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "DELIVERY DATE INVALID.");
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
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @GetMapping(value = "/orders", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<List<CustomerOrderDTO>> getCustOrders(@AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting all the customers' orders.", user.getUsername());
        List<CustomerOrder> customerOrders = coRepository.findAll();
        if (customerOrders.size() == 0) {
            log.info("User {} requested all the customers' orders. NO DATA FOUND.", user.getUsername());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<CustomerOrderDTO> customerOrderDTOS = convertList(customerOrders);
        log.info("User {} requested all the customers' orders. RETURNING DATA.", user.getUsername());
        return new ResponseEntity<>(customerOrderDTOS, HttpStatus.OK);
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get a customer order by its id.
     * Firstly, we find the customer order by id, using the CustomerOrderRepository.
     * Secondly, we check that the returned Optional contains an order.
     *      If not, we return to the customer an HttpStatus.NO_CONTENT.
     * Thirdly, we convert the CustomerOrder as a CustomerOrderDTO.
     * Finally, we add the HATEOAS links to the CustomerOrderDTO and we return all the data to the user.
     * @param id Correspond to the id of the customer order that the user want to see.
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @GetMapping(value = "/orders/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<CustomerOrderDTO> getCustomerOrderById(@PathVariable(value = "id") Long id,
                                                                               @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting the customer order with id: {}", user.getUsername(), id);
        Optional<CustomerOrder> orderOptional = coRepository.findById(id);
        if (orderOptional.isEmpty()) {
            log.info("User {} requested the customer order with id: {}. NO DATA FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        CustomerOrderDTO customerOrderDTO = CustomerOrderDTO.convert(orderOptional.get());
        log.info("User {} requested the customer order with id: {}. RETURNING DATA.", user.getUsername(), id);
        return new ResponseEntity<>(createHATEOAS(customerOrderDTO), HttpStatus.OK);
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
     * @param id Corresponds to the id of the customer.
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @GetMapping(value = "/{id}/orders", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<List<CustomerOrderDTO>> getCustomerOrders(@PathVariable(value = "id") Long id,
                                                                                  @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting all the orders of the customer: {}.", user.getUsername(), id);
        if (!cRepository.existsById(id)) {
            log.info("User {} requested all the orders of the customer: {}. NO CUSTOMER FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<CustomerOrder> customerOrders = coRepository.findByCustomerId(id);
        if (customerOrders.size() == 0) {
            log.info("User {} requested all the orders of the customer: {}. NO ORDER FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<CustomerOrderDTO> customerOrderDTOS = convertList(customerOrders);
        log.info("User {} requested all the orders of the customer: {}. RETURNING DATA.", user.getUsername(), id);
        return new ResponseEntity<>(customerOrderDTOS, HttpStatus.OK);
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
     * @param date Corresponds to the delivery date the user wants the sales orders.
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @GetMapping(value = "/orders/deliverydate={date}", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<List<CustomerOrderDTO>> getCustOrdersDate(@PathVariable(value = "date") LocalDate date,
                                                                                  @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting the customer orders with a delivery date of: {}.", user.getUsername(), date);
        if (date == null) {
            log.info("User {} requested the customer orders with a delivery date that is null.", user.getUsername());
            return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
        }
        List<CustomerOrder> orderList = coRepository.findByDeliveryDate(date);
        if (orderList.size() < 1) {
            log.info("User {} requested the customer orders with a delivery date of: {}. NO DATA FOUND.",
                    user.getUsername(), date);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<CustomerOrderDTO> customerOrderDTOS = convertList(orderList);
        log.info("User {} requested the customer orders with a delivery date of: {}. RETURNING DATA.",
                user.getUsername(), date);
        return new ResponseEntity<>(customerOrderDTOS, HttpStatus.OK);
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
     * @param user Corresponds to the authenticated user.
     * @param orderCuDTO Corresponds to the new customer order that the user wants to save.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @PostMapping(value = "/orders", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<CustomerOrderDTO> createCustomerOrder(@AuthenticationPrincipal Employee user,
                                                                              @RequestBody CustomerOrderCuDTO orderCuDTO) {
        log.info("User {} is requesting to create a new customer order.", user.getUsername());
        Pair<HttpStatus, String> orderValidation = orderValidation(orderCuDTO);
        if (!orderValidation.getFirst().equals(HttpStatus.ACCEPTED)) {
            log.info("User {} requested to create a new customer order, date: {}. {}",
                    user.getUsername(), orderCuDTO.getDate(), orderValidation.getSecond());
            return new ResponseEntity<>(orderValidation.getFirst());
        }
        CustomerOrder customerOrder = new CustomerOrder();
        customerOrder.setEmployee(user);
        customerOrder.setDate(orderCuDTO.getDate());
        customerOrder.setDeliveryDate(orderCuDTO.getDeliveryDate());
        customerOrder.setSent(false);
        if (orderCuDTO.getCustomerId() != null) {
            Optional<Customer> customerOptional = cRepository.findById(orderCuDTO.getCustomerId());
            customerOrder.setCustomer(customerOptional.get());
        } else {
            customerOrder.setCustomer(null);
        }
        log.debug("User {} requested to create a new customer order, date: {}. SAVING DATA",
                user.getUsername(), customerOrder.getDate());
        CustomerOrder savedCustomerOrder = coRepository.save(customerOrder);
        CustomerOrderDTO customerOrderDTO = CustomerOrderDTO.convert(savedCustomerOrder);
        log.info("User {} requested to create a new customer order, date: {}. RETURNING DATA.",
                user.getUsername(), customerOrder.getDate());
        return new ResponseEntity<>(createHATEOAS(customerOrderDTO), HttpStatus.CREATED);
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
     * @param id Corresponds to the id of the order to update.
     * @param user Corresponds to the authenticated user.
     * @param orderCuDTO Corresponds to the data that the user want to change.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @PutMapping(value = "/orders/{id}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<CustomerOrderDTO> updateOrder(@PathVariable(value = "id") Long id,
                                                                      @AuthenticationPrincipal Employee user,
                                                                      @RequestBody CustomerOrderCuDTO orderCuDTO) {
        log.info("User {} is requesting to update the customer order with id: {}.", user.getUsername(), id);
        Optional<CustomerOrder> orderOptional = coRepository.findById(id);
        if (orderOptional.isEmpty()) {
            log.info("User {} requested to update the customer order with id: {}. NO CUSTOMER ORDER FOUND.",
                    user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        CustomerOrder customerOrder = orderOptional.get();
        if (orderCuDTO.getDeliveryDate() != null) customerOrder.setDeliveryDate(orderCuDTO.getDeliveryDate());
        log.debug("User {} requested to update the customer order with id: {}. SAVING MODIFICATION.",
                user.getUsername(), customerOrder.getId());
        CustomerOrder savedOrder = coRepository.save(customerOrder);
        CustomerOrderDTO customerOrderDTO = CustomerOrderDTO.convert(savedOrder);
        log.info("User {} requested to update the customer order with id: {}. RETURNING DATA.",
                user.getUsername(), savedOrder.getId());
        return new ResponseEntity<>(createHATEOAS(customerOrderDTO), HttpStatus.ACCEPTED);
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to ship a customer order.
     * We use the function customerOrderShipment provided by the CustomerOrderManagerImpl class.
     * Depending on the Exception returned by customerOrderShipment, we return an appropriate HttpStatus.
     * @param orderId Corresponds to the id of the order to update.
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @PutMapping(value = "/orders/{id}/send", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<CustomerOrderDTO> sendOrder(@PathVariable(value = "id") Long orderId,
                                                                    @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to send the customer order with id: {}", user.getUsername(), orderId);
        try {
            CustomerOrder customerOrder = orderManager.customerOrderShipment(orderId);
            CustomerOrderDTO convert = CustomerOrderDTO.convert(customerOrder);
            createHATEOAS(convert);
            return new ResponseEntity<>(convert, HttpStatus.ACCEPTED);
        } catch (UnknownOrderException e) {
            log.info("User {} requested to send the customer order with id: {}. NO CUSTOMER ORDER FOUND.",
                    user.getUsername(), orderId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (ProductStockException e) {
            log.info("User {} requested to send the customer order with id: {}. NOT ENOUGH STOCK.",
                    user.getUsername(), orderId);
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        } catch (OrderStateException e) {
            log.info("User {} requested to send the customer order with id: {}. ORDER IS ALREADY SENT.",
                    user.getUsername(), orderId);
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to cancel the shipment of a customer order.
     * We use the function customerOrderShipmentCancel provided by the CustomerOrderManagerImpl class.
     * Depending on the Exception returned by customerOrderShipmentCancel, we return an appropriate HttpStatus.
     * @param orderId Corresponds to the id of the order to update.
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @PutMapping(value = "/orders/{id}/cancel-sending", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<CustomerOrderDTO> cancelSendOrder(@PathVariable(value = "id") Long orderId,
                                                                          @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to cancel the shipment of the customer order with id: {}", user.getUsername(), orderId);
        try {
            CustomerOrder customerOrder = orderManager.customerOrderShipmentCancel(orderId);
            CustomerOrderDTO convert = CustomerOrderDTO.convert(customerOrder);
            createHATEOAS(convert);
            return new ResponseEntity<>(convert, HttpStatus.ACCEPTED);
        } catch (UnknownOrderException e) {
            log.info("User {} requested to cancel the shipment of the customer order with id: {}. NO CUSTOMER ORDER FOUND.",
                    user.getUsername(), orderId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (OrderStateException e) {
            log.info("User {} requested to cancel the shipment the customer order with id: {}. ORDER HAS NOT BEEN SENT.",
                    user.getUsername(), orderId);
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        } catch (ProductStockException e) {
            log.info("User {} requested to send the customer order with id: {}. {}.",
                    user.getUsername(), orderId, e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
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
     * @param id Corresponds to the id of the customer order that the user wants to delete.
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @DeleteMapping(value = "/orders/{id}", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<CustomerOrderDTO> deleteOrderById(@PathVariable(value = "id") Long id,
                                                                          @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to delete the customer order with id: {}.", user.getUsername(), id);
        Optional<CustomerOrder> customerOrderOptional = coRepository.findById(id);
        if (customerOrderOptional.isEmpty()) {
            log.info("User {} requested to delete the customer order with id: {}. NO CUSTOMER ORDER FOUN.",
                    user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        CustomerOrder customerOrder = customerOrderOptional.get();
        if (customerOrder.getDate().plusDays(3).isAfter(LocalDate.now())) {
            log.info("User {} requested to delete the customer order with id: {}. ORDER TOO OLD.",
                    user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        log.debug("User {} requested to delete the customer order with id: {}. DELETING CUSTOMER ORDER.",
                user.getUsername(), id);
        coRepository.deleteById(id);
        log.info("User {} requested to delete the customer order with id: {}. CUSTOMER ORDER DELETED.",
                user.getUsername(), id);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    /**
     * AVAILABLE FOR: ROLE_ADMIN
     * This function is used to delete a customer order by its id.
     * Firstly, we check that a customer exists with the corresponding id.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Secondly, we can delete the object from the database.
     * Finally, we return to the user that the operation worked correctly.
     * @param id Corresponds to the id of the customer order that the user wants to delete.
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @DeleteMapping(value = "/orders/{id}/force", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<CustomerOrderDTO> deleteOrderByIdForce(@PathVariable(value = "id") Long id,
                                                                               @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to delete the customer order with id: {}.", user.getUsername(), id);
        if (!coRepository.existsById(id)) {
            log.info("User {} requested to delete the customer order with id: {}. NO CUSTOMER ORDER.",
                    user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        log.warn("User {} requested to delete the customer order with id: {}. DELETING CUSTOMER ORDER.",
                user.getUsername(), id);
        coRepository.deleteById(id);
        log.info("User {} requested to delete the customer order with id: {}. CUSTOMER ORDER DELETED.",
                user.getUsername(), id);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
