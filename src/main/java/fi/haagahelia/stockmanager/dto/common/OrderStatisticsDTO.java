package fi.haagahelia.stockmanager.dto.common;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDate;


@Data
@EqualsAndHashCode(callSuper = false)
public class OrderStatisticsDTO extends RepresentationModel<OrderStatisticsDTO> {
    private LocalDate date;
    private Integer totalOrders;
    private Integer totalOrdersForTheMonth;
    private Double orderValuesForTheMonth;

    public void incrTotalOrders() {
        if (this.totalOrders == null) this.totalOrders = 0;
        this.totalOrders += 1;
    }

    public void incrTotalOrderForMonth() {
        if (this.totalOrdersForTheMonth == null) this.totalOrdersForTheMonth = 0;
        this.totalOrdersForTheMonth += 1;
    }

    public void addOrderValueForTheMonth(Double value) {
        if (this.orderValuesForTheMonth == null) this.orderValuesForTheMonth = 0.0;
        this.orderValuesForTheMonth += value;
    }
}

