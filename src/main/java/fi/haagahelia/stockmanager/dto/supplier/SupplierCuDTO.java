package fi.haagahelia.stockmanager.dto.supplier;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SupplierCuDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private String name;
    private String email;
    private String phoneNumber;
    private Long geolocationId;
}
