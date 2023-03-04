package fi.haagahelia.stockmanager.model.supplier.order;

import fi.haagahelia.stockmanager.model.product.Product;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "BRU_SUP_ORDER_LINE")
public class SupplierOrderLine {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    @EmbeddedId
    private SupplierOrderLinePK supplierOrderLinePK;

    @Column(name = "lin_quantity")
    private Integer quantity;

    @Column(name = "lin_buy_price")
    private Double buyPrice;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("pk_suo_id")
    @JoinColumn(name = "pk_suo_id")
    private SupplierOrder supplierOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("pk_pro_id")
    @JoinColumn(name = "pk_pro_id")
    private Product product;

    /* ------------------------------------------------ CONSTRUCTORS ------------------------------------------------ */

    public SupplierOrderLine() { }

    public SupplierOrderLine(Integer quantity, Double buyPrice, SupplierOrder supplierOrder, Product product) {
        this.quantity = quantity;
        this.buyPrice = buyPrice;
        this.supplierOrder = supplierOrder;
        this.product = product;
        this.supplierOrderLinePK = new SupplierOrderLinePK(supplierOrder.getId(), product.getId());
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SupplierOrderLine that = (SupplierOrderLine) o;
        return quantity.equals(that.quantity) && buyPrice.equals(that.buyPrice)
                && supplierOrder.equals(that.supplierOrder) && product.equals(that.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantity, buyPrice, supplierOrder, product);
    }

    @Override
    public String toString() {
        return "SupplierOrderLine{" +
                "quantity=" + quantity +
                ", buyPrice=" + buyPrice +
                ", supplierOrder=" + supplierOrder +
                ", product=" + product +
                '}';
    }

    /* ---------------------------------------------- GETTERS & SETTERS --------------------------------------------- */

    public SupplierOrderLinePK getSupplierOrderLinePK() {
        return supplierOrderLinePK;
    }

    public void setSupplierOrderLinePK(SupplierOrderLinePK supplierOrderLinePK) {
        this.supplierOrderLinePK = supplierOrderLinePK;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(Double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public SupplierOrder getSupplierOrder() {
        return supplierOrder;
    }

    public void setSupplierOrder(SupplierOrder supplierOrder) {
        this.supplierOrder = supplierOrder;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
