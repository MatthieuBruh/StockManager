package fi.haagahelia.stockmanager.dto.product;


import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.haagahelia.stockmanager.dto.product.brand.BrandDTO;
import fi.haagahelia.stockmanager.dto.product.category.CategoryDTO;
import fi.haagahelia.stockmanager.model.product.Product;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class ProductSimpleDTO extends RepresentationModel<ProductSimpleDTO> {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Long id;
    private String name;
    private String description;
    private Double salePrice;
    private Integer stock;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    @JsonIgnore
    private BrandDTO brand;
    @JsonIgnore
    private CategoryDTO category;

    /* ------------------------------------------------- CONVERTORS ------------------------------------------------- */

    public static ProductSimpleDTO convert(Product product) {
        ProductSimpleDTOBuilder productBuilder = builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .salePrice(product.getSalePrice())
                .stock(product.getStock());


        if (product.getBrand() != null) {
            productBuilder.brand(BrandDTO.convert(product.getBrand()));
        }

        if (product.getCategory() != null) {
            productBuilder.category(CategoryDTO.convert(product.getCategory()));
        }

        return productBuilder.build();
    }
}
