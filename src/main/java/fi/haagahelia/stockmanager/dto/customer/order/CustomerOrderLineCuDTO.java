package fi.haagahelia.stockmanager.dto.customer.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerOrderLineCuDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Integer quantity;
    private Double sellPrice;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

}
