package fi.haagahelia.stockmanager.model.product.brand;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "BRU_BRAND")
public class Brand {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bra_id")
    private Long id;

    @Column(name = "bra_name", nullable = false, unique = true, updatable = false)
    private String name;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */



    /* ------------------------------------------------ CONSTRUCTORS ------------------------------------------------ */

    public Brand() { }

    public Brand(String name) {
        this.name = name;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Brand brand = (Brand) o;
        return id.equals(brand.id) && name.equals(brand.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Brand{" +
                "id=" + id +
                ", name='" + name + '\'' +
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
}
