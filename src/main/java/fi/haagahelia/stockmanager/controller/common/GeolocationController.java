package fi.haagahelia.stockmanager.controller.common;

import fi.haagahelia.stockmanager.dto.common.GeolocationCuDTO;
import fi.haagahelia.stockmanager.dto.common.GeolocationDTO;
import fi.haagahelia.stockmanager.model.common.Geolocation;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.common.GeolocationRepository;
import fi.haagahelia.stockmanager.repository.customer.CustomerRepository;
import fi.haagahelia.stockmanager.repository.supplier.SupplierRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RestController
@RequestMapping("/api/geolocations")
public class GeolocationController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final GeolocationRepository gRepository;
    private final CustomerRepository cRepository;
    private final SupplierRepository sRepository;

    @Autowired
    public GeolocationController(GeolocationRepository gRepository, CustomerRepository cRepository, SupplierRepository sRepository) {
        this.gRepository = gRepository;
        this.cRepository = cRepository;
        this.sRepository = sRepository;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    /**
     * This method is used to create and add the HATEOAS links to a DTO models.
     * @param geolocationDTO The dto model to which we will add the HATEOAS links.
     * @return The dto model with the HATEOAS links.
     */
    private GeolocationDTO createHATEOAS(GeolocationDTO geolocationDTO) {
        Link selfLink = linkTo(GeolocationController.class).slash(String.valueOf(geolocationDTO.getId())).withSelfRel();
        Link collectionLink = linkTo(methodOn(GeolocationController.class)).slash("").withRel("geolocations");
        geolocationDTO.add(selfLink, collectionLink);
        return geolocationDTO;
    }

    /**
     * This function is used to ensre that a GeolocationCuDTO is valid.
     * We check the following constraints:
     *      - Street name is not null and not empty.
     *      - Street number is not null and not empty.
     *      - Postcode is not null and not empty.
     *      - Locality is not null and not empty.
     *      - Country is not null and not empty.
     * @param geoCuDTO Corresponds to the geolocation that we have to validate.
     * @return A Pair object that contains an HttpStatus and the decision reason.
     */
    private Pair<HttpStatus, String> validateGeolocation(GeolocationCuDTO geoCuDTO) {
        if (geoCuDTO.getStreetName() == null)  return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "STREET NAME IS NULL.");
        if (geoCuDTO.getStreetName().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "STREET NAME IS EMPTY.");
        if (geoCuDTO.getStreetNumber() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "STREET NUMBER IS NULL.");
        if (geoCuDTO.getStreetNumber().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "STREET NUMBER IS EMPTY.");
        if (geoCuDTO.getPostcode() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "POSTCODE IS NULL.");
        if (geoCuDTO.getPostcode().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "POSTCODE IS EMPTY");
        if (geoCuDTO.getLocality() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "LOCALITY IS NULL.");
        if (geoCuDTO.getLocality().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "LOCALITY IS EMPTY");
        if (geoCuDTO.getCountry() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "COUNTRY IS NULL.");
        if (geoCuDTO.getCountry().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "COUNTRY IS EMPTY.");
        if (gRepository.existsByStreetNameAndStreetNumberAndPostcodeAndCountry(geoCuDTO.getStreetName(),
                geoCuDTO.getStreetNumber(), geoCuDTO.getPostcode(), geoCuDTO.getCountry())) {
            return Pair.of(HttpStatus.CONFLICT, "GEOLOCATION ALREADY EXISTS");
        }
        return Pair.of(HttpStatus.ACCEPTED, "");
    }

    /* ------------------------------------------------- API METHODS ------------------------------------------------ */

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to find all the geolocations that are in the database.
     * Firstly, we do a query to the database to find all the geolocations using the geolocation repository.
     * Secondly, we check that the list that was returned by the geolocation repository contains at least one geolocation.
     *      If not, we return an HttpStatu.NO_CONTENT to the user.
     * Thirdly, we create a new list of GeolocationDTO.
     * We iterate over all the objects from the database list to convert each of them into a GeolocationDTO object.
     * We also add the HATEOAS links to the GeolocationDTO by using the createHATEOAS function.
     * Each object that has been converted is added to the list of GeolocationDTO.
     * Finally, we return to the user the list of GeolocationDTO with an HttpStatus.OK.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<List<GeolocationDTO>> getAllGeolocation(@AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting all the geolocations from the database.", user.getUsername());
        List<Geolocation> geolocations = gRepository.findAll();
        if (geolocations.size() < 1) {
            log.info("User {} requested all the geolocations from the database. NO DATA FOUND.", user.getUsername());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<GeolocationDTO> geolocationDTOS = new ArrayList<>();
        for (Geolocation geolocation : geolocations) {
            GeolocationDTO geolocationDTO = GeolocationDTO.convert(geolocation);
            geolocationDTOS.add(createHATEOAS(geolocationDTO));
        }
        log.info("User {} requested all the geolocations from the database. RETURNING DATA.", user.getUsername());
        return new ResponseEntity<>(geolocationDTOS, HttpStatus.OK);
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to find a specific geolocation by the id given by the user.
     * Firstly, we do a query to the database to select the geolocation that corresponds to the id using the geolocation repo.
     * Secondly, we check that the Optional that was returned by the geolocation repository is empty.
     *      If it is empty, we return an HttpStatu.NO_CONTENT to the user.
     * Thirdly, we convert the geolocation as a GeolocationDTO by getting the brand that is inside the Optional.
     * We also add the HATEOAS links to the GeolocationDTO object.
     * Finally, we can return the object to the user with an HttpStatus.OK.
     * @param id Corresponds to the id of the geolocation that the user wants to access.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @GetMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<GeolocationDTO> getGeolocationById(@PathVariable(value = "id") Long id,
                                                                           @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting the geolocation with id: '{}'.", user.getUsername(), id);
        Optional<Geolocation> geoFounded = gRepository.findById(id);
        if (geoFounded.isEmpty()) {
            log.info("User {} requested the geolocation with id: '{}'. NO DATA FOUND", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        GeolocationDTO geolocationDTO = GeolocationDTO.convert(geoFounded.get());
        log.info("User {} requested the geolocation with id: '{}'. RETURNING DATA", user.getUsername(), id);
        return new ResponseEntity<>(createHATEOAS(geolocationDTO), HttpStatus.OK);
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to create and save a new geolocation.
     * Firstly, we check that the GeolocationCuDTO object is valide by using the validateGeolocation function.
     *      If not, we return an HttpStatus code to the user.
     * Secondly, we create a new Geolocation object, and we set all the attributes of the latter.
     * Thirdly, we save the new object in the database.
     * Fourthly, we convert the Geolocation as a GeolocationDTO and we add the HATEOAS links to it.
     * Finally, we return all the data to the user.
     * @param geoCuDTO Corresponds to the geolocation that the user wants to create and save.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<GeolocationDTO> createGeo(@RequestBody GeolocationCuDTO geoCuDTO,
                                                                  @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to create and save a new geolocation: ({} {}, {}, {}).", user.getUsername(),
                geoCuDTO.getStreetName(), geoCuDTO.getStreetNumber(), geoCuDTO.getPostcode(), geoCuDTO.getCountry());
        Pair<HttpStatus, String> validation = validateGeolocation(geoCuDTO);
        if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
            log.info("User {} requested to create and save a new geolocation ({} {}, {}, {}). {}", user.getUsername(),
                    geoCuDTO.getStreetName(), geoCuDTO.getStreetNumber(), geoCuDTO.getPostcode(), geoCuDTO.getCountry(),
                    validation.getSecond());
            return new ResponseEntity<>(validation.getFirst());
        }
        Geolocation geolocation = new Geolocation();
        geolocation.setStreetName(geoCuDTO.getStreetName());
        geolocation.setStreetNumber(geoCuDTO.getStreetNumber());
        geolocation.setPostcode(geoCuDTO.getPostcode());
        geolocation.setLocality(geoCuDTO.getLocality());
        geolocation.setCountry(geoCuDTO.getCountry());
        log.info("User {} requested to create and save a new geolocation ({} {}, {}, {}). SAVING DATA.",
                user.getUsername(), geoCuDTO.getStreetName(), geoCuDTO.getStreetNumber(),
                geoCuDTO.getPostcode(), geoCuDTO.getCountry());
        Geolocation savedGeolocation = gRepository.save(geolocation);
        GeolocationDTO geolocationDTO = GeolocationDTO.convert(savedGeolocation);
        createHATEOAS(geolocationDTO);
        log.info("User {} requested to create and save a new geolocation ({} {}, {}, {}). GEOLOCATION CREATED AND SAVED.",
                user.getUsername(), geoCuDTO.getStreetName(), geoCuDTO.getStreetNumber(),
                geoCuDTO.getPostcode(), geoCuDTO.getCountry());
        return new ResponseEntity<>(geolocationDTO, HttpStatus.CREATED);
    }

    /**
     * AVAILABLE FOR: ROLE_ADMIN
     * This function is used to delete a saved geolocation from the database.
     * Firstly, we check that a geolocation exists in the database with the id given by the user.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Secondly, we verify that no customers or suppliers are related to this brand.
     *      If a customer or supplier is related to this geolocation, it's not possible to delete the geolocation.
     *      We return an HttpStatus.NOT_ACCEPTABLE to the user.
     * Finally, we can delete the geolocation from the database, and we return an HttpStatus.ACCEPTED to the user.
     * @param id Corresponds to the id of the category that the user wants to delete.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus.
     */
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<GeolocationDTO> deleteGeolocationById(@PathVariable(value = "id") Long id,
                                                                              @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to delete the geolocation with id: '{}'", user.getUsername(), id);
        if (!gRepository.existsById(id)) {
            log.info("User {} requested to delete the geolocation with id: '{}'. NO DATA FOUND", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        if (cRepository.existsByGeolocationId(id) || sRepository.existsByGeolocationId(id)) {
            log.info("User {} requested to delete the geolocation with id: '{}'." +
                    "CUSTOMER OR SUPPLIER ARE STILL RELATED TO THIS GEOLOCATION.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        log.warn("User {} requested to delete the geolocation with the id: '{}'. DELETING GEOLOCATION.",
                user.getUsername(), id);
        gRepository.deleteById(id);
        log.info("User {} requested to delete the geolocation with the id: '{}'. GEOLOCATION DELETED.",
                user.getUsername(), id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
