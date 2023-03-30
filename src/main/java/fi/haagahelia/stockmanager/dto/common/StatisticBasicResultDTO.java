package fi.haagahelia.stockmanager.dto.common;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Data
@EqualsAndHashCode(callSuper = true)
public class StatisticBasicResultDTO<T> extends RepresentationModel<StatisticBasicResultDTO<T>> {
    private String resultName;
    private T resultValue;
}
