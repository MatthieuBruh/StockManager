package fi.haagahelia.stockmanager.model.supplier.order;

import fi.haagahelia.stockmanager.model.supplier.Supplier;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "BRU_SUPPLIER_ORDER")
public class SupplierOrder {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suo_id")
    private Long id;

    @Column(name = "suo_date", nullable = false)
    private LocalDate date;

    @Column(name = "suo_delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "suo_is_sent", nullable = false)
    private Boolean orderIsSent;

    @Column(name = "suo_is_received", nullable = false)
    private Boolean isReceived;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suo_sup_id")
    private Supplier supplier;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "supplierOrder")
    private List<SupplierOrderLine> supplierOrderLines;


    /* ------------------------------------------------ CONSTRUCTORS ------------------------------------------------ */

    public SupplierOrder() { }

    public SupplierOrder(LocalDate date, LocalDate deliveryDate, Boolean orderIsSent, Boolean isReceived, Supplier supplier) {
        this.date = date;
        this.deliveryDate = deliveryDate;
        this.orderIsSent = orderIsSent;
        this.isReceived = isReceived;
        this.supplier = supplier;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SupplierOrder that = (SupplierOrder) o;
        return date.equals(that.date) && deliveryDate.equals(that.deliveryDate) && supplier.equals(that.supplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, deliveryDate, supplier);
    }

    /* ---------------------------------------------- GETTERS & SETTERS --------------------------------------------- */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public Boolean getOrderIsSent() {
        return orderIsSent;
    }

    public void setOrderIsSent(Boolean orderIsSent) {
        this.orderIsSent = orderIsSent;
    }

    public Boolean getReceived() {
        return isReceived;
    }

    public void setReceived(Boolean received) {
        isReceived = received;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public List<SupplierOrderLine> getSupplierOrderLines() {
        return supplierOrderLines;
    }

    public void setSupplierOrderLines(List<SupplierOrderLine> supplierOrderLines) {
        this.supplierOrderLines = supplierOrderLines;
    }

    public void addSupplierOrderLine(SupplierOrderLine supplierOrderLine) {
        this.supplierOrderLines.add(supplierOrderLine);
    }

    public void removeSupplierOrderLine(SupplierOrderLine supplierOrderLine) {
        this.supplierOrderLines.remove(supplierOrderLine);
    }
}
