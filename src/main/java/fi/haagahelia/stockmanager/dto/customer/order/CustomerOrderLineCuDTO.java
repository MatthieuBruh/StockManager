package fi.haagahelia.stockmanager.dto.customer.order;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerOrderLineCuDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Integer quantity;
    private Double sellPrice;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

}
