package fi.haagahelia.stockmanager.dto.product;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductCuDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Long id;
    private String name;
    private String description;
    private Double purchasePrice;
    private Double salePrice;
    private Integer stock;
    private Integer minStock;
    private Integer batchSize;
    private Long brandId;
    private Long categoryId;
    private Long supplierId;

}
