package fi.haagahelia.stockmanager.dto.customer.order;


import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.haagahelia.stockmanager.dto.customer.CustomerDTO;
import fi.haagahelia.stockmanager.dto.user.EmployeeDTO;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDate;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class CustomerOrderDTO extends RepresentationModel<CustomerOrderDTO> {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Long id;
    private LocalDate date;
    private LocalDate deliveryDate;
    private Boolean isSent;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    @JsonIgnore
    private EmployeeDTO employeeDTO;

    @JsonIgnore
    private CustomerDTO customerDTO;

    /* ------------------------------------------------- CONVERTORS ------------------------------------------------- */

    public static CustomerOrderDTO convert(CustomerOrder customerOrder) {
        CustomerOrderDTOBuilder orderDTOBuilder = builder()
                .id(customerOrder.getId())
                .date(customerOrder.getDate())
                .deliveryDate(customerOrder.getDeliveryDate())
                .isSent(customerOrder.getSent());

        if (customerOrder.getEmployee() != null)
            orderDTOBuilder.employeeDTO(EmployeeDTO.convert(customerOrder.getEmployee()));
        if (customerOrder.getCustomer() != null)
            orderDTOBuilder.customerDTO(CustomerDTO.convert(customerOrder.getCustomer()));

        return orderDTOBuilder.build();
    }
}
