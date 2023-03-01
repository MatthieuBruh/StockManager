package fi.haagahelia.stockmanager.repository.common;

import fi.haagahelia.stockmanager.model.common.Geolocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeolocationRepository extends JpaRepository<Geolocation, Long> {
    Optional<Geolocation> findById(Long id);
    Page<Geolocation> findAll(Specification<Geolocation> spec, Pageable pageable);
    Boolean existsByStreetNameAndStreetNumberAndPostcodeAndCountry(String streetName, String streetNumber,
                                                                   String postcode, String country);
}
