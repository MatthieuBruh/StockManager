package fi.haagahelia.stockmanager.dto.customer;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerCuDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private String firstName;

    private String lastName;

    private String email;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    private Long geolocationId;

}
