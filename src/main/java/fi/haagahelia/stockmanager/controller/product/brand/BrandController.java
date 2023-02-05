package fi.haagahelia.stockmanager.controller.product.brand;

import fi.haagahelia.stockmanager.dto.product.brand.BrandCuDTO;
import fi.haagahelia.stockmanager.dto.product.brand.BrandDTO;
import fi.haagahelia.stockmanager.model.product.brand.Brand;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.product.BrandRepository;
import fi.haagahelia.stockmanager.repository.product.ProductRepository;
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
        if (brandCuDTO.getName() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "BRAND NAME IS NULL");
        if (brandCuDTO.getName().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "BRAND NAME IS EMPTY");
        if (bRepository.existsByName(brandCuDTO.getName())) return Pair.of(HttpStatus.CONFLICT, "BRAND ALREADY EXISTS");
        return Pair.of(HttpStatus.ACCEPTED, "");
    }

    /* ------------------------------------------------- API METHODS ------------------------------------------------ */

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to find all the brands that are in the database.
     * Firstly, we do a query to the database to select all the brands using the brand repository.
     * Secondly, we check that the list that was returned by the brand repository contains at least one brand.
     *      If not, we return an HttpStatu.NO_CONTENT to the user.
     * Thirdly, we create a new list of BrandDTO.
     * We iterate over all the objects from the database list to convert each of them into a BrandDTO object.
     * We also add the HATEOAS links to the BrandDTO by using the createHATEOAS function.
     * Each object that has been converted is added to the list of BrandDTO.
     * Finally, we return to the user the list of BrandDTO with an HttpStatus.OK.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<List<BrandDTO>> getAllBrands(@AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting all the brands.", user.getUsername());
        List<Brand> brands = bRepository.findAll();
        if (brands.size() < 1) {
            log.info("User {} requested all the brands. NO DATA FOUND", user.getUsername());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<BrandDTO> brandDTOS = new ArrayList<>();
        for (Brand brand : brands) {
            BrandDTO brandDTO = BrandDTO.convert(brand);
            brandDTOS.add(createHATEOAS(brandDTO));
        }
        log.info("User {} requested all the brands. RETURNING DATA", user.getUsername());
        return new ResponseEntity<>(brandDTOS, HttpStatus.OK);
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to find a specific brand by the id given by the user.
     * Firstly, we do a query to the database to select the brand that corresponds to the id using the brand repository.
     * Secondly, we check that the Optional that was returned by the brand repository is empty.
     *      If it is empty, we return an HttpStatu.NO_CONTENT to the user.
     * Thirdly, we convert the brand as a brandDTO by getting the brand that is inside the Optional.
     * We also add the HATEOAS links to the brandDTO object.
     * Finally, we can return the object to the user with an HttpStatus.OK.
     * @param id Corresponds to the id of the brand that the user wants to access.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @GetMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<BrandDTO> getBrandById(@PathVariable(name = "id") Long id,
                                                               @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting the brand with id {}", user.getUsername(), id);
        Optional<Brand> brandOptional = bRepository.findById(id);
        if (brandOptional.isEmpty()) {
            log.info("User {} requested the brand with id {}. NO DATA FOUND", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        BrandDTO brandDTO = BrandDTO.convert(brandOptional.get());
        createHATEOAS(brandDTO);
        log.info("User {} requested the brand with id {}. RETURNING DATA", user.getUsername(), id);
        return new ResponseEntity<>(brandDTO, HttpStatus.OK);
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to create and save a new band.
     * Firstly, we validate the brandCuDTO, to ensure that invalid data is not saved in the database.
     * To validate the brandCuDTO, we use the validateBrand function.
     *      If the object is invalid, we return an HttpStatus that corresponds to the reason of the invalidation.
     * Secondly, we create a Brand object, and we transfer data from the BrandCuDTO to the Brand object.
     * Thirdly, we save the object in the database. Then, we convert the saved Brand to a BrandDTO.
     * Finally, we add the HATEOAS links to the BrandDTO and we return it with an HttpStatus.CREATED to the user.
     * @param user Corresponds to the user that is authenticated.
     * @param brandCuDTO Corresponds to the brand that the user wants to create and save.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @PostMapping(produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<BrandDTO> createNewBrand(@AuthenticationPrincipal Employee user,
                                                                 @RequestBody BrandCuDTO brandCuDTO) {
        log.info("User {} is requesting to create and save a new brand with the name: '{}'.",
                user.getUsername(), brandCuDTO.getName());
        Pair<HttpStatus, String> validation = validateBrand(brandCuDTO);
        if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
            log.info("User {} requested to create and save a new brand with the name: '{}'. {}.",
                    user.getUsername(), brandCuDTO.getName(), validation.getSecond());
            return new ResponseEntity<>(validation.getFirst());
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
    }

    /**
     * AVAILABLE FOR: ROLE_ADMIN
     * This function is used to delete a saved brand from the database.
     * Firstly, we check that a brand exists in the database with the id given by the user.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Secondly, we verify that no products are related to this brand.
     *      If a product is related to this brand, it's not possible to delete the brand.
     *      We return an HttpStatus.NOT_ACCEPTABLE to the user.
     * Thirdly, we can delete the brand from the database.
     * Finally, we return an HttpStatus.ACCEPTED to the user.
     * @param id Corresponds to the id of the brand that the user wants to delete.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus.
     */
    @DeleteMapping(value = ("/{id}"), produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<BrandDTO> deleteBrand(@PathVariable(name = "id") Long id,
                                                              @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to delete the brand with id: '{}'", user.getUsername(), id);
        if (!bRepository.existsById(id)) {
            log.info("User {} requested to delete the brand with id : '{}'. NO DATA FOUND", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        if (pRepository.existsByBrandId(id)) {
            log.info("User {} requested to delete the brand with id : '{}'. PRODUCTS RELATED TO THIS BRAND.",
                    user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        log.debug("User {} requested to delete the brand with id: '{}'. DELETING BRAND", user.getUsername(), id);
        bRepository.deleteById(id);
        log.info("User {} requested to delete the brand with id: '{}'. BRAND DELETED", user.getUsername(), id);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
