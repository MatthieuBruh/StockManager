package fi.haagahelia.stockmanager.controller.supplier;


import fi.haagahelia.stockmanager.controller.common.GeolocationController;
import fi.haagahelia.stockmanager.dto.supplier.SupplierCuDTO;
import fi.haagahelia.stockmanager.dto.supplier.SupplierDTO;
import fi.haagahelia.stockmanager.model.common.Geolocation;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.common.GeolocationRepository;
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

@Slf4j
@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final SupplierRepository sRepository;
    private final GeolocationRepository gRepository;

    @Autowired
    public SupplierController(SupplierRepository sRepository, GeolocationRepository gRepository) {
        this.sRepository = sRepository;
        this.gRepository = gRepository;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    /**
     * This method is used to create and add the HATEOAS links to a DTO models.
     * @param supplierDTO The dto model to which we will add the HATEOAS links.
     * @return The dto model with the HATEOAS links.
     */
    private SupplierDTO createHATEOAS(SupplierDTO supplierDTO) {
        Link selfLink = linkTo(SupplierController.class).slash(String.valueOf(supplierDTO.getId())).withSelfRel();
        supplierDTO.add(selfLink);

        Link collectionLink = linkTo(SupplierController.class).withRel("suppliers");
        supplierDTO.add(collectionLink);

        // Geolocation field
        if (supplierDTO.getGeolocation() != null) {
            Long geoId = supplierDTO.getId();
            Link geolocation = linkTo(GeolocationController.class).slash(geoId).withRel("geolocation");
            supplierDTO.add(geolocation);
        }

        return supplierDTO;
    }

    /**
     * This function is used to validate the data of a supplier (before create or update)
     * We check the following constrains:
     *      - Name must not be null or empty
     *      - If geolocationId is not null, the id should be valid
     *      - In case of a creation: name should not exist
     *      - In case of an update: name should exist
     * @param supplierCuDTO Corresponds to the supplier's information to validate
     * @param isForUpdate Used to know if we check value for a update or a creation.
     * @return A Pair object that contains an HttpStatus and the decision reason.
     */
    private Pair<HttpStatus, String> validateSupplier(SupplierCuDTO supplierCuDTO, boolean isForUpdate) {
        if (supplierCuDTO.getName() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "NAME IS NULL");
        if (supplierCuDTO.getName().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "NAME IS EMPTY");
        if (supplierCuDTO.getGeolocationId() != null) {
            if (!gRepository.existsById(supplierCuDTO.getGeolocationId())) {
                return Pair.of(HttpStatus.NOT_FOUND, "NO GEOLOCATION FOUND");
            }
        }
        if (isForUpdate) {
            if (!sRepository.existsByName(supplierCuDTO.getName())) return Pair.of(HttpStatus.NOT_FOUND, "NOT FOUND");
        } else {
            if (sRepository.existsByName(supplierCuDTO.getName())) return Pair.of(HttpStatus.CONFLICT, "ALREADY EXISTS");
        }
        return Pair.of(HttpStatus.ACCEPTED, "");
    }

    /**
     * This function is used to add a geolocation to a supplier if needed.
     * We start by checking that the id correspond to a valid geolocation.
     * If correspond to a valid geolocation we add the geolocation to the supplier object.
     * Else, we do nothing.
     * @param supplier the supplier that we need to add the geolocation
     * @param geoId the geolocation id
     */
    private void setGeolocationById(Supplier supplier, Long geoId, String username) {
        Optional<Geolocation> geolocationOptional = gRepository.findById(geoId);
        if (geolocationOptional.isPresent()) {
            log.debug("User {} requested to create a new supplier with the name: {}. Adding the geolocation with id: {}.",
                    username, supplier.getName(), geoId);
            Geolocation geolocation = geolocationOptional.get();
            supplier.setGeolocation(geolocation);
        }
    }

    /* ------------------------------------------------- API METHODS ------------------------------------------------ */

    /**
     * Available for: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get all the suppliers that are saved in the database.
     * Firstly, we will SELECT all the suppliers that are in the database.
     * Secondly, we check that the list is not empty.
     * If the list is empty, we return an empty ResponseEntity with an HttpStatus.NO_CONTENT.
     * If the list contains at least one object, we can do the third step.
     * Thirdly, we transform all the suppliers as a SupplierDTO objects. We also add the HATEOAS links.
     * Finally, we return the data in a ResponseEntity with the HttpStatus.OK.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<List<SupplierDTO>> getAllSuppliers(@AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting all the suppliers from the database.", user.getUsername());
        List<Supplier> suppliers = sRepository.findAll();
        if (suppliers.size() < 1) {
            log.info("User {} requested all the suppliers. NO DATA FOUND.", user.getUsername());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<SupplierDTO> supplierDTOS = new ArrayList<>();
        for (Supplier supplier : suppliers) {
            SupplierDTO supplierDTO = SupplierDTO.convert(supplier);
            supplierDTOS.add(createHATEOAS(supplierDTO));
        }
        log.info("User {} requested all the suppliers. RETURNING DATA.", user.getUsername());
        return new ResponseEntity<>(supplierDTOS, HttpStatus.OK);
    }

    /**
     * Available for: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get the supplier that correspond to a given id.
     * Firstly, we will SELECT the data in the database.
     * Secondly, we verify that data is not empty. If data is empty, we return a HttpStatus.NO_CONTENT.
     * Thirdly we transform the supplier as a SupplierDTO object, and we add the HATEOAS links to the object.
     * Finally, if everything went OK, we return the data to the user with a HttpStatus.OK.
     * @param id Correspond to the id of the supplier searched by the user.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @GetMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<SupplierDTO> getSupplierByID(@PathVariable(value = "id") Long id,
                                                                     @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting the supplier with id: {}.", user.getUsername(), id);
        Optional<Supplier> supplierOptional = sRepository.findById(id);
        if (!supplierOptional.isPresent()) {
            log.info("User {} requested the supplier with id: {}. NO DATA FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        SupplierDTO supplierDTO = SupplierDTO.convert(supplierOptional.get());
        createHATEOAS(supplierDTO);
        log.info("User {} requested the supplier with id: {}. RETURNING DATA.", user.getUsername(), id);
        return new ResponseEntity<>(supplierDTO, HttpStatus.OK);
    }

    /**
     * Available for: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to create and save a new supplier.
     * Firstly, we get the new supplier in the RequestBody.
     * Secondly, we check that the information given by the user are correct, using the validateSupplier function.
     *      If not, we return an HttpStatus with the corresponding code.
     * Thirdly, we can create the supplier object and save it in the database.
     * Finally, we convert the saved supplier as a SupplierDTO and we return it to the user with an HttpStatus.CREATED.
     * @param supplierCuDTO Corresponds to the new supplier that the user wants to create.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<SupplierDTO> createNewSupplier(@RequestBody SupplierCuDTO supplierCuDTO,
                                                                       @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to create and save a new supplier with name: {}",
                user.getUsername(), supplierCuDTO.getName());
        Pair<HttpStatus, String> validation = validateSupplier(supplierCuDTO, false);
        if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
            log.info("User {} requested to create and save a new supplier with the name: {}. {}",
                    user.getUsername(), supplierCuDTO.getName(), validation.getSecond());
            return new ResponseEntity<>(validation.getFirst());
        }
        Supplier supplier = new Supplier();
        supplier.setName(supplierCuDTO.getName());
        supplier.setEmail(supplierCuDTO.getEmail());
        supplier.setPhoneNumber(supplierCuDTO.getPhoneNumber());
        setGeolocationById(supplier, supplierCuDTO.getGeolocationId(), user.getUsername());
        log.debug("User {} requested to create and save a new supplier with the name: {}. SAVING SUPPLIER.",
                user.getUsername(), supplier.getName());
        Supplier savedSupplier = sRepository.save(supplier);
        SupplierDTO supplierDTO = SupplierDTO.convert(savedSupplier);
        createHATEOAS(supplierDTO);
        log.info("User {} requested to create and save a new supplier with the name: {}. SUPPLIER CREATED AND SAVED.",
                user.getUsername(), supplier.getName());
        return new ResponseEntity<>(supplierDTO, HttpStatus.CREATED);
    }

    /**
     * Available for: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to update an existing supplier by his name.
     * Firstly, we get the updated supplierCuDTO in the RequestBody.
     * Secondly, we check that a supplier exists with the given name.
     *      If any supplier correspond to the given id, we return an HttpStatus.NO_CONTENT.
     * Thirdly, we check that the supplierCuDTO0's information are correct.
     *      If it is not valid, we return an HttpStatus with the corresponding code.
     * Fourthly, we can change the value of the supplier.
     * Fifthly, we can save the modification in the database.
     * Finally, we convert the saved supplier as a SupplierDTO, we also add the HATEOAS links, and we return it the
     * user with an HttpStatus.ACCEPTED.
     * @param name Correspond to the supplier's name in the database that we want to update
     * @param supplierCuDTO Correspond to the user's new data.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @PutMapping(value = "/{name}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<SupplierDTO> updateSupplier(@PathVariable(value = "name") String name,
                                                                    @RequestBody SupplierCuDTO supplierCuDTO,
                                                                    @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to update the supplier with name: '{}'.", user.getUsername(), name);
        Optional<Supplier> optionalSupplier = sRepository.findByName(name);
        if (!optionalSupplier.isPresent()) {
            log.info("User {} requested to update the supplier with name: '{}'. NO DATA FOUND", user.getUsername(), name);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Pair<HttpStatus, String> validation = validateSupplier(supplierCuDTO, true);
        if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
            log.info("User {} requested to update the supplier with name: '{}'. {}",
                    user.getUsername(), name,validation.getSecond());
            return new ResponseEntity<>(validation.getFirst());
        }
        Supplier supplier = optionalSupplier.get();
        supplier.setName(supplierCuDTO.getName());
        supplier.setEmail(supplierCuDTO.getEmail());
        supplier.setPhoneNumber(supplierCuDTO.getPhoneNumber());
        setGeolocationById(supplier, supplierCuDTO.getGeolocationId(), user.getUsername());
        log.debug("User {} requested to update the supplier with the id: {}. SAVING SUPPLIER.", user.getUsername(), name);
        Supplier savedSupplier = sRepository.save(supplier);
        SupplierDTO supplierDTO = SupplierDTO.convert(savedSupplier);
        log.info("User {} requested to update the supplier with the id: {}. SUPPLIER SAVED.", user.getUsername(), name);
        return new ResponseEntity<>(createHATEOAS(supplierDTO), HttpStatus.ACCEPTED);
    }


    /**
     * Available for: ROLE_ADMIN
     * This function is used to delete a supplier. /!\ DELETE THE RELATED ORDERS AND ORDER LINES
     * Firstly, we check that a supplier exists with the given id.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Finally, we can delete the supplier from the database, and we return an HttpStatus.ACCEPTED to the user.
     * @param id Corresponds to the supplier's id that the user wants to delete.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @DeleteMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<SupplierDTO> deleteSupplierById(@PathVariable(value = "id") Long id,
                                                                        @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to delete the supplier with id: {}.", user.getUsername(), id);
        if (!sRepository.existsById(id)) {
            log.info("User {} requested to delete the supplier with id: {}. NO DATA FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        log.debug("User {} requested to delete the supplier with id: {}. DELETING SUPPLIER.", user.getUsername(), id);
        sRepository.deleteById(id);
        log.info("User {} requested to delete the supplier with id: {}. SUPPLIER DELETED.", user.getUsername(), id);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
