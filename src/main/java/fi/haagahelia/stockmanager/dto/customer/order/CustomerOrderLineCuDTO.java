package fi.haagahelia.stockmanager.dto.customer.order;

import lombok.*;

@Data
@AllArgsConstructor
public class CustomerOrderLineCuDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Integer quantity;
    private Double sellPrice;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

}
