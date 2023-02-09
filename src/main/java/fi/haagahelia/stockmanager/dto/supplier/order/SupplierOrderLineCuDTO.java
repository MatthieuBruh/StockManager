package fi.haagahelia.stockmanager.dto.supplier.order;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SupplierOrderLineCuDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Integer quantity;
    private Double buyPrice;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */
}
