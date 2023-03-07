package fi.haagahelia.stockmanager.controller.supplier.order;


import fi.haagahelia.stockmanager.controller.supplier.SupplierController;
import fi.haagahelia.stockmanager.dto.common.ErrorResponse;
import fi.haagahelia.stockmanager.dto.supplier.order.SupplierOrderCuDTO;
import fi.haagahelia.stockmanager.dto.supplier.order.SupplierOrderDTO;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.supplier.SupplierRepository;
import fi.haagahelia.stockmanager.repository.supplier.order.SupplierOrderRepository;
import fi.haagahelia.stockmanager.service.order.SupplierOrderService;
import fi.haagahelia.stockmanager.exception.ProductStockException;
import fi.haagahelia.stockmanager.exception.OrderStateException;
import fi.haagahelia.stockmanager.exception.UnknownOrderException;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/api/suppliers/")
public class SupplierOrderController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final SupplierOrderRepository sOrderRepository;
    private final SupplierRepository sRepository;
    private final SupplierOrderService orderManager;

    @Autowired
    public SupplierOrderController(SupplierOrderRepository sOrderRepository, SupplierRepository sRepository,
                                   SupplierOrderService orderManager) {
        this.sOrderRepository = sOrderRepository;
        this.sRepository = sRepository;
        this.orderManager = orderManager;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */


    /**
     * This method is used to create and add the HATEOAS links to a DTO models.
     * @param orderDTO The dto model to which we will add the HATEOAS links.
     * @return The dto model with the HATEOAS links.
     */
    private SupplierOrderDTO createHATEOAS(SupplierOrderDTO orderDTO) {
        Link selfLink = linkTo(SupplierOrderController.class).slash(String.valueOf(orderDTO.getId())).withSelfRel();
        orderDTO.add(selfLink);

        Link collectionLink = linkTo(SupplierOrderController.class).slash("").withRel("suppliers-orders");
        orderDTO.add(collectionLink);

        // Supplier related field
        if (orderDTO.getSupplierDTO() != null) {
            Link supplierLink = linkTo(SupplierController.class)
                    .slash(orderDTO.getSupplierDTO().getId()).withRel("supplier");
            orderDTO.add(supplierLink);
        }
        return orderDTO;
    }

    /**
     * This function is used to convert a List of SupplierOrder into a List of SupplierOrderDTO.
     * It also adds the HATEOAS links on each element of the list.
     * @param supplierOrders Corresponds to the list of supplier orders.
     * @return Corresponds to the list of SupplierOrderDTO.
     */
    private PagedModel<SupplierOrderDTO> convertSupplierOrder(Page<SupplierOrder> supplierOrders) {
        List<SupplierOrderDTO> supplierOrderDTOS = new ArrayList<>();
        for (SupplierOrder supOrder : supplierOrders) {
            SupplierOrderDTO supOrderDTO = SupplierOrderDTO.convert(supOrder);
            createHATEOAS(supOrderDTO);
            supplierOrderDTOS.add(supOrderDTO);
        }
        PagedModel.PageMetadata pmd = new PagedModel.PageMetadata(supplierOrders.getSize(), supplierOrders.getNumber(), supplierOrders.getTotalElements());
        return PagedModel.of(supplierOrderDTOS, pmd);
    }

    private Pair<HttpStatus, String> orderValidation(SupplierOrderCuDTO orderCuDTO) {
        if (orderCuDTO.getDate() == null) return Pair.of(HttpStatus.BAD_REQUEST, "SUPPLIER_ORDER_DATE__INVALID.");
        if (orderCuDTO.getDeliveryDate() == null) return Pair.of(HttpStatus.BAD_REQUEST, "SUPPLIER_ORDER_DELIVERY_DATE__INVALID.");
        if (orderCuDTO.getIsReceived() == null) return Pair.of(HttpStatus.BAD_REQUEST, "SUPPLIER_ORDER_RECEIVE_STATUS__INVALID.");
        if (orderCuDTO.getOrderIsSent() == null) return Pair.of(HttpStatus.BAD_REQUEST, "SUPPLIER_ORDER_IS_SENT_STATUS__INVALID.");
        if (orderCuDTO.getIsReceived() && !orderCuDTO.getOrderIsSent()) {
            return Pair.of(HttpStatus.BAD_REQUEST, "SUPPLIER_ORDER_CANNOT_BE_RECEIVED_CAUSE_NOT_SENT.");
        }
        if (orderCuDTO.getSupplierId() == null) return Pair.of(HttpStatus.BAD_REQUEST, "SUPPLIER_ID_INVALID.");
        return Pair.of(HttpStatus.ACCEPTED, "");
    }


    /* ------------------------------------------------- API METHODS ------------------------------------------------ */

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get all the orders from the database.
     * Firstly, we findALl data by using the supplierOrderRepository.
     * Secondly, we check if the size of the list is equals to 0.
     *      If it is the case, we return an HttpStatus.NO_CONTENT to the user.
     * Thirdly (at least one supplier order), we convert each supplierOrder as a SupplierOrderDTO.
     * At the same time, we also add the HATEOAS links by using the createHATEOAS function.
     * Finally, we can return the list with the SupplierOrderDTOs and an HttpStatusCode.Ok
     *
     * @return a ResponseEntity containing a page model of SupplierOrderDTO objects or a Error Message.
     *      --> HttpStatus.OK if at least one supplier order has been found. (Page of SupplierOrderDTO)
     *      --> HttpStatus.NO_CONTENT if no supplier order exists. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/orders",produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<?> getSupplierOrders(@AuthenticationPrincipal Employee user,
                                                             @RequestParam(required = false) String searchQuery,
                                                             @PageableDefault(size = 10) Pageable pageable,
                                                             @SortDefault.SortDefaults({
                                                                     @SortDefault(sort = "id", direction = Sort.Direction.ASC)}) Sort sort) {
        try {
            log.info("User {} is requesting all the supplier orders.", user.getUsername());
            Specification<SupplierOrder> spec = null;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                spec = (root, query, cb) -> cb.like(cb.lower(root.get("id")), "%" + searchQuery.toLowerCase() + "%");
            }
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            Page<SupplierOrder> supplierOrders = sOrderRepository.findAll(spec, pageable);
            if (supplierOrders.getTotalElements() < 1) {
                log.info("User {} requested all the supplier orders. NO DATA FOUND.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_SUPPLIER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            PagedModel<SupplierOrderDTO> supplierOrderDTOPage = convertSupplierOrder(supplierOrders);
            supplierOrderDTOPage.add(linkTo(SupplierOrderController.class).slash("orders").withRel("suppliers-orders"));
            log.info("User {} requested all the supplier orders. RETURNING DATA.", user.getUsername());
            return new ResponseEntity<>(supplierOrderDTOPage, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested all the supplier orders. UNEXPECTED ERROR!", user.getUsername());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to find a supplier order by its id.
     * Firstly, we search in the database the supplier order by the given id.
     * Secondly, we check if the Optional object is empty or not.
     *      If the Optional object is empty, we return an HttpStatus.NO_CONTENT.
     * Thirdly (else), we Convert the SupplierOrder object that is in the Optional as a SupplierOrderDTO.
     * Finally, we add the HATEOAS links to the SupplierOrderDTO object, and we return the data to the user.
     *
     * @param id Correspond to the id of the order searched by the user.
     * @return a ResponseEntity containing a SupplierOrderDTO objects or a Error Message.
     *      --> HttpStatus.OK if the supplier order exists. (SupplierOrderDTO)
     *      --> HttpStatus.BAD_REQUEST if no supplier order corresponds to the id. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/orders/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<?> getSupplierOrder(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting the supplier order with id: '{}'", user.getUsername(), id);
            Optional<SupplierOrder> supOrderOptional = sOrderRepository.findById(id);
            if (!supOrderOptional.isPresent()) {
                log.info("User {} requested the supplier order with id: '{}'. NO DATA FOUND.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_SUPPLIER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            SupplierOrderDTO supplierOrderDTO = SupplierOrderDTO.convert(supOrderOptional.get());
            log.info("User {} requested the supplier order with id: '{}'. RETURNING DATA.", user.getUsername(), id);
            return new ResponseEntity<>(createHATEOAS(supplierOrderDTO), HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested the supplier order with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get all the orders from a supplier.
     * Firstly, we check that the supplier exists.
     *      If not, we return an HttpStatus.BAD_REQUEST to the user.
     * Secondly, we search all the orders that the suo_sup_id is equals to the id given by the user.
     *      We also check that the list has a size of 0. If it is the case, we return an HttpStatus.NO_CONTENT to the user.
     * Thirdly (at least one order), we convert each SupplierOrder as a SupplierOrderDTO.
     * We also add the HATEOAS links at the same time.
     * Finally, we return the list of SupplierOrderDTO to the user with an HttpStatus.OK.
     *
     * @param id Correspond to the id of the Supplier.
     * @param user authenticated Employee object
     * @param pageable pagination information (page number, size, and sorting)
     * @param sort sorting information for the query
     * @return a ResponseEntity containing a page model of SupplierOrderDTO objects or a Error Message.
     *      --> HttpStatus.OK if at least one supplier order has been found. (Page of SupplierOrderDTO)
     *      --> HttpStatus.BAD_REQUEST if no supplier exists with the given id. (ErrorMessage)
     *      --> HttpStatus.NO_CONTENT if no supplier order exists. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/{id}/orders", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<?> getSpecSupplierOrders(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user,
                                                                 @PageableDefault(size = 10) Pageable pageable,
                                                                 @SortDefault.SortDefaults({
                                                                         @SortDefault(sort = "id", direction = Sort.Direction.ASC)}) Sort sort) {
        try {
            log.info("User {} is requesting all the orders related to the supplier: '{}'", user.getUsername(), id);
            if (!sRepository.existsById(id)) {
                log.info("User {} requested the orders related to the supplier: '{}'. NO SUPPLIER WITH THIS ID.",
                        user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_SUPPLIER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            Page<SupplierOrder> supplierOrders = sOrderRepository.findBySupplierId(id, pageable);
            if (supplierOrders.getSize() < 1) {
                log.info("User {} requested the orders related to the supplier: '{}'. NO DATA FOUND.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_SUPPLIER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            PagedModel<SupplierOrderDTO> supplierOrderDTOPage = convertSupplierOrder(supplierOrders);
            supplierOrderDTOPage.add(linkTo(SupplierOrderController.class).slash(id).slash("orders").withRel("suppliers-orders"));
            log.info("User {} requested the orders related to the supplier: '{}'. RETURNING DATA.", user.getUsername(), id);
            return new ResponseEntity<>(supplierOrderDTOPage, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested the orders related to the supplier: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            System.out.println(e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to find all the supplier orders that have a specific delivery date.
     * Firstly, we check that the date given by the user is valid (not null).
     *      If the date is null, we return to the user an HttpStatus.PRECONDITION_FAILED.
     * Secondly, we find all the orders that have the given date in the database.
     *      We also check that the list returned by this step is not empty,
     *      if the list is empty, we return an HttpStatus.NO_CONTENT to the user.
     * Finally, we convert all the SupplierOrders as a SupplierOrdersDTO by using the convertSupplierOrder function.
     * When the list has been converted, we can return data to the user with an HttpStatus.OK.
     *
     * @param date Corresponds to the delivery date the user wants the sales orders.
     * @param user authenticated Employee object
     * @param pageable pagination information (page number, size, and sorting)
     * @param sort sorting information for the query
     * @return a ResponseEntity containing a page model of SupplierOrderDTO objects or a Error Message.
     *      --> HttpStatus.OK if at least one supplier order has been found. (Page of SupplierOrderDTO)
     *      --> HttpStatus.UNPROCESSABLE_ENTITY if the delivery date is null. (ErrorMessage)
     *      --> HttpStatus.NO_CONTENT if no supplier order exists. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/orders/delivery={date}", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<?> getSupOrdersDate(@PathVariable(value = "date") LocalDate date, @AuthenticationPrincipal Employee user,
                                                            @PageableDefault(size = 10) Pageable pageable,
                                                            @SortDefault.SortDefaults({
                                                                    @SortDefault(sort = "deliveryDate", direction = Sort.Direction.ASC)}) Sort sort) {
        try {
            log.info("User {} is requesting all the supplier orders related to the date: '{}'", user.getUsername(), date);
            if (date == null) {
                log.info("User {} requested the supplier orders with a delivery date that is null.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(), "URL_DELIVERY_DATE_NULL");
                return new ResponseEntity<>(bm, HttpStatus.UNPROCESSABLE_ENTITY);
            }
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            Page<SupplierOrder> supplierOrders = sOrderRepository.findByDeliveryDate(date, pageable);
            if (supplierOrders.getTotalElements() < 1) {
                log.info("User {} requested the supplier orders with a delivery date: '{}'. NO ORDERS FOUND.", user.getUsername(), date);
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_SUPPLIER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            PagedModel<SupplierOrderDTO> supplierOrderDTOPage = convertSupplierOrder(supplierOrders);
            supplierOrderDTOPage.add(linkTo(SupplierOrderController.class).slash("orders").slash("delivery=" + date).withRel("suppliers-orders"));
            log.info("User {} requested the supplier orders with a delivery date: '{}'. RETURNING ORDERS.", user.getUsername(), date);
            return new ResponseEntity<>(supplierOrderDTOPage, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested the supplier orders with a delivery date: '{}'. UNEXPECTED ERROR!", user.getUsername(), date);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to create a new supplier order.
     * Firstly, we verify that the data provided by the user are valid. So, we call the orderValidation method.
     *      If an information is incorrect/invalid, we return a HttpStatus code.
     * Secondly, we create the new SupplierOrder object, and we set all the new different attributes.
     * Thirdly, after that the SupplierOrder object is created, we can save the data in the database.
     * Finally, we convert the SupplierOrder object as a SupplierOrderDTO object.
     * We can add the HATEOAS links and return the data to the user.
     *
     * @param orderCuDTO Corresponds to the new supplier order that the user wants to save.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing a SupplierOrderDTO objects or a Error Message.
     *      --> HttpStatus.CREATED if the supplier order has been created. (SupplierOrderDTO)
     *      --> HttpStatus.XX if a criteria has not been validated. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PostMapping(value = "/orders", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<?> createSupplierOrder(@RequestBody SupplierOrderCuDTO orderCuDTO, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to create a new supplier order.", user.getUsername());
            Pair<HttpStatus, String> validation = orderValidation(orderCuDTO);
            if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
                log.info("User {} requested to create a new supplier order, date: '{}'. {}",
                        user.getUsername(), orderCuDTO.getDate(), validation.getSecond());
                ErrorResponse bm = new ErrorResponse(validation.getFirst().getReasonPhrase(), validation.getSecond());
                return new ResponseEntity<>(bm, validation.getFirst());
            }
            Optional<Supplier> supplierOptional = sRepository.findById(orderCuDTO.getSupplierId());
            if (!supplierOptional.isPresent()) {
                log.info("User {} requested to create a new supplier order, date: '{}'. NO SUPPLIER FOUND", user.getUsername(), orderCuDTO.getDate());
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "SUPPLIER_NOT_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            SupplierOrder supplierOrder = new SupplierOrder();
            supplierOrder.setSupplier(supplierOptional.get());
            supplierOrder.setDate(orderCuDTO.getDate());
            supplierOrder.setDeliveryDate(orderCuDTO.getDeliveryDate());
            supplierOrder.setOrderIsSent(orderCuDTO.getOrderIsSent());
            supplierOrder.setReceived(orderCuDTO.getIsReceived());
            log.debug("User {} requested to create a new supplier order, date: '{}'. SAVING ORDER.", user.getUsername(), supplierOrder.getDate());
            SupplierOrder savedOrder = sOrderRepository.save(supplierOrder);
            SupplierOrderDTO supplierOrderDTO = SupplierOrderDTO.convert(savedOrder);
            createHATEOAS(supplierOrderDTO);
            log.info("User {} requested to create a new supplier order, date: '{}'. RETURNING SAVED ORDER.", user.getUsername(), supplierOrder.getDate());
            return new ResponseEntity<>(supplierOrderDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            log.info("User {} requested to create a new supplier order, date: '{}'. UNEXPECTED ERROR!", user.getUsername(), orderCuDTO.getDate());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to update an existing supplier order by its id.
     * Firstly, we find the supplier order in the database, using the supplier order repository.
     *      Then, we check that the Optional return is empty or not.
     *      If the optional is empty, we return to the user an HttpStatus.NO_CONTENT.
     * Secondly, we can update the following fields of the supplier order: is sent, delivery date, is received.
     * Thirdly, we can save the modification in the database.
     * Finally, we convert the saved SupplierOrder as a SupplierOrderDTO and we add the HATEOAS links.
     * After that, we can return the saved SupplierOrderDTO to the user with an HttpStatus.ACCEPTED.
     *
     * @param id Corresponds to the id of the supplier order to update.
     * @param orderCuDTO Corresponds to the new data that the user wants to change.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing a SupplierOrderDTO objects or a Error Message.
     *      --> HttpStatus.OK if the supplier order has been updated. (SupplierOrderDTO)
     *      --> HttpStatus.BAD_REQUEST if no supplier order corresponds to the given id. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PutMapping(value = "/orders/{id}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<?> updateSupOrder(@PathVariable(value = "id") Long id,
                                                          @RequestBody SupplierOrderCuDTO orderCuDTO,
                                                          @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to update the supplier order with id: '{}'.", user.getUsername(), id);
            Optional<SupplierOrder> orderOptional = sOrderRepository.findById(id);
            if (!orderOptional.isPresent()) {
                log.info("User {} requested to update the supplier order with id: '{}'. NO SUPPLIER ORDER FOUND", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_SUPPLIER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            SupplierOrder supplierOrder = orderOptional.get();
            if (orderCuDTO.getDeliveryDate() != null) supplierOrder.setDeliveryDate(orderCuDTO.getDeliveryDate());
            // if (orderCuDTO.getOrderIsSent() != null) supplierOrder.setOrderIsSent(orderCuDTO.getOrderIsSent());
            // if (orderCuDTO.getIsReceived() != null) supplierOrder.setReceived(orderCuDTO.getIsReceived());
            log.debug("User {} requested to update the supplier order with id: '{}'. SAVING ORDER'S UPDATE.", user.getUsername(), supplierOrder.getDate());
            SupplierOrder savedOrder = sOrderRepository.save(supplierOrder);
            SupplierOrderDTO supplierOrderDTO = SupplierOrderDTO.convert(savedOrder);
            createHATEOAS(supplierOrderDTO);
            log.info("User {} requested to update the supplier order with id: '{}'. RETURNING UPDATED ORDER.", user.getUsername(), supplierOrder.getDate());
            return new ResponseEntity<>(supplierOrderDTO, HttpStatus.ACCEPTED);
        } catch (Exception e) {
            log.info("User {} requested to update the supplier order with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), orderCuDTO.getDate());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to send an order to a supplier.
     * We use the function sendOrderById provided by the SupplierOrderManagerImpl class.
     * Depending on the Exception returned by sendOrderById, we return an appropriate HttpStatus.
     *
     * @param id Corresponds to the id of the supplier order to send.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing a SupplierOrderDTO objects or an Error Message.
     *      --> HttpStatus.OK if the supplier order has been updated. (SupplierOrderDTO)
     *      --> HttpStatus.BAD_REQUEST if no supplier order corresponds to the given id. (ErrorMessage)
     *      --> HttpStatus.CONFLICT if no supplier order is already sent. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PutMapping(value = "/orders/{id}/send", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<?> sendOrder(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to send the supplier order with id: {}.", user.getUsername(), id);
        try {
            SupplierOrder supplierOrder = orderManager.sendOrderById(id);
            SupplierOrderDTO convert = SupplierOrderDTO.convert(supplierOrder);
            createHATEOAS(convert);
            return new ResponseEntity<>(convert, HttpStatus.OK);
        } catch (UnknownOrderException e) {
            log.info("User {} requested to change the receive state of the supplier order with id: '{}'. NO SUPPLIER ORDER FOUND.", user.getUsername(), id);
            ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_SUPPLIER_ORDER_FOUND");
            return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
        } catch (OrderStateException e) {
            log.info("User {} requested to change the state of the supplier order with id: '{}'. ORDER IS ALREADY SENT.", user.getUsername(), id);
            ErrorResponse bm = new ErrorResponse(HttpStatus.CONFLICT.getReasonPhrase(), "SUPPLIER_ORDER_ALREADY_SENT_OR_NO_LINES");
            return new ResponseEntity<>(bm, HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.info("User {} requested to change the state of the supplier order with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to considerate a supplier order as received.
     * We use the function receiveOrderById provided by the SupplierOrderManagerImpl class.
     * Depending on the Exception returned by receiveOrderById, we return an appropriate HttpStatus.
     *
     * @param id Corresponds to the id of the supplier order to send.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing a SupplierOrderDTO objects or an Error Message.
     *      --> HttpStatus.OK if the supplier order has been updated. (SupplierOrderDTO)
     *      --> HttpStatus.BAD_REQUEST if no supplier order corresponds to the given id. (ErrorMessage)
     *      --> HttpStatus.CONFLICT if no supplier order is already received. (ErrorMessage)
     *      --> HttpStatus.NOT_MODIFIED if a product of the order had a problem. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PutMapping(value = "/orders/{id}/received", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<?> receivedOrder(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to change the receive state of the supplier order with id: {}.", user.getUsername(), id);
        try {
            SupplierOrder supplierOrder = orderManager.receiveOrderById(id);
            SupplierOrderDTO convert = SupplierOrderDTO.convert(supplierOrder);
            createHATEOAS(convert);
            return new ResponseEntity<>(convert, HttpStatus.OK);
        } catch (UnknownOrderException e) {
            log.info("User {} requested to change the receive state of the supplier order with id: '{}'. NO SUPPLIER ORDER FOUND.", user.getUsername(), id);
            ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_SUPPLIER_ORDER_FOUND");
            return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
        } catch (OrderStateException e) {
            log.info("User {} requested to change the state of the supplier order with id: '{}'. ORDER IS NOT SENT OR ALREADY RECEIVED.", user.getUsername(), id);
            ErrorResponse bm = new ErrorResponse(HttpStatus.CONFLICT.getReasonPhrase(), "SUPPLIER_ORDER_ALREADY_RECEIVED_OR_NOT_SENT");
            return new ResponseEntity<>(bm, HttpStatus.CONFLICT);
        } catch (ProductStockException e) {
            log.info("User {} requested to change the receive state of the supplier order with id: '{}'." + e.getMessage(), user.getUsername(), id);
            ErrorResponse bm = new ErrorResponse(HttpStatus.NOT_MODIFIED.getReasonPhrase(), "PRODUCT_STOCK_ERROR");
            return new ResponseEntity<>(bm, HttpStatus.NOT_MODIFIED);
        } catch (Exception e) {
            log.info("User {} requested to change the receive state of the supplier order with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to cancel the reception of a supplier order.
     * We use the function cancelReceiveOrder provided by the SupplierOrderManagerImpl class.
     * Depending on the Exception returned by cancelReceiveOrder, we return an appropriate HttpStatus.
     *
     * @param id Corresponds to the id of the supplier order to send.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing a SupplierOrderDTO objects or an Error Message.
     *      --> HttpStatus.OK if the supplier order has been updated. (SupplierOrderDTO)
     *      --> HttpStatus.BAD_REQUEST if no supplier order corresponds to the given id. (ErrorMessage)
     *      --> HttpStatus.CONFLICT if no supplier order is not already received. (ErrorMessage)
     *      --> HttpStatus.NOT_MODIFIED if a product of the order had a problem. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PutMapping(value = "/orders/{id}/cancel-reception", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<?> cancelReceivedOrder(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to change the receive state (not received) of the supplier order with id: {}.", user.getUsername(), id);
        try {
            SupplierOrder supplierOrder = orderManager.cancelReceiveOrder(id);
            SupplierOrderDTO convert = SupplierOrderDTO.convert(supplierOrder);
            createHATEOAS(convert);
            return new ResponseEntity<>(convert, HttpStatus.OK);
        } catch (UnknownOrderException e) {
            log.info("User {} requested to cancel the reception of the supplier order with id: {}. NO SUPPLIER ORDER FOUND.", user.getUsername(), id);
            ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_SUPPLIER_ORDER_FOUND");
            return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
        } catch (ProductStockException e) {
            log.info("User {} requested: " + e.getMessage(), user.getUsername());
            ErrorResponse bm = new ErrorResponse(HttpStatus.NOT_MODIFIED.getReasonPhrase(), "PRODUCT_STOCK_ERROR");
            return new ResponseEntity<>(bm, HttpStatus.NOT_MODIFIED);
        } catch (OrderStateException e) {
            log.info("User {} requested to cancel a supplier order that is not received, order id: {}", user.getUsername(), id);
            ErrorResponse bm = new ErrorResponse(HttpStatus.CONFLICT.getReasonPhrase(), "SUPPLIER_ORDER_NOT_RECEIVED");
            return new ResponseEntity<>(bm, HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.info("User {} requested to change the receive state of the supplier order with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * Firstly, we check that a supplier order exists with the corresponding id.
     *      If not, we return an HttpStatus.BAD_REQUEST to the user.
     * Secondly, we check that the age of the order is not more than three days old and that the order is not set
     *      If the order has been made more than 3 days ago, we return to the user an HttpStatus.NO_ACCEPTABLE.
     * Secondly, we can delete the object from the database.
     * Finally, we return to the user that the operation worked correctly.
     *
     * @param id Corresponds to the id of the supplier order that the user wants to delete.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing an Error Message.
     *      --> HttpStatus.OK if the supplier order has been deleted.
     *      --> HttpStatus.BAD_REQUEST if no supplier order corresponds to the given id.
     *      --> HttpStatus.PRECONDITION_FAILED if the supplier order is too old.
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs.
     */
    @DeleteMapping(value = "/orders/{id}", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<ErrorResponse> deleteSupOrder(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to delete the supplier order with id: '{}'.", user.getUsername(), id);
            Optional<SupplierOrder> orderOptional = sOrderRepository.findById(id);
            if (!orderOptional.isPresent()) {
                log.info("User {} requested to delete the supplier order with id: '{}'. NO SUPPLIER ORDER FOUND.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_SUPPLIER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            SupplierOrder supplierOrder = orderOptional.get();
            if (supplierOrder.getDate().plusDays(3).isAfter(LocalDate.now()) || supplierOrder.getOrderIsSent()) {
                log.info("User {} requested to delete the supplier order with id: '{}'. ORDER CANNOT BE DELETED.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.PRECONDITION_FAILED.getReasonPhrase(), "SUPPLIER_ORDER_TOO_OLD");
                return new ResponseEntity<>(bm, HttpStatus.PRECONDITION_FAILED);
            }
            log.debug("User {} requested to delete the supplier order with id: '{}'. DELETING SUPPLIER ORDER.", user.getUsername(), id);
            sOrderRepository.deleteById(id);
            log.info("User {} requested to delete the supplier order with id: '{}'. SUPPLIER ORDER DELETED.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to delete the supplier order with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_ADMIN
     * This function is used to delete a supplier order by its id.
     * Firstly, we check that a supplier order exists with the corresponding id.
     *      If not, we return an HttpStatus.BAD_REQUEST to the user.
     * Secondly, we can delete the object from the database.
     * Finally, we return to the user that the operation worked correctly.
     *
     * @param id Corresponds to the id of the supplier order that the user wants to delete.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing an Error Message.
     *      --> HttpStatus.OK if the supplier order has been deleted.
     *      --> HttpStatus.BAD_REQUEST if no supplier order corresponds to the given id.
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs.
     */
    @DeleteMapping(value = "/orders/{id}/force", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<ErrorResponse> deleteOrderForce(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to delete the supplier order with id: '{}'.", user.getUsername(), id);
            if (!sOrderRepository.existsById(id)) {
                log.info("User {} requested to delete the supplier order with id: '{}'. NO CUSTOMER ORDER.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_SUPPLIER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            log.warn("User {} requested to delete the supplier order with id: '{}'. DELETING CUSTOMER ORDER.", user.getUsername(), id);
            sOrderRepository.deleteById(id);
            log.info("User {} requested to delete the supplier order with id: '{}'. CUSTOMER ORDER DELETED.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to delete the supplier order with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
