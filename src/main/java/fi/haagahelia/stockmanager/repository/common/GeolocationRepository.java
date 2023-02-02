package fi.haagahelia.stockmanager.repository.common;

import fi.haagahelia.stockmanager.model.common.Geolocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeolocationRepository extends JpaRepository<Geolocation, Long> {
    Optional<Geolocation> findById(Long id);
    Boolean existsByStreetNameAndStreetNumberAndPostcodeAndCountry(String streetName, String streetNumber,
                                                                   String postcode, String country);
}
