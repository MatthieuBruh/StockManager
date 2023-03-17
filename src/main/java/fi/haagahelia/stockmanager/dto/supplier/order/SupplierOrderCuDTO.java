package fi.haagahelia.stockmanager.dto.supplier.order;


import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierOrderCuDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private LocalDate date;
    private LocalDate deliveryDate;
    private Boolean orderIsSent;
    private Boolean isReceived;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    private Long supplierId;
}
