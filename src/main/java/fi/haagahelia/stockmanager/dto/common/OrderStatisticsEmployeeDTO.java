package fi.haagahelia.stockmanager.dto.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderStatisticsEmployeeDTO extends OrderStatisticsDTO{
    private String employeeName;
}
