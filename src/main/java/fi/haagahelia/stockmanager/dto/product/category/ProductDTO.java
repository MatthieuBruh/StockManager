package fi.haagahelia.stockmanager.dto.product.category;

import fi.haagahelia.stockmanager.dto.product.brand.BrandDTO;
import org.springframework.hateoas.RepresentationModel;

public abstract class ProductDTO extends RepresentationModel<ProductDTO> {
    public abstract Long getId();
    public abstract String getName();
    public abstract Double getSalePrice();
    public abstract Integer getStock();
    public abstract BrandDTO getBrand();
    public abstract CategoryDTO getCategory();

}
