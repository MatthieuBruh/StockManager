package fi.haagahelia.stockmanager.controller.product;


import fi.haagahelia.stockmanager.controller.product.brand.BrandController;
import fi.haagahelia.stockmanager.controller.product.category.CategoryController;
import fi.haagahelia.stockmanager.dto.product.ProductSimpleDTO;
import fi.haagahelia.stockmanager.repository.product.BrandRepository;
import fi.haagahelia.stockmanager.repository.product.CategoryRepository;
import fi.haagahelia.stockmanager.repository.product.ProductRepository;
import fi.haagahelia.stockmanager.repository.supplier.SupplierRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RestController
@RequestMapping("/api/products")
public class ProductController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final ProductRepository pRepository;
    private final BrandRepository bRepository;
    private final CategoryRepository cRepository;
    private final SupplierRepository sRepository;

    @Autowired
    public ProductController(ProductRepository pRepository, BrandRepository bRepository, CategoryRepository cRepository, SupplierRepository sRepository) {
        this.pRepository = pRepository;
        this.bRepository = bRepository;
        this.cRepository = cRepository;
        this.sRepository = sRepository;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    /**
     * This method is used to create and add the HATEOAS links to a DTO models.
     * @param productSimpleDTO The dto model to which we will add the HATEOAS links.
     * @return The dto model with the HATEOAS links.
     */
    private ProductSimpleDTO createHATEOAS(ProductSimpleDTO productSimpleDTO) {
        Link selfLink = linkTo(ProductController.class).slash(String.valueOf(productSimpleDTO.getId())).withSelfRel();
        productSimpleDTO.add(selfLink);
        Link collectionLink = linkTo(methodOn(ProductController.class)).slash("").withRel("products");
        productSimpleDTO.add(collectionLink);

        // Related object link
        if (productSimpleDTO.getBrand() != null) {
            Link brandLink = linkTo(BrandController.class)
                    .slash(String.valueOf(productSimpleDTO.getBrand().getId())).withRel("brand");
            productSimpleDTO.add(brandLink);
        }

        if (productSimpleDTO.getCategory() != null) {
            Link categoryLink = linkTo(CategoryController.class)
                    .slash(String.valueOf(productSimpleDTO.getCategory().getId())).withRel("category");
            productSimpleDTO.add(categoryLink);
        }
        return productSimpleDTO;
    }


    /* ------------------------------------------------- API METHODS ------------------------------------------------ */
}
