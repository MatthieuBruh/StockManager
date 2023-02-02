package fi.haagahelia.stockmanager.model.user;


import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "BRU_ROLE")
public class Role {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rol_id")
    private Long id;

    @Column(name = "rol_name", nullable = false, unique = true)
    private String name;

    @Column(name = "rol_description", nullable = false)
    private String description;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */



    /* ------------------------------------------------ CONSTRUCTORS ------------------------------------------------ */

    public Role() { }

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return name.equals(role.name) && Objects.equals(description, role.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }

    @Override
    public String toString() {
        return "Role{" +
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
