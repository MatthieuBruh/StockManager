package fi.haagahelia.stockmanager.model.common;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "BRU_GEOLOCATION")
public class Geolocation {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "geo_id")
    private Long id;

    @Column(name = "geo_street_name", nullable = false)
    private String streetName;

    @Column(name = "geo_street_number", nullable = false)
    private String streetNumber;

    @Column(name = "geo_postcode", nullable = false)
    private String postcode;

    @Column(name = "geo_locality", nullable = false)
    private String locality;

    @Column(name = "geo_country", nullable = false)
    private String country;

    /* -------------------------------------------------- RELATIONS ------------------------------------------------- */


    /* ------------------------------------------------ CONSTRUCTORS ------------------------------------------------ */

    public Geolocation() { }

    public Geolocation(String streetName, String streetNumber, String postcode, String locality, String country) {
        this.streetName = streetName;
        this.streetNumber = streetNumber;
        this.postcode = postcode;
        this.locality = locality;
        this.country = country;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Geolocation that = (Geolocation) o;
        return streetName.equals(that.streetName) && streetNumber.equals(that.streetNumber)
                && postcode.equals(that.postcode) && locality.equals(that.locality) && country.equals(that.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(streetName, streetNumber, postcode, locality, country);
    }

    @Override
    public String toString() {
        return "Geolocation{" +
                "id=" + id +
                ", streetName='" + streetName + '\'' +
                ", streetNumber='" + streetNumber + '\'' +
                ", postcode='" + postcode + '\'' +
                ", locality='" + locality + '\'' +
                ", country='" + country + '\'' +
                '}';
    }

    /* ---------------------------------------------- GETTERS & SETTERS --------------------------------------------- */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getStreetNumber() {
        return streetNumber;
    }

    public void setStreetNumber(String streetNumber) {
        this.streetNumber = streetNumber;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
