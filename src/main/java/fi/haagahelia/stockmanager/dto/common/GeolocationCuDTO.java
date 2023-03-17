package fi.haagahelia.stockmanager.dto.common;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeolocationCuDTO {
    private String streetName;
    private String streetNumber;
    private String postcode;
    private String locality;
    private String country;
}
