package fi.haagahelia.stockmanager.model.user;


import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "BRU_EMPLOYEE")
public class Employee implements UserDetails {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emp_id")
    private Long id;

    @Column(name = "emp_email", nullable = false, unique = true, updatable = false)
    private String email;

    @Column(name = "emp_username", nullable = false, unique = true, updatable = false)
    private String username;

    @Column(name = "emp_first_name", nullable = false)
    private String firstName;

    @Column(name = "emp_last_name", nullable = false)
    private String lastName;

    @Column(name = "emp_password", nullable = false)
    private String password;

    @Column(name = "emp_is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "emp_is_blocked", nullable = false)
    private Boolean isBlocked;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "BRU_EMP_ROLES",
            joinColumns = @JoinColumn(name = "employee_id", referencedColumnName = "emp_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "rol_id"))
    private List<Role> roles = new ArrayList<>();

    /* ------------------------------------------------ CONSTRUCTORS ------------------------------------------------ */

    public Employee() { }

    public Employee(String email, String username, String firstName, String lastName, String password,
                    Boolean isActive, Boolean isBlocked) {
        this.email = email;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.isActive = isActive;
        this.isBlocked = isBlocked;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Employee employee = (Employee) o;
        return email.equals(employee.email) && username.equals(employee.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, username);
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", password='" + password + '\'' +
                ", isActive=" + isActive +
                ", isBlocked=" + isBlocked +
                '}';
    }

    /* ---------------------------------------------- GETTERS & SETTERS --------------------------------------------- */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public Boolean getBlocked() {
        return isBlocked;
    }

    public void setBlocked(Boolean blocked) {
        isBlocked = blocked;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    /* -------------------------------- TOOLS RELATED TO USER DETAILS IMPLEMENTATION -------------------------------- */

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream().map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (!isBlocked) return true;
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
