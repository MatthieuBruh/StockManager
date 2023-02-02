package fi.haagahelia.stockmanager.dto.customer.order;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CustomerOrderLineCuDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Integer quantity;
    private Double sellPrice;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    private Long customerOrderId;

    private Long productId;

}
