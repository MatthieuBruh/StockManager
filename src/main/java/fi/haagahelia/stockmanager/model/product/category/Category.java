package fi.haagahelia.stockmanager.model.product.category;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "BRU_CATEGORY")
public class Category {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cat_id")
    private Long id;

    @Column(name = "cat_name", nullable = false, updatable = false, unique = true)
    private String name;

    @Column(name = "cat_description", nullable = false)
    private String description;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */


    /* ------------------------------------------------ CONSTRUCTORS ------------------------------------------------ */

    public Category() { }

    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id.equals(category.id) && name.equals(category.name) && description.equals(category.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description);
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
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
}
