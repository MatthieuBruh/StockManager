package fi.haagahelia.stockmanager.dto.customer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.haagahelia.stockmanager.dto.common.GeolocationDTO;
import fi.haagahelia.stockmanager.model.customer.Customer;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class CustomerDTO extends RepresentationModel<CustomerDTO> {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Long id;

    private String firstName;

    private String lastName;

    private String email;


    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    @JsonIgnore
    private GeolocationDTO geolocationDTO;

    /* ------------------------------------------------- CONVERTORS ------------------------------------------------- */

    public static CustomerDTO convert(Customer customer) {
        CustomerDTOBuilder customerDTOBuilder = builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail());

        if (customer.getGeolocation() != null)
            customerDTOBuilder.geolocationDTO(GeolocationDTO.convert(customer.getGeolocation()));

        return customerDTOBuilder.build();
    }
}
