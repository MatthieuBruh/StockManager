package fi.haagahelia.stockmanager.model.customer.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CustomerOrderLinePK implements Serializable {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */
    @Column(name = "pk_cuo_id")
    private Long customerOrderId;

    @Column(name = "pk_pro_id")
    private Long productId;

    /* ------------------------------------------------ CONSTRUCTORS ------------------------------------------------ */

    public CustomerOrderLinePK() { }

    public CustomerOrderLinePK(Long customerOrderId, Long productId) {
        this.customerOrderId = customerOrderId;
        this.productId = productId;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerOrderLinePK that = (CustomerOrderLinePK) o;
        return customerOrderId.equals(that.customerOrderId) && productId.equals(that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerOrderId, productId);
    }

    @Override
    public String toString() {
        return "CustomerOrderLinePK{" +
                "customerOrderId=" + customerOrderId +
                ", productId=" + productId +
                '}';
    }

    /* ---------------------------------------------- GETTERS & SETTERS --------------------------------------------- */

    public Long getCustomerOrderId() {
        return customerOrderId;
    }

    public void setCustomerOrderId(Long customerOrderId) {
        this.customerOrderId = customerOrderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
