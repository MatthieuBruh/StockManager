package fi.haagahelia.stockmanager.dto.customer.order;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerOrderCuDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private LocalDate date;
    private LocalDate deliveryDate;
    private Boolean isSent;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    private Long productId;
    private Long customerId;

}
