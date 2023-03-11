package fi.haagahelia.stockmanager.controller.product.brand;

import fi.haagahelia.stockmanager.dto.common.ErrorResponse;
import fi.haagahelia.stockmanager.dto.product.brand.BrandCuDTO;
import fi.haagahelia.stockmanager.dto.product.brand.BrandDTO;
import fi.haagahelia.stockmanager.model.product.brand.Brand;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.product.BrandRepository;
import fi.haagahelia.stockmanager.repository.product.ProductRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
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
@RequestMapping("/api/brands")
public class BrandController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final BrandRepository bRepository;
    private final ProductRepository pRepository;

    @Autowired
    public BrandController(BrandRepository bRepository, ProductRepository pRepository) {
        this.bRepository = bRepository;
        this.pRepository = pRepository;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    /**
     * This method is used to create and add the HATEOAS links to the BrandDTO model.
     * @param brandDTO The DTO model to which we will add the HATEOAS links.
     * @return The DTO model with the HATEOAS links.
     */
    private BrandDTO createHATEOAS(BrandDTO brandDTO) {
        Link selfRel = linkTo(BrandController.class).slash(String.valueOf(brandDTO.getId())).withSelfRel();
        Link collection = linkTo(BrandController.class).slash("").withRel("brands");
        brandDTO.add(selfRel, collection);
        return brandDTO;
    }

    /**
     * This function is used to validate a brand object.
     * We check the following conditions:
     *      - Name is not null
     *      - Name is not empty
     *      - A brand does not exist with the same name
     * @param brandCuDTO Corresponds to the BrandCuDTO that we validate
     * @return A Pair object that contains an HttpStatus and the decision reason.
     */
    private Pair<HttpStatus, String> validateBrand(BrandCuDTO brandCuDTO) {
        if (brandCuDTO.getName() == null) return Pair.of(HttpStatus.BAD_REQUEST, "NULL_BRAND_NAME");
        if (brandCuDTO.getName().length() < 1) return Pair.of(HttpStatus.BAD_REQUEST, "EMPTY_BRAND_NAME");
        if (bRepository.existsByName(brandCuDTO.getName())) return Pair.of(HttpStatus.CONFLICT, "BRAND_ALREADY_EXISTS");
        return Pair.of(HttpStatus.ACCEPTED, "");
    }

    /* ------------------------------------------------- API METHODS ------------------------------------------------ */


    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * Retrieves a page of BrandDTO objects that match the given search query, sorted by brand name in ascending order.
     *
     * @param user authenticated Employee object
     * @param searchQuery the search query, which can be null or an empty string
     * @param pageable pagination information (page number, size, and sorting)
     * @param sort sorting information for the query
     * @return a ResponseEntity containing a page model of BrandDTO objects.
     *      --> HttpStatus.OK if the list is non-empty.
     *      --> HttpStatus.NO_CONTENT if it is empty.
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs.
     */
    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<?> getAllBrands(@AuthenticationPrincipal Employee user,
                                            @RequestParam(required = false) String searchQuery,
                                            @PageableDefault(size = 10) Pageable pageable,
                                            @SortDefault.SortDefaults({
                                            @SortDefault(sort = "name", direction = Sort.Direction.ASC)}) Sort sort) {
        try {
            log.info("User {} is requesting all the brands.", user.getUsername());
            Specification<Brand> spec = null;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                spec = (root, query, cb) -> cb.like(
                        cb.lower(root.get("name")),
                        "%" + searchQuery.toLowerCase() + "%"
                );
            }
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            Page<Brand> brands = bRepository.findAll(spec, pageable);
            if (brands.getTotalElements() < 1) {
                log.info("User {} requested all the brands. NO DATA FOUND", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_DATA_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            List<BrandDTO> brandDTOS = new ArrayList<>();
            for (Brand brand : brands.getContent()) {
                BrandDTO brandDTO = BrandDTO.convert(brand);
                brandDTOS.add(createHATEOAS(brandDTO));
            }
            PagedModel<BrandDTO> brandDTOPage = PagedModel.of(brandDTOS,
                    new PagedModel.PageMetadata(brands.getSize(), brands.getNumber(), brands.getTotalElements()));
            brandDTOPage.add(linkTo(BrandController.class).withRel("brands"));
            return new ResponseEntity<>(brandDTOPage, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested all the brands. UNEXPECTED ERROR!", user.getUsername());
            e.printStackTrace();
            System.out.println(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to retrieves a BrandDTO object that match the given id.
     * Firstly, we search that a Brand corresponds to the given id.
     * Secondly, we convert the Brand object to a BrandDTO and we add the HATEOAS links.
     * Finally, we return the BrandDTO to the user.
     *
     * @param id Corresponds to the id of the brand that the user wants to access
     * @param user Corresponds to the user that is authenticated
     * @return a ResponseEntity containing a BrandDTO object or an ErrorMessage.
     *      --> HttpStatus.OK if the BrandDTO exists.
     *      --> HttpStatus.NO_CONTENT if there is no brand.
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs.
     */
    @GetMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<?> getBrandById(@PathVariable(name = "id") Long id,
                                                               @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting the brand with id {}", user.getUsername(), id);
            Optional<Brand> brandOptional = bRepository.findById(id);
            if (!brandOptional.isPresent()) {
                log.info("User {} requested the brand with id {}. NO DATA FOUND", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_DATA_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            BrandDTO brandDTO = BrandDTO.convert(brandOptional.get());
            createHATEOAS(brandDTO);
            log.info("User {} requested the brand with id {}. RETURNING DATA", user.getUsername(), id);
            return new ResponseEntity<>(brandDTO, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested the brand with id {}. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to create and save a new Brand.
     * Validates the BrandCuDTO to ensure that the data is valid before saving it to the database.
     * If the BrandCuDTO is invalid, returns an appropriate HTTP status code with an error message.
     * After saving the Brand to the database, converts the saved Brand to a BrandDTO and adds HATEOAS links to it.
     * Returns a ResponseEntity containing the BrandDTO with HTTP status code CREATED if the Brand has been successfully created and saved.
     * Returns a ResponseEntity with HTTP status code INTERNAL_SERVER_ERROR if an unexpected error occurs.
     *
     * @param user Corresponds to the authenticated user.
     * @param brandCuDTO Corresponds to the brand that the user wants to create and save.
     * @return a ResponseEntity containing a BrandDTO object or an ErrorMessage.
     *      --> HttpStatus.CREATED if the BrandDTO has been created.
     *      --> HttpStatus.XX depending on the criteria that has not been validated.
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs.
     */
    @PostMapping(produces = "application/json", consumes = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<?> createNewBrand(@AuthenticationPrincipal Employee user,
                                                                 @RequestBody BrandCuDTO brandCuDTO) {
        try {
            log.info("User {} is requesting to create and save a new brand with the name: '{}'.",
                    user.getUsername(), brandCuDTO.getName());
            Pair<HttpStatus, String> validation = validateBrand(brandCuDTO);
            if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
                log.info("User {} requested to create and save a new brand with the name: '{}'. {}.",
                        user.getUsername(), brandCuDTO.getName(), validation.getSecond());
                ErrorResponse bm = new ErrorResponse(validation.getFirst().getReasonPhrase(), validation.getSecond());
                return new ResponseEntity<>(bm, validation.getFirst());
            }
            Brand brand = new Brand();
            brand.setName(brandCuDTO.getName());
            log.debug("User {} requested to create and save a new brand with the name: '{}'. SAVING THE BRAND",
                    user.getUsername(), brand.getName());
            Brand savedBrand = bRepository.save(brand);
            BrandDTO brandDTO = BrandDTO.convert(savedBrand);
            createHATEOAS(brandDTO);
            log.info("User {} requested to create and save a new brand with the name: '{}'. BRAND CREATED AND SAVED.",
                    user.getUsername(), savedBrand.getName());
            return new ResponseEntity<>(brandDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            log.info("User {} requested to create and save a new brand with the name: '{}' UNEXPECTED ERROR!",
                    user.getUsername(), brandCuDTO.getName());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_ADMIN
     * This function is used to delete a saved brand from the database.
     * Firstly, we check that a brand exists in the database with the id given by the user.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Secondly, we verify that no products are related to this brand.
     *      If a product is related to this brand, it's not possible to delete the brand.
     *      We return an HttpStatus.CONFLICT to the user.
     * Thirdly, we can delete the brand from the database.
     * Finally, we return to the user an HttpStatus.ACCEPTED.
     * @param id Corresponds to the id of the brand that the user wants to delete.
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing an ErrorMessage or nothing.
     *      --> HttpStatus.OK if the Brand has been deleted.
     *      --> HttpStatus.XX if no brand exists or the brand has some relationships.
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs.
     */
    @DeleteMapping(value = ("/{id}"))
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<ErrorResponse> deleteBrand(@PathVariable(name = "id") Long id,
                                                                   @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to delete the brand with id: '{}'", user.getUsername(), id);
            if (!bRepository.existsById(id)) {
                log.info("User {} requested to delete the brand with id : '{}'. NO DATA FOUND", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_DATA_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            if (pRepository.existsByBrandId(id)) {
                log.info("User {} requested to delete the brand with id : '{}'. PRODUCTS RELATED TO THIS BRAND.",
                        user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.CONFLICT.getReasonPhrase(), "BRAND_HAS_RELATIONSHIPS");
                return new ResponseEntity<>(bm, HttpStatus.CONFLICT);
            }
            log.debug("User {} requested to delete the brand with id: '{}'. DELETING BRAND", user.getUsername(), id);
            bRepository.deleteById(id);
            log.info("User {} requested to delete the brand with id: '{}'. BRAND DELETED", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to delete the brand with id: '{}'. UNEXPECTED ERROR!",
                    user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
