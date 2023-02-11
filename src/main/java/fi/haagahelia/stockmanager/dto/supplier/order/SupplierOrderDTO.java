package fi.haagahelia.stockmanager.dto.supplier.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.haagahelia.stockmanager.dto.supplier.SupplierDTO;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDate;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class SupplierOrderDTO extends RepresentationModel<SupplierOrderDTO> {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Long id;
    private LocalDate date;
    private LocalDate deliveryDate;
    private Boolean orderIsSent;
    private Boolean isReceived;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    @JsonIgnore
    private SupplierDTO supplierDTO;

    /* ------------------------------------------------- CONVERTORS ------------------------------------------------- */

    public static SupplierOrderDTO convert(SupplierOrder supplierOrder) {
        SupplierOrderDTOBuilder supplierOrderDTOBuilder = builder()
                .id(supplierOrder.getId())
                .date(supplierOrder.getDate())
                .deliveryDate(supplierOrder.getDeliveryDate())
                .orderIsSent(supplierOrder.getOrderIsSent())
                .isReceived(supplierOrder.getReceived());

        if (supplierOrder.getSupplier() != null) {
            supplierOrderDTOBuilder.supplierDTO(SupplierDTO.convert(supplierOrder.getSupplier()));
        }

        return supplierOrderDTOBuilder.build();
    }
}
