package fi.haagahelia.stockmanager.dto.supplier.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.haagahelia.stockmanager.dto.product.ProductCompleteDTO;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrderLine;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Builder
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierOrderLineDTO extends RepresentationModel<SupplierOrderLineDTO> {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Integer quantity;
    private Double buyPrice;
    private Long supplierOrderId;
    private Long productId;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    @JsonIgnore
    private SupplierOrderDTO supplierOrderDTO;

    @JsonIgnore
    private ProductCompleteDTO productCompleteDTO;


    /* ------------------------------------------------- CONVERTORS ------------------------------------------------- */

    public static SupplierOrderLineDTO convert(SupplierOrderLine supplierOrderLine) {
        SupplierOrderLineDTOBuilder supplierOrderLineDTOBuilder = builder()
                .quantity(supplierOrderLine.getQuantity())
                .buyPrice(supplierOrderLine.getBuyPrice());

        if (supplierOrderLine.getSupplierOrder() != null) {
            supplierOrderLineDTOBuilder.supplierOrderDTO(SupplierOrderDTO.convert(supplierOrderLine.getSupplierOrder()));
        }
        if (supplierOrderLine.getProduct() != null) {
            supplierOrderLineDTOBuilder.productCompleteDTO(ProductCompleteDTO.convert(supplierOrderLine.getProduct()));
        }
        supplierOrderLineDTOBuilder.supplierOrderId(supplierOrderLine.getSupplierOrder().getId());
        supplierOrderLineDTOBuilder.productId(supplierOrderLine.getProduct().getId());
        return supplierOrderLineDTOBuilder.build();
    }

}
