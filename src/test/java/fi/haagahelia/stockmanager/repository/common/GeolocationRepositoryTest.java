package fi.haagahelia.stockmanager.repository.common;

import fi.haagahelia.stockmanager.model.common.Geolocation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Log4j2
public class GeolocationRepositoryTest {

    @Autowired
    private GeolocationRepository gRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @BeforeEach
    public void setUp() {
        EntityManager em = testEntityManager.getEntityManager();
        Query geolocationDelete = em.createQuery("DELETE Geolocation  g");
        geolocationDelete.executeUpdate();
    }

    /**
     * This test is used to ensure that the geolocation repository can find the geolocation that corresponds to an id.
     */
    @Test
    public void findById() {
        // Initialization
        Geolocation geolocation = new Geolocation("Ratapihantie", "13", "00520", "Helsinki", "Finland");
        log.debug("GEOLOCATION TEST - FIND BY ID - New geolocation created: " + geolocation);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(geolocation);
        em.getTransaction().commit();
        log.debug("GEOLOCATION TEST - FIND BY ID - New geolocation saved: " + geolocation);
        // Execution
        Optional<Geolocation> geolocationOptional = gRepository.findById(geolocation.getId());
        // Verification
        log.debug("GEOLOCATION TEST - FIND BY ID - Geolocation verifications");
        assertTrue(geolocationOptional.isPresent());
        Geolocation geoLocationFound = geolocationOptional.get();
        assertNotNull(geoLocationFound);
        assertNotNull(geoLocationFound.getId()); assertEquals(geolocation.getId(), geoLocationFound.getId());
        assertNotNull(geoLocationFound.getStreetName()); assertEquals(geolocation.getStreetName(), geoLocationFound.getStreetName());
        assertNotNull(geoLocationFound.getStreetNumber()); assertEquals(geolocation.getStreetNumber(), geoLocationFound.getStreetNumber());
        assertNotNull(geoLocationFound.getPostcode()); assertEquals(geolocation.getPostcode(), geoLocationFound.getPostcode());
        assertNotNull(geoLocationFound.getLocality()); assertEquals(geolocation.getLocality(), geoLocationFound.getLocality());
        assertNotNull(geoLocationFound.getCountry()); assertEquals(geolocation.getCountry(), geoLocationFound.getCountry());
    }

    /**
     * This test is used to ensure that the geolocation repository will not find a geolocation if we give a wrong id.
     */
    @Test
    public void notFoundById() {
        // Execution
        Optional<Geolocation> optionalGeolocation = gRepository.findById(99999L);
        // Verification
        log.debug("GEOLOCATION TEST - NOT FOUND BY ID - Geolocation verification");
        assertFalse(optionalGeolocation.isPresent());
    }

    /**
     * This method is used to ensure that the geolocation repository can return if a specific geolocation exists.
     */
    @Test
    public void existsByStreetNameAndStreetNumberAndPostcodeAndCountry() {
        // Initialization
        Geolocation geolocation = new Geolocation("Campus Battelle, Rue de la Tambourine", "17",
                "1227", "Carouge", "Switzerland");
        log.debug("GEOLOCATION TEST - EXISTS BY ALL ATTRIBUTES - New geolocation created: " + geolocation);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(geolocation);
        em.getTransaction().commit();
        log.debug("GEOLOCATION TEST - EXISTS BY ALL ATTRIBUTES - New geolocation saved: " + geolocation);
        // Execution
        Boolean result = gRepository.existsByStreetNameAndStreetNumberAndPostcodeAndCountry(
                "Campus Battelle, Rue de la Tambourine", "17", "1227", "Switzerland");
        // Verification
        log.debug("GEOLOCATION TEST - EXISTS BY ALL ATTRIBUTES - Geolocation verifications");
        assertNotNull(result);
        assertTrue(result);
    }

    /**
     * This method is used to ensure that the geolocation repository can return if a specific geolocation exists.
     */
    @Test
    public void doesNotExistsByStreetNameAndStreetNumberAndPostcodeAndCountry() {
        // Execution
        Boolean result = gRepository.existsByStreetNameAndStreetNumberAndPostcodeAndCountry(
                "DOES NOT EXIST", "XXX", "XXXXX", "NONE");
        // Verification
        assertNotNull(result);
        assertFalse(result);
    }
}