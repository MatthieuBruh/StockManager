package fi.haagahelia.stockmanager.dto.common;

import fi.haagahelia.stockmanager.model.common.Geolocation;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;


@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class GeolocationDTO extends RepresentationModel<GeolocationDTO> {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Long id;
    private String streetName;
    private String streetNumber;
    private String postcode;
    private String locality;
    private String country;

    /* ------------------------------------------------- CONVERTORS ------------------------------------------------- */

    public static GeolocationDTO convert(Geolocation geolocation) {
        return builder()
                .id(geolocation.getId())
                .streetName(geolocation.getStreetName())
                .streetNumber(geolocation.getStreetNumber())
                .postcode(geolocation.getPostcode())
                .locality(geolocation.getLocality())
                .country(geolocation.getCountry())
                .build();
    }
}
