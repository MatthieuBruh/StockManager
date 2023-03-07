package fi.haagahelia.stockmanager.model.customer.order;

import fi.haagahelia.stockmanager.model.product.Product;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "BRU_CUS_ORDER_LINE")
public class CustomerOrderLine {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    @EmbeddedId
    private CustomerOrderLinePK customerOrderLinePK;

    @Column(name = "lin_quantity")
    private Integer quantity;

    @Column(name = "lin_sale_price")
    private Double sellPrice;


    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("pk_cuo_id")
    @JoinColumn(name = "pk_cuo_id")
    private CustomerOrder customerOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("pk_pro_id")
    @JoinColumn(name = "pk_pro_id")
    private Product product;

    /* ------------------------------------------------ CONSTRUCTORS ------------------------------------------------ */

    public CustomerOrderLine() { }

    public CustomerOrderLine(Integer quantity, Double sellPrice, CustomerOrder customerOrder, Product product) {
        this.quantity = quantity;
        this.sellPrice = sellPrice;
        this.customerOrder = customerOrder;
        this.product = product;
        this.customerOrderLinePK = new CustomerOrderLinePK(customerOrder.getId(), product.getId());
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerOrderLine that = (CustomerOrderLine) o;
        return quantity.equals(that.quantity) && sellPrice.equals(that.sellPrice)
                && customerOrder.equals(that.customerOrder) && product.equals(that.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantity, sellPrice, customerOrder, product);
    }

    @Override
    public String toString() {
        return "CustomerOrderLine{" +
                "quantity=" + quantity +
                ", sellPrice=" + sellPrice +
                ", customerOrder=" + customerOrder +
                ", product=" + product +
                '}';
    }

    /* ---------------------------------------------- GETTERS & SETTERS --------------------------------------------- */

    public CustomerOrderLinePK getCustomerOrderLinePK() {
        return customerOrderLinePK;
    }

    public void setCustomerOrderLinePK(CustomerOrderLinePK customerOrderLinePK) {
        this.customerOrderLinePK = customerOrderLinePK;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(Double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public CustomerOrder getCustomerOrder() {
        return customerOrder;
    }

    public void setCustomerOrder(CustomerOrder customerOrder) {
        this.customerOrder = customerOrder;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
