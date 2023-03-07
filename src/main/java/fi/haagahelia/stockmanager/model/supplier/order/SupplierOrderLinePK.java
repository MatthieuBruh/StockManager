package fi.haagahelia.stockmanager.model.supplier.order;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SupplierOrderLinePK implements Serializable {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    @Column(name = "pk_suo_id")
    private Long supplierOrderId;

    @Column(name = "pk_pro_id")
    private Long productId;

    /* ------------------------------------------------ CONSTRUCTORS ------------------------------------------------ */

    public SupplierOrderLinePK(Long supplierOrderId, Long productId) {
        this.supplierOrderId = supplierOrderId;
        this.productId = productId;
    }

    public SupplierOrderLinePK() {

    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SupplierOrderLinePK that = (SupplierOrderLinePK) o;
        return supplierOrderId.equals(that.supplierOrderId) && productId.equals(that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supplierOrderId, productId);
    }

    @Override
    public String toString() {
        return "SupplierOrderLinePK{" +
                "supplierOrderId=" + supplierOrderId +
                ", productId=" + productId +
                '}';
    }

    /* ---------------------------------------------- GETTERS & SETTERS --------------------------------------------- */

    public Long getSupplierOrderId() {
        return supplierOrderId;
    }

    public void setSupplierOrderId(Long supplierOrderId) {
        this.supplierOrderId = supplierOrderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
