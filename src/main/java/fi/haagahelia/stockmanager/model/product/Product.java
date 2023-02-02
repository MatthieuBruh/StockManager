package fi.haagahelia.stockmanager.model.product;

import fi.haagahelia.stockmanager.model.product.brand.Brand;
import fi.haagahelia.stockmanager.model.product.category.Category;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

import java.util.Objects;

@Entity
@Table(name = "BRU_PRODUCT", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"pro_name","pro_sup_id"})
})
public class Product {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pro_id")
    private Long id;

    @Column(name = "pro_name", nullable = false)
    private String name;

    @Column(name = "pro_description", nullable = false)
    private String description;

    @Column(name = "pro_purchase_price", nullable = false)
    private Double purchasePrice;

    @Column(name = "pro_sale_price", nullable = false)
    private Double salePrice;

    @Column(name = "pro_stock") // If a product has a stock has null, we don't want to order it again
    @Min(0)
    private Integer stock;

    @Column(name = "pro_min_stock", nullable = false)
    @Min(0)
    private Integer minStock;

    @Column(name = "pro_batch_size", nullable = false)
    @Min(2)
    private Integer batchSize;


    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pro_bra_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pro_cat_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pro_sup_id")
    private Supplier supplier;


    /* ------------------------------------------------ CONSTRUCTORS ------------------------------------------------ */

    public Product() { }

    public Product(String name, String description, Double purchasePrice, Double salePrice, Integer stock,
                   Integer minStock, Integer batchSize, Brand brand, Category category, Supplier supplier) {
        this.name = name;
        this.description = description;
        this.purchasePrice = purchasePrice;
        this.salePrice = salePrice;
        this.stock = stock;
        this.minStock = minStock;
        this.batchSize = batchSize;
        this.brand = brand;
        this.category = category;
        this.supplier = supplier;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return id.equals(product.id) && name.equals(product.name) && Objects.equals(description, product.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description);
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", purchasePrice=" + purchasePrice +
                ", salePrice=" + salePrice +
                ", stock=" + stock +
                ", minStock=" + minStock +
                ", batchSize=" + batchSize +
                '}';
    }

    /* ---------------------------------------------- GETTERS & SETTERS --------------------------------------------- */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(Double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public Double getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(Double salePrice) {
        this.salePrice = salePrice;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getMinStock() {
        return minStock;
    }

    public void setMinStock(Integer minStock) {
        this.minStock = minStock;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }
}
