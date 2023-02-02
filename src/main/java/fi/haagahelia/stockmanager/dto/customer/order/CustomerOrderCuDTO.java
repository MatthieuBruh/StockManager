package fi.haagahelia.stockmanager.dto.customer.order;


import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CustomerOrderCuDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private LocalDate date;
    private LocalDate deliveryDate;
    private Boolean isSent;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    private Long employeeId;

    private Long productId;

}
