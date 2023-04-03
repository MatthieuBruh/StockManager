package fi.haagahelia.stockmanager.dto.supplier;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
