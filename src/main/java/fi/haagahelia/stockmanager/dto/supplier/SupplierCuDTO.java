package fi.haagahelia.stockmanager.dto.supplier;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierCuDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private String name;
    private String email;
    private String phoneNumber;
    private Long geolocationId;
}
