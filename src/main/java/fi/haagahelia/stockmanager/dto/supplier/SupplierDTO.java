package fi.haagahelia.stockmanager.dto.supplier;


import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.haagahelia.stockmanager.dto.common.GeolocationDTO;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class SupplierDTO extends RepresentationModel<SupplierDTO> {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    @JsonIgnore
    private GeolocationDTO geolocation;

    /* ------------------------------------------------- CONVERTORS ------------------------------------------------- */

    public static SupplierDTO convert(Supplier supplier) {
        SupplierDTOBuilder supplierDTOBuilder = builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .email(supplier.getEmail())
                .phoneNumber(supplier.getPhoneNumber());

        if (supplier.getGeolocation() != null) {
            supplierDTOBuilder.geolocation(GeolocationDTO.convert(supplier.getGeolocation()));
        }
        return supplierDTOBuilder.build();

    }
}
