package fi.haagahelia.stockmanager.controller.supplier.order;


import fi.haagahelia.stockmanager.repository.product.ProductRepository;
import fi.haagahelia.stockmanager.repository.supplier.order.SupplierOrderLineRepository;
import fi.haagahelia.stockmanager.repository.supplier.order.SupplierOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/suppliers/orders/{orderId}/details")
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


    /* ------------------------------------------------- API METHODS ------------------------------------------------ */
}
