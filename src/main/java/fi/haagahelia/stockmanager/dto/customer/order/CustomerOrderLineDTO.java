package fi.haagahelia.stockmanager.dto.customer.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.haagahelia.stockmanager.dto.product.ProductSimpleDTO;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrderLine;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class CustomerOrderLineDTO extends RepresentationModel<CustomerOrderLineDTO> {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Integer quantity;
    private Double sellPrice;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    @JsonIgnore
    private CustomerOrderDTO customerOrderDTO;

    @JsonIgnore
    private ProductSimpleDTO productSimpleDTO;

    /* ------------------------------------------------- CONVERTORS ------------------------------------------------- */

    public static CustomerOrderLineDTO convert(CustomerOrderLine orderLine) {
        CustomerOrderLineDTOBuilder orderLineDTOBuilder = builder()
                .quantity(orderLine.getQuantity())
                .sellPrice(orderLine.getSellPrice());

        if (orderLine.getCustomerOrder() != null)
            orderLineDTOBuilder.customerOrderDTO(CustomerOrderDTO.convert(orderLine.getCustomerOrder()));

        if (orderLine.getProduct() != null)
            orderLineDTOBuilder.productSimpleDTO(ProductSimpleDTO.convert(orderLine.getProduct()));

        return orderLineDTOBuilder.build();
    }
}
