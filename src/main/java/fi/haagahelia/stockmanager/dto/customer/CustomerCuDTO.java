package fi.haagahelia.stockmanager.dto.customer;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CustomerCuDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private String firstName;

    private String lastName;

    private String email;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    private Long geolocationId;

}
