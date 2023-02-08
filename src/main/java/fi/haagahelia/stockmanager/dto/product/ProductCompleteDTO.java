package fi.haagahelia.stockmanager.dto.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.haagahelia.stockmanager.dto.product.brand.BrandDTO;
import fi.haagahelia.stockmanager.dto.product.category.CategoryDTO;
import fi.haagahelia.stockmanager.dto.product.category.ProductDTO;
import fi.haagahelia.stockmanager.dto.supplier.SupplierDTO;
import fi.haagahelia.stockmanager.model.product.Product;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class ProductCompleteDTO extends ProductDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Long id;
    private String name;
    private String description;
    private Double purchasePrice;
    private Double salePrice;
    private Integer stock;
    private Integer minStock;
    private Integer batchSize;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    @JsonIgnore
    private BrandDTO brand;
    @JsonIgnore
    private CategoryDTO category;
    @JsonIgnore
    private SupplierDTO supplier;

    /* ------------------------------------------------- CONVERTORS ------------------------------------------------- */

    public static ProductCompleteDTO convert(Product product) {
        ProductCompleteDTOBuilder productCompleteDTOBuilder = builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .purchasePrice(product.getPurchasePrice())
                .salePrice(product.getSalePrice())
                .stock(product.getStock())
                .minStock(product.getMinStock())
                .batchSize(product.getBatchSize());

        if (product.getBrand() != null) {
            productCompleteDTOBuilder.brand(BrandDTO.convert(product.getBrand()));
        }

        if (product.getCategory() != null) {
            productCompleteDTOBuilder.category(CategoryDTO.convert(product.getCategory()));
        }

        if (product.getSupplier() != null) {
            productCompleteDTOBuilder.supplier(SupplierDTO.convert(product.getSupplier()));
        }

        return productCompleteDTOBuilder.build();
    }
}
