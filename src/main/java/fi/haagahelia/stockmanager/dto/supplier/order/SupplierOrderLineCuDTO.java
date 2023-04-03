package fi.haagahelia.stockmanager.dto.supplier.order;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SupplierOrderLineCuDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Integer quantity;
    private Double buyPrice;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */
}
