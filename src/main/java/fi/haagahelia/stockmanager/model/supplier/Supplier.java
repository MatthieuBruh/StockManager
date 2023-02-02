package fi.haagahelia.stockmanager.model.supplier;

import fi.haagahelia.stockmanager.model.common.Geolocation;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "BRU_SUPPLIER")
public class Supplier {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sup_id")
    private Long id;

    @Column(name = "sup_name", nullable = false, unique = true)
    private String name;

    @Column(name = "sup_email")
    private String email;

    @Column(name = "sup_phone")
    private String phoneNumber;


    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sup_geo_id")
    private Geolocation geolocation;

    /* ------------------------------------------------ CONSTRUCTORS ------------------------------------------------ */

    public Supplier() { }

    public Supplier(String name, String email, String phoneNumber, Geolocation geolocation) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.geolocation = geolocation;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Supplier supplier = (Supplier) o;
        return id.equals(supplier.id) && name.equals(supplier.name) && Objects.equals(email, supplier.email) && Objects.equals(phoneNumber, supplier.phoneNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, phoneNumber);
    }

    @Override
    public String toString() {
        return "Supplier{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Geolocation getGeolocation() {
        return geolocation;
    }

    public void setGeolocation(Geolocation geolocation) {
        this.geolocation = geolocation;
    }
}
