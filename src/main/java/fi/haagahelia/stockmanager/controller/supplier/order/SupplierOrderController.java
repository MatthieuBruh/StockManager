package fi.haagahelia.stockmanager.controller.supplier.order;


import fi.haagahelia.stockmanager.repository.supplier.SupplierRepository;
import fi.haagahelia.stockmanager.repository.supplier.order.SupplierOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/suppliers/")
public class SupplierOrderController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final SupplierOrderRepository sOrderRepository;
    private final SupplierRepository sRepository;

    @Autowired
    public SupplierOrderController(SupplierOrderRepository sOrderRepository, SupplierRepository sRepository) {
        this.sOrderRepository = sOrderRepository;
        this.sRepository = sRepository;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */


    /* ------------------------------------------------- API METHODS ------------------------------------------------ */
}
