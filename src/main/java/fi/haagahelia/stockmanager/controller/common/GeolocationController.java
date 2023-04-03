package fi.haagahelia.stockmanager.controller.common;

import fi.haagahelia.stockmanager.dto.common.ErrorResponse;
import fi.haagahelia.stockmanager.dto.common.GeolocationCuDTO;
import fi.haagahelia.stockmanager.dto.common.GeolocationDTO;
import fi.haagahelia.stockmanager.model.common.Geolocation;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.common.GeolocationRepository;
import fi.haagahelia.stockmanager.repository.customer.CustomerRepository;
import fi.haagahelia.stockmanager.repository.supplier.SupplierRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Log4j2
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
        Link collectionLink = linkTo(GeolocationController.class).withRel("geolocations");
        geolocationDTO.add(selfLink, collectionLink);
        return geolocationDTO;
    }

    /**
     * This function is used to ensure that a GeolocationCuDTO is valid.
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
        if (geoCuDTO.getStreetName() == null)  return Pair.of(HttpStatus.BAD_REQUEST, "GEOLOCATION_STREET_NAME__NULL.");
        if (geoCuDTO.getStreetName().length() < 1) return Pair.of(HttpStatus.BAD_REQUEST, "GEOLOCATION_STREET_NAME__EMPTY.");
        if (geoCuDTO.getStreetNumber() == null) return Pair.of(HttpStatus.BAD_REQUEST, "GEOLOCATION_STREET_NUMBER__NULL.");
        if (geoCuDTO.getStreetNumber().length() < 1) return Pair.of(HttpStatus.BAD_REQUEST, "GEOLOCATION_STREET_NUMBER__EMPTY.");
        if (geoCuDTO.getPostcode() == null) return Pair.of(HttpStatus.BAD_REQUEST, "GEOLOCATION_POSTCODE_NULL.");
        if (geoCuDTO.getPostcode().length() < 1) return Pair.of(HttpStatus.BAD_REQUEST, "GEOLOCATION_POSTCODE_EMPTY");
        if (geoCuDTO.getLocality() == null) return Pair.of(HttpStatus.BAD_REQUEST, "GEOLOCATION_LOCALITY_NULL.");
        if (geoCuDTO.getLocality().length() < 1) return Pair.of(HttpStatus.BAD_REQUEST, "GEOLOCATION_LOCALITY_EMPTY");
        if (geoCuDTO.getCountry() == null) return Pair.of(HttpStatus.BAD_REQUEST, "GEOLOCATION_COUNTRY_NULL.");
        if (geoCuDTO.getCountry().length() < 1) return Pair.of(HttpStatus.BAD_REQUEST, "GEOLOCATION_COUNTRY_EMPTY.");
        if (gRepository.existsByStreetNameAndStreetNumberAndPostcodeAndCountry(geoCuDTO.getStreetName(),
                geoCuDTO.getStreetNumber(), geoCuDTO.getPostcode(), geoCuDTO.getCountry())) {
            return Pair.of(HttpStatus.CONFLICT, "GEOLOCATION_ALREADY_EXISTS");
        }
        return Pair.of(HttpStatus.ACCEPTED, "");
    }

    /* ------------------------------------------------- API METHODS ------------------------------------------------ */

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to find all the geolocations.
     * Firstly, database query to find all through the geolocation repository.
     * Secondly, verification if there is at least on geolocation.
     *      --> If not, return to the user an HttpStatus.NO_CONTENT
     * Thirdly, converting each Geolocation object into a GeolocationDTO one and adding HATEOAS links.
     * Fourthly, updating the PageModel of GeolocationDTO.
     * Finally, return the PageModel of GeolocationDTO object with HttpStatus.OK.
     *
     * @param user authenticated Employee object
     * @param searchQuery the search query, which can be null or an empty string
     * @param pageable pagination information (page number, size, and sorting)
     * @param sort sorting information for the query
     * @return a ResponseEntity containing a page model of GeolocationDTO objects or a Error Message.
     *      --> HttpStatus.OK if at least one geolocation has been found. (Page of GeolocationDTO)
     *      --> HttpStatus.NO_CONTENT if no geolocation exists. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_VENDOR', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> getGeolocations(@AuthenticationPrincipal Employee user,
                                                           @RequestParam(required = false) String searchQuery,
                                                           @PageableDefault(size = 10) Pageable pageable,
                                                           @SortDefault.SortDefaults({
                                                                   @SortDefault(sort = "streetName", direction = Sort.Direction.ASC)}) Sort sort) {
        try {
            log.info("User {} is requesting all the geolocations from the database.", user.getUsername());
            Specification<Geolocation> spec = null;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                spec = (root, query, cb) -> cb.like(cb.lower(root.get("streetName")), "%" + searchQuery.toLowerCase() + "%");
            }
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            Page<Geolocation> geolocations = gRepository.findAll(spec, pageable);
            if (geolocations.getTotalElements() < 1) {
                log.info("User {} requested all the geolocations from the database. NO DATA FOUND.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_GEOLOCATION_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            List<GeolocationDTO> geolocationDTOS = new ArrayList<>();
            for (Geolocation geolocation : geolocations) {
                GeolocationDTO geolocationDTO = GeolocationDTO.convert(geolocation);
                geolocationDTOS.add(createHATEOAS(geolocationDTO));
            }
            PagedModel.PageMetadata pmd = new PagedModel.PageMetadata(geolocations.getSize(), geolocations.getNumber(), geolocations.getTotalElements());
            PagedModel<GeolocationDTO> geolocationDTOPage = PagedModel.of(geolocationDTOS, pmd);
            geolocationDTOPage.add(linkTo(GeolocationController.class).withRel("geolocations"));
            log.info("User {} requested all the geolocations from the database. RETURNING DATA.", user.getUsername());
            return new ResponseEntity<>(geolocationDTOPage, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested all the geolocations. UNEXPECTED ERROR!", user.getUsername());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to find a specific geolocation by the id given by the user.
     * Firstly, database query to find the geolocation that corresponds to the id.
     * Secondly, verification that the optional has an object inside.
     *      --> If not, return an HttpStatus.BAD_REQUEST.
     * Thirdly, convert the geolocation as a GeolocationDTO. Adding the HATEOAS links.
     * Finally, return the GeolocationDTO with an HttpStatus.OK.
     *
     * @param id Corresponds to the id of the geolocation that the user wants to access.
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing a page model of GeolocationDTO objects or a Error Message.
     *      --> HttpStatus.OK if the geolocation has been found. (GeolocationDTO)
     *      --> HttpStatus.BAD_REQUEST if no geolocation has been found. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_VENDOR', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> getGeolocationById(@PathVariable(value = "id") Long id,
                                                              @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting the geolocation with id: '{}'.", user.getUsername(), id);
            Optional<Geolocation> geoFounded = gRepository.findById(id);
            if (!geoFounded.isPresent()) {
                log.info("User {} requested the geolocation with id: '{}'. NO DATA FOUND", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_GEOLOCATION_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            GeolocationDTO geolocationDTO = GeolocationDTO.convert(geoFounded.get());
            log.info("User {} requested the geolocation with id: '{}'. RETURNING DATA", user.getUsername(), id);
            return new ResponseEntity<>(createHATEOAS(geolocationDTO), HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested the geolocation with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to create and save a new geolocation.
     * Firstly, validation of the GeolocationCuDTO object provided by the user, using the validateGeolocation function.
     *      --> If the object is invalid, we return an HttpStatus that corresponds to the reason of the invalidation.
     * Secondly, create and set up Geolocation object. Saving the created Geolocation in the database.
     * Thirdly, convert the returned Geolocation as a GeolocationDTO. Adding HATEOAS links.
     * Finally, returning the GeolocationDTO to the user with an HttpStatus.CREATED.
     *
     * @param geoCuDTO Corresponds to the geolocation that the user wants to create and save.
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing a GeolocationDTO objects or a Error Message.
     *      --> HttpStatus.OK if the geolocation has been created. (GeolocationDTO)
     *      --> HttpStatus.XX if a criteria has not been validated. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_VENDOR', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> createGeo(@RequestBody GeolocationCuDTO geoCuDTO,
                                                     @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to create and save a new geolocation: ({} {}, {}, {}).", user.getUsername(),
                    geoCuDTO.getStreetName(), geoCuDTO.getStreetNumber(), geoCuDTO.getPostcode(), geoCuDTO.getCountry());
            Pair<HttpStatus, String> validation = validateGeolocation(geoCuDTO);
            if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
                log.info("User {} requested to create and save a new geolocation ({} {}, {}, {}). {}", user.getUsername(),
                        geoCuDTO.getStreetName(), geoCuDTO.getStreetNumber(), geoCuDTO.getPostcode(), geoCuDTO.getCountry(), validation.getSecond());
                ErrorResponse bm = new ErrorResponse(validation.getFirst().getReasonPhrase(), validation.getSecond());
                return new ResponseEntity<>(bm, validation.getFirst());
            }
            Geolocation geolocation = new Geolocation(); geolocation.setStreetName(geoCuDTO.getStreetName());
            geolocation.setStreetNumber(geoCuDTO.getStreetNumber()); geolocation.setPostcode(geoCuDTO.getPostcode());
            geolocation.setLocality(geoCuDTO.getLocality()); geolocation.setCountry(geoCuDTO.getCountry());

            log.info("User {} requested to create and save a new geolocation ({} {}, {}, {}). SAVING DATA.",
                    user.getUsername(), geoCuDTO.getStreetName(), geoCuDTO.getStreetNumber(), geoCuDTO.getPostcode(), geoCuDTO.getCountry());
            Geolocation savedGeolocation = gRepository.save(geolocation);
            GeolocationDTO geolocationDTO = GeolocationDTO.convert(savedGeolocation);
            createHATEOAS(geolocationDTO);
            log.info("User {} requested to create and save a new geolocation ({} {}, {}, {}). GEOLOCATION CREATED AND SAVED.",
                    user.getUsername(), geoCuDTO.getStreetName(), geoCuDTO.getStreetNumber(), geoCuDTO.getPostcode(), geoCuDTO.getCountry());
            return new ResponseEntity<>(geolocationDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            log.info("User {} requested to create and save a new geolocation ({} {}, {}, {}). UNEXPECTED ERROR!", user.getUsername(),
                    geoCuDTO.getStreetName(), geoCuDTO.getStreetNumber(), geoCuDTO.getPostcode(), geoCuDTO.getCountry());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_ADMIN
     * This function is used to delete a geolocation by its id.
     * Firstly, check that a geolocation corresponds to the given id.
     *      --> If not, return an HttpStatus.BAD_REQUEST.
     * Secondly, check that no supplier or customer are related to the geolocation.
     *      --> If yes, return an HttpStatus.CONFLICT.
     * Finally, deletion of the geolocation using the geolocation repository, and returning an HttpStatus.NO_CONTENT.
     *
     * @param id Corresponds to the id of the geolocation that the user wants to delete.
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing an Error Message.
     *      --> HttpStatus.OK if the geolocation has been deleted.
     *      --> HttpStatus.BAD_REQUEST if no geolocation corresponds to the given id.
     *      --> HttpStatus.CONFLICT if the geolocation has at least one relationship.
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs.
     */
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ErrorResponse> deleteGeolocation(@PathVariable(value = "id") Long id,
                                                                         @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to delete the geolocation with id: '{}'", user.getUsername(), id);
            if (!gRepository.existsById(id)) {
                log.info("User {} requested to delete the geolocation with id: '{}'. NO DATA FOUND", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_GEOLOCATION_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            if (cRepository.existsByGeolocationId(id) || sRepository.existsByGeolocationId(id)) {
                log.info("User {} requested to delete the geolocation with id: '{}'. " +
                        "CUSTOMER OR SUPPLIER ARE STILL RELATED TO THIS GEOLOCATION.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.CONFLICT.getReasonPhrase(), "GEOLOCATION_HAS_RELATIONSHIPS");
                return new ResponseEntity<>(bm, HttpStatus.CONFLICT);
            }
            log.warn("User {} requested to delete the geolocation with the id: '{}'. DELETING GEOLOCATION.", user.getUsername(), id);
            gRepository.deleteById(id);
            log.info("User {} requested to delete the geolocation with the id: '{}'. GEOLOCATION DELETED.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to delete the geolocation with id: '{}'. UNEXPECTED ERROR!", user.getUsername(),id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
