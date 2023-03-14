package fi.haagahelia.stockmanager.dto.customer.order;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

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
