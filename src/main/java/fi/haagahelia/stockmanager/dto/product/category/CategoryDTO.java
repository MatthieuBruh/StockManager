package fi.haagahelia.stockmanager.dto.product.category;


import fi.haagahelia.stockmanager.model.product.category.Category;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class CategoryDTO extends RepresentationModel<CategoryDTO> {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Long id;
    private String name;
    private String description;

    /* ------------------------------------------------- CONVERTORS ------------------------------------------------- */

    public static CategoryDTO convert(Category category) {
        return builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
