package fi.haagahelia.stockmanager.controller.product.category;


import fi.haagahelia.stockmanager.dto.product.category.CategoryCuDTO;
import fi.haagahelia.stockmanager.dto.product.category.CategoryDTO;
import fi.haagahelia.stockmanager.model.product.category.Category;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.product.CategoryRepository;
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
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final CategoryRepository cRepository;
    private final ProductRepository pRepository;

    @Autowired
    public CategoryController(CategoryRepository cRepository, ProductRepository pRepository) {
        this.cRepository = cRepository;
        this.pRepository = pRepository;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    /**
     * This method is used to create and add the HATEOAS links to the CategoryDTO model.
     * @param cat The DTO model to which we will add the HATEOAS links.
     * @return The DTO model with the HATEOAS links.
     */
    private CategoryDTO createHATEOAS(CategoryDTO cat) {
        Link selfLink = linkTo(CategoryController.class).slash(String.valueOf(cat.getId())).withSelfRel();
        Link collectionlink = linkTo(methodOn(CategoryController.class)).slash("").withRel("categories");
        cat.add(selfLink, collectionlink);
        return cat;
    }

    /**
     * This function is used to validate a CategoryCuDTO when a new one is going to be created.
     * We check the following conditions:
     *      - Name must not be null
     *      - Name must not be empty
     *      - Name must not already exist in the database
     * @param categoryCuDTO Corresponds to the object to validate
     * @return A Pair object that contains an HttpStatus and the decision reason.
     */
    private Pair<HttpStatus, String > validateCategory(CategoryCuDTO categoryCuDTO) {
        if (categoryCuDTO.getName() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "NAME IS NULL.");
        if (categoryCuDTO.getName().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "NAME IS EMPTY.");
        if (cRepository.existsByName(categoryCuDTO.getName())) return Pair.of(HttpStatus.CONFLICT, "ALREADY EXISTS.");
        return Pair.of(HttpStatus.ACCEPTED, "");
    }

    /* ------------------------------------------------- API METHODS ------------------------------------------------ */

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to find all the categories that are in the database.
     * Firstly, we do a query to the database to find all the categories using the category repository.
     * Secondly, we check that the list that was returned by the category repository contains at least one category.
     *      If not, we return an HttpStatu.NO_CONTENT to the user.
     * Thirdly, we create a new list of CategoryDTO.
     * We iterate over all the objects from the database list to convert each of them into a CategoryDTO object.
     * We also add the HATEOAS links to the CategoryDTO by using the createHATEOAS function.
     * Each object that has been converted is added to the list of CategoryDTO.
     * Finally, we return to the user the list of CategoryDTO with an HttpStatus.OK.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<List<CategoryDTO>> getALlCategories(@AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting all the categories from the database.", user.getUsername());
        List<Category> categories = cRepository.findAll();
        if (categories.size() < 1) {
            log.info("User {} requested all the categories from the database. NO DATA FOUND.", user.getUsername());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<CategoryDTO> categoryDTOS = new ArrayList<>();
        for (Category category : categories) {
            CategoryDTO categoryDTO = CategoryDTO.convert(category);
            categoryDTOS.add(createHATEOAS(categoryDTO));
        }
        log.info("User {} requested all the categories from the database. RETURNING DATA", user.getUsername());
        return new ResponseEntity<>(categoryDTOS, HttpStatus.OK);
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to find a specific category by the id given by the user.
     * Firstly, we do a query to the database to select the category that corresponds to the id using the category repo.
     * Secondly, we check that the Optional that was returned by the category repository is empty.
     *      If it is empty, we return an HttpStatu.NO_CONTENT to the user.
     * Thirdly, we convert the category as a CategoryDTO by getting the category that is inside the Optional.
     * We also add the HATEOAS links to the CategoryDTO object.
     * Finally, we can return the object to the user with an HttpStatus.OK.
     * @param id Corresponds to the id of the category that the user wants to access.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @GetMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<CategoryDTO> getCategoryndById(@PathVariable(value = "id") Long id,
                                                                  @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting the category with id: '{}'", user.getUsername(), id);
        Optional<Category> categoryOptional = cRepository.findById(id);
        if (categoryOptional.isEmpty()) {
            log.info("User {} requested the category with id: '{}'. NO DATA FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        CategoryDTO categoryDTO = CategoryDTO.convert(categoryOptional.get());
        createHATEOAS(categoryDTO);
        log.info("User {} requested the category with the id: '{}'. RETURNING DATA.", user.getUsername(), id);
        return new ResponseEntity<>(categoryDTO, HttpStatus.OK);
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to create and save a new category.
     * Firstly, we validate the CategoryCuDTO, to ensure that invalid data is not saved in the database.
     * To validate the CategoryCuDTO, we use the validateCategory function.
     *      If the object is invalid, we return an HttpStatus that corresponds to the reason of the invalidation.
     * Secondly, we create a category object, and we transfer data from the CategoryCuDTO to the Category object.
     * Thirdly, we save the object in the database. Then, we convert the saved category to a CategoryDTO.
     * Finally, we add the HATEOAS links to the CategoryDTO and we return it with an HttpStatus.CREATED to the user.
     * @param categoryCuDTO Corresponds to the category that the user wants to create and save.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<CategoryDTO> createNewCategory(@RequestBody CategoryCuDTO categoryCuDTO,
                                                                       @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to create and save a new category with the name: '{}'.",
                user.getUsername(), categoryCuDTO.getName());
        Pair<HttpStatus, String> validation = validateCategory(categoryCuDTO);
        if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
            log.info("User {} requested to create and save a new category with the name: '{}'. {}",
                    user.getUsername(), categoryCuDTO.getName(), validation.getSecond());
            return new ResponseEntity<>(validation.getFirst());
        }
        Category category = new Category();
        category.setName(categoryCuDTO.getName());
        category.setDescription(categoryCuDTO.getDescription());
        log.debug("User {} requested to create and save a category with the name: '{}'. SAVING CATEGORY.",
                user.getUsername(), category.getName());
        Category savedCategory = cRepository.save(category);

        log.info("User {} requested to create and save a new category with the name: '{}'. CATEGORY CREATED AND SAVED.",
                user.getUsername(), savedCategory.getName());
        CategoryDTO categoryDTO = CategoryDTO.convert(savedCategory);

        return new ResponseEntity<>(createHATEOAS(categoryDTO), HttpStatus.CREATED);
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to update an existing category by its id.
     * Firstly, we search the category in the database.
     * Secondly, we check that the Optional returned by the database contains a category.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Thirdly, we get the category that is in the optional, and we store it as a category object.
     * Fourthly, we can update the description of the category.
     * Fifthly, we save the modification to the database.
     * Finally, we convert the updated category as a CategoryDTO and we add the HATEOAS links using createHATEOAS function.
     * We return the data to the user with an HttpStatus.ACCEPTED.
     * @param id Corresponds to the category's id in the database that we want to update.
     * @param categoryCuDTO Corresponds to the user's new data.
     * @param user Corresponds to the authenticated user.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<CategoryDTO> updateCategory(@PathVariable(value = "id") Long id,
                                                                    @RequestBody CategoryCuDTO categoryCuDTO,
                                                                    @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to update the category with the id: '{}'.", user.getUsername(), id);
        Optional<Category> categoryOptional = cRepository.findById(id);
        if (categoryOptional.isEmpty()) {
            log.info("User {} requested to update the category with the id: '{}'. NO DATA FOUND",
                    user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Category category = categoryOptional.get();
        category.setDescription(categoryCuDTO.getDescription());
        log.debug("User {} requested to update the category with the id: '{}'. SAVING THE CATEGORY.", user.getUsername(), id);
        Category updatedCategory = cRepository.save(category);
        log.info("User {} requested to update the category with the id: '{}'. CATEGORY UPDATE", user.getUsername(), id);
        CategoryDTO categoryDTO = CategoryDTO.convert(updatedCategory);
        return new ResponseEntity<>(createHATEOAS(categoryDTO), HttpStatus.ACCEPTED);
    }

    /**
     * AVAILABLE FOR: ROLE_ADMIN
     * This function is used to delete a category by its id.
     * Firstly, we check that a category exists with the id given by the user.
     *      If no category exists with this id, we return to the user an HttpStatus.NO_CONTENT.
     * Secondly, we check that no products are related to this category.
     *      If a product is related, it is impossible to delete, we return an HttpStatus.NOT_ACCEPTABLE to the user.
     * Thirdly, we can delete the object from the database.
     * Finally, we return an HttpStatus.ACCEPTED when the object is deleted.
     * @param id Corresponds to the category's id in the database that we want to delete.
     * @param user Corresponds to the authenticated user.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<CategoryDTO> deleteCategoryById(@PathVariable(value = "id") Long id,
                                                                        @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to delete the category with id: '{}'", user.getUsername(), id);
        if (!cRepository.existsById(id)) {
            log.info("User {} requested to delete the category with id: '{}'. NO DATA FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        if (pRepository.existsByCategoryId(id)) {
            log.info("User {} requested to delete the category with id : '{}'. PRODUCTS RELATED TO THIS CATEGORY.",
                    user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        log.debug("User {} requested to delete the category with id: '{}'. DELETING DATA.", user.getUsername(), id);
        cRepository.deleteById(id);
        log.info("User {} requested to delete the category with id: '{}'. CATEGORY DELETED", user.getUsername(), id);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
