package fi.haagahelia.stockmanager.model.customer.order;

import fi.haagahelia.stockmanager.model.customer.Customer;
import fi.haagahelia.stockmanager.model.user.Employee;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "BRU_CUSTOMER_ORDER")
public class CustomerOrder {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cuo_id")
    private Long id;

    @Column(name = "cuo_date", nullable = false)
    private LocalDate date;

    @Column(name = "cuo_delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "cuo_is_sent", nullable = false)
    private Boolean isSent;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuo_emp_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuo_cus_id")
    private Customer customer;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "customerOrder")
    private List<CustomerOrderLine> customerOrderLines;

    /* ------------------------------------------------ CONSTRUCTORS ------------------------------------------------ */

    public CustomerOrder() { this.customerOrderLines = new ArrayList<>(); }

    public CustomerOrder(LocalDate date, LocalDate deliveryDate, Boolean isSent, Employee employee, Customer customer) {
        this.date = date;
        this.deliveryDate = deliveryDate;
        this.isSent = isSent;
        this.employee = employee;
        this.customer = customer;
        this.customerOrderLines = new ArrayList<>();
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerOrder that = (CustomerOrder) o;
        return date.equals(that.date) && deliveryDate.equals(that.deliveryDate) && employee.equals(that.employee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, deliveryDate, employee);
    }

    @Override
    public String toString() {
        return "CustomerOrder{" +
                "id=" + id +
                ", date=" + date +
                ", deliveryDate=" + deliveryDate +
                ", isSent=" + isSent +
                '}';
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

    public Boolean getSent() {
        return isSent;
    }

    public void setSent(Boolean sent) {
        isSent = sent;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<CustomerOrderLine> getCustomerOrderLines() {
        return customerOrderLines;
    }

    public void setCustomerOrderLines(List<CustomerOrderLine> customerOrderLines) {
        this.customerOrderLines = customerOrderLines;
    }

    public void addCustomerOrderLine(CustomerOrderLine customerOrderLine) {
        this.customerOrderLines.add(customerOrderLine);
    }

    public void removeCustomerOrderLine(CustomerOrderLine customerOrderLine) {
        this.customerOrderLines.remove(customerOrderLine);
    }
}
