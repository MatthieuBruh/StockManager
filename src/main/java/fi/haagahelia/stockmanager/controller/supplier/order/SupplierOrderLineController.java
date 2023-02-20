package fi.haagahelia.stockmanager.controller.supplier.order;


import fi.haagahelia.stockmanager.controller.product.ProductController;
import fi.haagahelia.stockmanager.dto.supplier.order.SupplierOrderLineCuDTO;
import fi.haagahelia.stockmanager.dto.supplier.order.SupplierOrderLineDTO;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrderLine;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrderLinePK;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.product.ProductRepository;
import fi.haagahelia.stockmanager.repository.supplier.order.SupplierOrderLineRepository;
import fi.haagahelia.stockmanager.repository.supplier.order.SupplierOrderRepository;
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
import java.util.Objects;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Slf4j
@RestController
@RequestMapping("/api/suppliers/orders")
public class SupplierOrderLineController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final SupplierOrderRepository soRepository;
    private final SupplierOrderLineRepository soLineRepository;
    private final ProductRepository pRepository;

    @Autowired
    public SupplierOrderLineController(SupplierOrderRepository soRepository,
                                       SupplierOrderLineRepository soLineRepository, ProductRepository pRepository) {
        this.soRepository = soRepository;
        this.soLineRepository = soLineRepository;
        this.pRepository = pRepository;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    /**
     * This method is used to create and add the HATEOAS links to a DTO model.
     * @param lineDTO The dto model to which we will add the HATEOAS links.
     * @return The dto model with the HATEOAS links.
     */
    private SupplierOrderLineDTO createHATEOAS(SupplierOrderLineDTO lineDTO) {
        Long orderId = lineDTO.getSupplierOrderDTO().getId();
        Long productId = lineDTO.getProductCompleteDTO().getId();

        Link selfRel = linkTo(SupplierOrderLineController.class)
                .slash("/" + orderId + "/details/product=" + productId).withSelfRel();
        lineDTO.add(selfRel);


        Link orderDetailsLink = linkTo(SupplierOrderLineController.class)
                .slash(orderId).slash("details").withRel("order-details");
        lineDTO.add(orderDetailsLink);

        Link orderLink = linkTo(SupplierOrderController.class).slash("orders").slash(orderId).withRel("order");
        lineDTO.add(orderLink);

        Link productLink = linkTo(ProductController.class).slash(productId).withRel("product");
        lineDTO.add(productLink);
        return lineDTO;
    }

    /**
     * This function is used to convert a List of SupplierOrder into a List of SupplierOrderDTO.
     * It also adds the HATEOAS links on each element of the list.
     * @param orderLines Corresponds to the list of SupplierOrder.
     * @return Corresponds to the list of SupplierOrderDTO.
     */
    private List<SupplierOrderLineDTO> convertOrderLines(List<SupplierOrderLine> orderLines) {
        List<SupplierOrderLineDTO> supplierOrderLineDTOS = new ArrayList<>();
        for (SupplierOrderLine cusOrderLine : orderLines) {
            SupplierOrderLineDTO cusOrderLineDTO = SupplierOrderLineDTO.convert(cusOrderLine);
            createHATEOAS(cusOrderLineDTO);
            supplierOrderLineDTOS.add(cusOrderLineDTO);
        }
        return supplierOrderLineDTOS;
    }


    /* ------------------------------------------------- API METHODS ------------------------------------------------ */

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get all the order lines that correspond to an order id.
     * Firstly, we find all the lines that are in the database using the SupplierOrderLineRepository.
     * Secondly, we check that list returned by the previous step is not empty.
     *      If, it is empty, we return an HttpStatus.NO_CONTENT.
     * Thirdly (else), we can convert each SupplierOrderLine as a SupplierOrderLineDTO, and we can create HATEOAS link.
     * Finally, we can return the data to the user with an HttpStatus.Ok.
     * @param orderId Corresponds to the order id
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @GetMapping(value = "/{orderId}/details", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<List<SupplierOrderLineDTO>> getSupOrderLines(@PathVariable(value = "orderId") Long orderId,
                                                                                     @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting the order lines that corresponds to the order: {}.", user.getUsername(), orderId);
        List<SupplierOrderLine> supplierOrderLines = soLineRepository.findBySupplierOrderId(orderId);
        if (supplierOrderLines.size() < 1) {
            log.info("User {} requested to get all the lines of the order: {}. NO DATA FOUND", user.getUsername(), orderId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<SupplierOrderLineDTO> supplierOrderLineDTOS = convertOrderLines(supplierOrderLines);
        log.info("User {} requested to get all the lines of the order: {}. RETURNING DATA.", user.getUsername(), orderId);
        return new ResponseEntity<>(supplierOrderLineDTOS, HttpStatus.OK);
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to find an order line, using the orderId and the productId.
     * Firstly, we find the line by using the SupplierOrderLineRepository.
     * At this point, we need the orderId and the productId.
     * Secondly, we check that the optional returned by the previous step is not empty.
     *      If it is empty, we return an HttpStatus.NO_CONTENT to the user.
     * Thirdly, we convert the SupplierOrderLine as an SupplierOrderLineDTO.
     * Finally, we return the data to the user with an HttpStatus.Ok.
     * @param orderId Corresponds to the id of the order
     * @param productId Corresponds to the id of the product
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @GetMapping(value = "/{orderId}/details/product={productId}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<SupplierOrderLineDTO> getSupOrderLine(@PathVariable(value = "orderId") Long orderId,
                                                                              @PathVariable(value = "productId") Long productId,
                                                                              @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting the the line: supOrderId: {}, productId: {}.",
                user.getUsername(), orderId, productId);
        Optional<SupplierOrderLine> orderLineOptional = soLineRepository.findBySupplierOrderIdAndProductId(orderId, productId);
        if (!orderLineOptional.isPresent()) {
            log.info("User {} requested the the line: supOrderId: {}, productId: {}. NO DATA FOUND.",
                    user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        SupplierOrderLineDTO supplierOrderLineDTO = SupplierOrderLineDTO.convert(orderLineOptional.get());
        log.info("User {} requested the the line: supOrderId: {}, productId: {}. RETURNING DATA.",
                user.getUsername(), orderId, productId);
        return new ResponseEntity<>(createHATEOAS(supplierOrderLineDTO), HttpStatus.OK);
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to create and save a new supplier order line.
     * Firstly, we check different constraints about the order: order exists,
     * and order is not sent or the delivery date is not passed.
     * Secondly, we check that the product exists.
     * Thirdly, we check other general constraints: product's supplier is the same as the order's supplier,
     * order line does not exist, the quantity set by the user is valid, and the price is bigger than 0
     *      (If not we use the sell price in the product table).
     * Fourthly, we can create the SupplierOrderLine object and set all the values.
     * Fifthly, we can save the object in the database.
     * Finally, we convert the SupplierOrderLine object as a SupplierOrderLineDTO, we add the HATEOAS links, and we can
     * return the data to the user with an HttpStatus.CREATED.
     * @param orderId Corresponds to the id of the order
     * @param productId Corresponds to the id of the product
     * @param orderCuDTO Corresponds to the data of the new order line.
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @PostMapping(value = "/{orderId}/details/product={productId}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<SupplierOrderLineDTO> createOrderLine(@PathVariable(value = "orderId") Long orderId,
                                                                              @PathVariable(value = "productId") Long productId,
                                                                              @RequestBody SupplierOrderLineCuDTO orderCuDTO,
                                                                              @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to create a new supplier order line: orderId: {}, productId: {}.",
                user.getUsername(), orderId, productId);
        // --------------- Order verifications ---------------
        Optional<SupplierOrder> orderOptional = soRepository.findById(orderId);
        if (!orderOptional.isPresent()) {
            log.info("User {} requested to create a new supplier order line: orderId: {} ; productId: {}." +
                    "ORDER NOT FOUND.", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        SupplierOrder supplierOrder = orderOptional.get();
        if (supplierOrder.getOrderIsSent() || supplierOrder.getDeliveryDate().isBefore(LocalDate.now())) {
            log.info("User {} requested to create a new supplier order line: orderId: {} ; productId: {}. " +
                    "ORDER ALREADY SENT OR DELIVERY DATE IS PASSED.", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        // --------------- Product verifications ---------------
        Optional<Product> productOptional = pRepository.findById(productId);
        if (!productOptional.isPresent()) {
            log.info("User {} requested to create a new supplier order line: orderId: {} ; productId: {}." +
                    "PRODUCT NOT FOUND.", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Product product = productOptional.get();
        // --------------- Others verifications ---------------
        if (!Objects.equals(product.getSupplier().getId(), supplierOrder.getSupplier().getId())) {
            log.info("User {} requested to create a new supplier order line: orderId: {} ; productId: {}." +
                    "PRODUCT IS NOT SUPPLIED BY THIS SUPPLIER.", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (soLineRepository.existsBySupplierOrderIdAndProductId(orderId, productId)) {
            log.info("User {} requested to create a new supplier order line: orderId: {} ; productId: {}." +
                    "ALREADY EXISTS.", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        if (orderCuDTO.getQuantity() == null || orderCuDTO.getQuantity() < 1) {
            log.info("User {} requested to create a new supplier order line: orderId: {} ; productId: {}." +
                    "INVALID QUANTITY", user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (orderCuDTO.getBuyPrice() == null || orderCuDTO.getBuyPrice() <= 0) {
            orderCuDTO.setBuyPrice(product.getPurchasePrice());
        }
        // --------------- CREATING ORDER LINE OBJECT ---------------
        SupplierOrderLine supplierOrderLine = new SupplierOrderLine();
        supplierOrderLine.setSupplierOrder(supplierOrder);
        supplierOrderLine.setProduct(product);
        supplierOrderLine.setSupplierOrderLinePK(new SupplierOrderLinePK(supplierOrder.getId(), product.getId()));
        supplierOrderLine.setQuantity(orderCuDTO.getQuantity());
        supplierOrderLine.setBuyPrice(orderCuDTO.getBuyPrice());
        // --------------- SAVING DATA ---------------
        log.debug("User {} requested to create a new supplier order line: orderId: {} ; productId: {}." +
                "SAVING SUPPLIER ORDER LINE.", user.getUsername(), orderId, productId);
        SupplierOrderLine savedLine = soLineRepository.save(supplierOrderLine);
        // --------------- RETURNING DATA ---------------
        SupplierOrderLineDTO savedLineDTO = SupplierOrderLineDTO.convert(savedLine);
        createHATEOAS(savedLineDTO);
        log.info("User {} requested to create a new supplier order line: orderId: {} ; productId: {}." +
                "SUPPLIER ORDER LINE SAVED.", user.getUsername(), orderId, productId);
        return new ResponseEntity<>(savedLineDTO, HttpStatus.CREATED);
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is created to delete a SupplierOrderLine.
     * Firstly, we find the supplier order line by using the SupplierOrderLineRepository.
     *      If the optional returned by the previous step, we return an HttpStatus.NO_CONTENT to the user.
     * Secondly, we check that the order is not already sent to the supplier.
     *      If the order is already sent, we cannot modify the related items to the order.
     *          We return to the user an HttpStatus.NOT_ACCEPTABLE.
     * Finally, we can delete the SupplierOrderLine and return to the user an HttpStatus.ACCEPTED.
     * @param orderId Corresponds to the id of the order
     * @param productId Corresponds to the id of the product
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity object that contains an HttpStatus code and the corresponding data.
     */
    @DeleteMapping(value = "/{orderId}/details/product={productId}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<SupplierOrderLineDTO> deleteOrderLine(@PathVariable(value = "orderId") Long orderId,
                                                                           @PathVariable(value = "productId") Long productId,
                                                                           @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to delete the order line: supOrderId: {}, productId: {}.",
                user.getUsername(), orderId, productId);
        Optional<SupplierOrderLine> orderLineOptional = soLineRepository.findBySupplierOrderIdAndProductId(orderId, productId);
        if (!orderLineOptional.isPresent()) {
            log.info("User {} requested to delete the order line: supOrderId: {}, productId: {}. NO DATA FOUND.",
                    user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        SupplierOrderLine orderLine = orderLineOptional.get();
        if (orderLine.getSupplierOrder().getOrderIsSent()) {
            log.info("User {} requested to delete the order line: supOrderId: {}, productId: {}. ORDER IS ALREADY SENT.",
                    user.getUsername(), orderId, productId);
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        log.warn("User {} requested to delete the order line: supOrderId: {}, productId: {}. DELETING DATA.",
                user.getUsername(), orderId, productId);
        soLineRepository.deleteBySupplierOrderIdAndProductId(orderId, productId);
        log.info("User {} requested to delete the order line: supOrderId: {}, productId: {}. ORDER LINE DELETED.",
                user.getUsername(), orderId, productId);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

}
