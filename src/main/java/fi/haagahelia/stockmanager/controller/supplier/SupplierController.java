package fi.haagahelia.stockmanager.controller.supplier;


import fi.haagahelia.stockmanager.controller.common.GeolocationController;
import fi.haagahelia.stockmanager.dto.common.ErrorResponse;
import fi.haagahelia.stockmanager.dto.supplier.SupplierCuDTO;
import fi.haagahelia.stockmanager.dto.supplier.SupplierDTO;
import fi.haagahelia.stockmanager.model.common.Geolocation;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.common.GeolocationRepository;
import fi.haagahelia.stockmanager.repository.product.ProductRepository;
import fi.haagahelia.stockmanager.repository.supplier.SupplierRepository;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final SupplierRepository sRepository;
    private final GeolocationRepository gRepository;
    private final ProductRepository pRepository;

    @Autowired
    public SupplierController(SupplierRepository sRepository, GeolocationRepository gRepository, ProductRepository pRepository) {
        this.sRepository = sRepository;
        this.gRepository = gRepository;
        this.pRepository = pRepository;
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
     * @param isForUpdate Used to know if we check value for an update or a creation.
     * @return A Pair object that contains an HttpStatus and the decision reason.
     */
    private Pair<HttpStatus, String> validateSupplier(SupplierCuDTO supplierCuDTO, boolean isForUpdate) {
        if (supplierCuDTO.getName() == null) return Pair.of(HttpStatus.BAD_REQUEST, "SUPPLIER_NAME_NULL");
        if (supplierCuDTO.getName().length() < 1) return Pair.of(HttpStatus.BAD_REQUEST, "SUPPLIER_NAME_EMPTY");
        if (supplierCuDTO.getGeolocationId() != null) {
            if (!gRepository.existsById(supplierCuDTO.getGeolocationId())) {
                return Pair.of(HttpStatus.NOT_FOUND, "SUPPLIER_GEOLOCATION_NOT_FOUND");
            }
        }
        if (isForUpdate) {
            if (!sRepository.existsByName(supplierCuDTO.getName())) return Pair.of(HttpStatus.BAD_REQUEST, "SUPPLIER_NOT_FOUND");
        } else {
            if (sRepository.existsByName(supplierCuDTO.getName())) return Pair.of(HttpStatus.CONFLICT, "SUPPLIER_ALREADY_EXISTS");
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
     * This function is used to find all the suppliers.
     * Firstly, database query through the supplier repository.
     * Secondly, verification of the returned page is not empty.
     *      --> If is empty: returns an HttpStatus.NO_CONTENT to the user.
     * Thirdly, converting each Supplier object into a SupplierDTO one and adding HATEOAS links.
     * Fourthly, updating the PageModel of SupplierDTO.
     * Finally, return the PageModel of Supplier object with HttpStatus.OK.
     *
     * @param user authenticated Employee object
     * @param searchQuery the search query, which can be null or an empty string
     * @param pageable pagination information (page number, size, and sorting)
     * @param sort sorting information for the query
     * @return a ResponseEntity containing a page model of SupplierDTO objects or a Error Message.
     *      --> HttpStatus.OK if at least one supplier has been found. (Page of SupplierDTO)
     *      --> HttpStatus.NO_CONTENT if no supplier exists. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<?> getSuppliers(@AuthenticationPrincipal Employee user,
                                                        @RequestParam(required = false) String searchQuery,
                                                        @PageableDefault(size = 10) Pageable pageable,
                                                        @SortDefault.SortDefaults({
                                                                @SortDefault(sort = "name", direction = Sort.Direction.ASC)}) Sort sort) {
        try {
            log.info("User {} is requesting all the suppliers from the database.", user.getUsername());
            Specification<Supplier> spec = null;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                spec = (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + searchQuery.toLowerCase() + "%");
            }
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            Page<Supplier> suppliers = sRepository.findAll(spec, pageable);
            if (suppliers.getSize() < 1) {
                log.info("User {} requested all the suppliers. NO DATA FOUND.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_SUPPLIER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            List<SupplierDTO> supplierDTOS = new ArrayList<>();
            for (Supplier supplier : suppliers) {
                SupplierDTO supplierDTO = SupplierDTO.convert(supplier);
                supplierDTOS.add(createHATEOAS(supplierDTO));
            }
            PagedModel.PageMetadata pmd = new PagedModel.PageMetadata(suppliers.getSize(), suppliers.getNumber(), suppliers.getTotalElements());
            PagedModel<SupplierDTO> supplierDTOPage = PagedModel.of(supplierDTOS, pmd);
            supplierDTOPage.add(linkTo(SupplierController.class).withRel("suppliers"));
            log.info("User {} requested all the suppliers. RETURNING DATA.", user.getUsername());
            return new ResponseEntity<>(supplierDTOPage, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested all the suppliers. UNEXPECTED ERROR!", user.getUsername());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Available for: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get the supplier that correspond to a given id.
     * Firstly, we will SELECT the data in the database.
     * Secondly, we verify that data is not empty. If data is empty, we return a HttpStatus.NO_CONTENT.
     * Thirdly we transform the supplier as a SupplierDTO object, and we add the HATEOAS links to the object.
     * Finally, if everything went OK, we return the data to the user with a HttpStatus.OK.
     *
     * @param id Correspond to the id of the supplier searched by the user.
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing a SupplierDTO objects or a Error Message.
     *      --> HttpStatus.OK if the supplier exists. (SupplierDTO)
     *      --> HttpStatus.BAD_REQUEST if no supplier corresponds to the id. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<?> getSupplierByID(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting the supplier with id: '{}'.", user.getUsername(), id);
            Optional<Supplier> supplierOptional = sRepository.findById(id);
            if (!supplierOptional.isPresent()) {
                log.info("User {} requested the supplier with id: '{}'. NO DATA FOUND.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_SUPPLIER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            SupplierDTO supplierDTO = SupplierDTO.convert(supplierOptional.get());
            createHATEOAS(supplierDTO);
            log.info("User {} requested the supplier with id: '{}'. RETURNING DATA.", user.getUsername(), id);
            return new ResponseEntity<>(supplierDTO, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested the supplier with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Available for: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to create and save a new supplier.
     * Firstly, we get the new supplier in the RequestBody.
     * Secondly, we check that the information given by the user are correct, using the validateSupplier function.
     *      If not, we return an HttpStatus with the corresponding code.
     * Thirdly, we can create the supplier object and save it in the database.
     * Finally, we convert the saved supplier as a SupplierDTO and we return it to the user with an HttpStatus.CREATED.
     *
     * @param supplierCuDTO Corresponds to the new supplier that the user wants to create.
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing a SupplierDTO objects or a Error Message.
     *      --> HttpStatus.CREATED if the supplier has been created. (SupplierDTO)
     *      --> HttpStatus.XX if a criteria has not been validated. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<?> createNewSupplier(@RequestBody SupplierCuDTO supplierCuDTO,  @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to create and save a new supplier with name: '{}'.", user.getUsername(), supplierCuDTO.getName());
            Pair<HttpStatus, String> validation = validateSupplier(supplierCuDTO, false);
            if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
                log.info("User {} requested to create and save a new supplier with the name: '{}'. '{}'",
                        user.getUsername(), supplierCuDTO.getName(), validation.getSecond());
                ErrorResponse bm = new ErrorResponse(validation.getFirst().getReasonPhrase(), validation.getSecond());
                return new ResponseEntity<>(bm, validation.getFirst());
            }
            Supplier supplier = new Supplier();
            supplier.setName(supplierCuDTO.getName());
            supplier.setEmail(supplierCuDTO.getEmail());
            supplier.setPhoneNumber(supplierCuDTO.getPhoneNumber());
            setGeolocationById(supplier, supplierCuDTO.getGeolocationId(), user.getUsername());
            log.debug("User {} requested to create and save a new supplier with the name: '{}'. SAVING SUPPLIER.", user.getUsername(), supplier.getName());
            Supplier savedSupplier = sRepository.save(supplier);
            SupplierDTO supplierDTO = SupplierDTO.convert(savedSupplier);
            createHATEOAS(supplierDTO);
            log.info("User {} requested to create and save a new supplier with the name: '{}'. SUPPLIER CREATED AND SAVED.", user.getUsername(), supplier.getName());
            return new ResponseEntity<>(supplierDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            log.info("User {} requested to create and save a new supplier. UNEXPECTED ERROR!", user.getUsername());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Available for: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to update an existing supplier by his id.
     * Firstly, we get the updated supplierCuDTO in the RequestBody.
     * Secondly, we check that a supplier exists with the given name.
     *      If any supplier correspond to the given id, we return an HttpStatus.NO_CONTENT.
     * Thirdly, we check that the supplierCuDTO0's information are correct.
     *      If it is not valid, we return an HttpStatus with the corresponding code.
     * Fourthly, we can change the value of the supplier.
     * Fifthly, we can save the modification in the database.
     * Finally, we convert the saved supplier as a SupplierDTO, we also add the HATEOAS links, and we return it the
     * user with an HttpStatus.OK.
     *
     * @param id Correspond to the supplier's id in the database that we want to update
     * @param supplierCuDTO Correspond to the user's new data.
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing a SupplierDTO objects or a Error Message.
     *      --> HttpStatus.CREATED if the supplier has been created. (SupplierDTO)
     *      --> HttpStatus.XX if a criteria has not been validated. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<?> updateSupplier(@PathVariable(value = "id") Long id,
                                                          @RequestBody SupplierCuDTO supplierCuDTO,
                                                          @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to update the supplier with name: '{}'.", user.getUsername(), id);
            Optional<Supplier> optionalSupplier = sRepository.findById(id);
            if (!optionalSupplier.isPresent()) {
                log.info("User {} requested to update the supplier with name: '{}'. NO DATA FOUND", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_SUPPLIER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            Pair<HttpStatus, String> validation = validateSupplier(supplierCuDTO, true);
            if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
                log.info("User {} requested to update the supplier with name: '{}'. {}", user.getUsername(), id, validation.getSecond());
                ErrorResponse bm = new ErrorResponse(validation.getFirst().getReasonPhrase(), validation.getSecond());
                return new ResponseEntity<>(bm, validation.getFirst());
            }
            Supplier supplier = optionalSupplier.get();
            supplier.setName(supplierCuDTO.getName());
            supplier.setEmail(supplierCuDTO.getEmail());
            supplier.setPhoneNumber(supplierCuDTO.getPhoneNumber());
            setGeolocationById(supplier, supplierCuDTO.getGeolocationId(), user.getUsername());
            log.debug("User {} requested to update the supplier with the id: '{}'. SAVING SUPPLIER.", user.getUsername(), id);
            Supplier savedSupplier = sRepository.save(supplier);
            SupplierDTO supplierDTO = SupplierDTO.convert(savedSupplier);
            log.info("User {} requested to update the supplier with the id: '{}'. SUPPLIER SAVED.", user.getUsername(), id);
            return new ResponseEntity<>(createHATEOAS(supplierDTO), HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to update the supplier with the name: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


    /**
     * Available for: ROLE_ADMIN
     * This function is used to delete a supplier. /!\ DELETE THE RELATED ORDERS AND ORDER LINES
     * Firstly, we check that a supplier exists with the given id.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Finally, we can delete the supplier from the database, and we return an HttpStatus.OK.
     *
     * @param id Corresponds to the supplier's id that the user wants to delete.
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing an Error Message.
     *      --> HttpStatus.OK if the supplier has been deleted.
     *      --> HttpStatus.BAD_REQUEST if no supplier corresponds to the given id.
     *      --> HttpStatus.CONFLICT if the supplier has at least one relationship.
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs.
     */
    @DeleteMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<ErrorResponse> deleteSupplierById(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to delete the supplier with id: '{}'.", user.getUsername(), id);
            if (!sRepository.existsById(id)) {
                log.info("User {} requested to delete the supplier with id: '{}'. NO DATA FOUND.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_SUPPLIER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            if (pRepository.existsBySupplierId(id)) {
                log.info("User {} requested to delete the supplier with id: '{}'. PRODUCTS RELATED TO THIS SUPPLIER.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.CONFLICT.getReasonPhrase(), "SUPPLIER_HAS_RELATIONSHIPS");
                return new ResponseEntity<>(bm, HttpStatus.CONFLICT);
            }
            log.debug("User {} requested to delete the supplier with id: '{}'. DELETING SUPPLIER.", user.getUsername(), id);
            sRepository.deleteById(id);
            log.info("User {} requested to delete the supplier with id: '{}'. SUPPLIER DELETED.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to delete the supplier with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}
