package fi.haagahelia.stockmanager.dto.product.brand;

import fi.haagahelia.stockmanager.model.product.brand.Brand;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;


@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class BrandDTO extends RepresentationModel<BrandDTO> {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */
    private Long id;
    private String name;

    /* ------------------------------------------------- CONVERTORS ------------------------------------------------- */

    public static BrandDTO convert(Brand brand) {
        return builder()
                .id(brand.getId())
                .name(brand.getName())
                .build();
    }
}
