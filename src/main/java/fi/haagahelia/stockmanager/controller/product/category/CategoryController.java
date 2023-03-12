package fi.haagahelia.stockmanager.controller.product.category;


import fi.haagahelia.stockmanager.dto.common.ErrorResponse;
import fi.haagahelia.stockmanager.dto.product.category.CategoryCuDTO;
import fi.haagahelia.stockmanager.dto.product.category.CategoryDTO;
import fi.haagahelia.stockmanager.model.product.category.Category;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.product.CategoryRepository;
import fi.haagahelia.stockmanager.repository.product.ProductRepository;
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
        Link collectionlink = linkTo(CategoryController.class).withRel("categories");
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
        if (categoryCuDTO.getName() == null) return Pair.of(HttpStatus.BAD_REQUEST, "NULL_CATEGORY_NAME.");
        if (categoryCuDTO.getName().length() < 1) return Pair.of(HttpStatus.BAD_REQUEST, "EMPTY_CATEGORY_NAME.");
        if (cRepository.existsByName(categoryCuDTO.getName())) return Pair.of(HttpStatus.CONFLICT, "CATEGORY_ALREADY_EXISTS.");
        return Pair.of(HttpStatus.ACCEPTED, "");
    }

    /* ------------------------------------------------- API METHODS ------------------------------------------------ */

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to find all the categories.
     * Firstly, database query through the category repository.
     * Secondly, verification of the returned page is not empty.
     *      --> If is empty: returns an HttpStatus.NO_CONTENT to the user.
     * Thirdly, converting each Category object into a CategoryDTO one and adding HATEOAS links.
     * Fourthly, updating the PageModel of CategoryDTO.
     * Finally, return the PageModel of CategoryDTO object with HttpStatus.OK.
     *
     * @param user authenticated Employee object
     * @param searchQuery the search query, which can be null or an empty string
     * @param pageable pagination information (page number, size, and sorting)
     * @param sort sorting information for the query
     * @return a ResponseEntity containing a page model of CategoryDTO objects or a Error Message.
     *      --> HttpStatus.OK if at least one category has been found. (Page of CategoryDTO)
     *      --> HttpStatus.NO_CONTENT if no category exists. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_VENDOR', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<?> getALlCategories(@AuthenticationPrincipal Employee user,
                                                            @RequestParam(required = false) String searchQuery,
                                                            @PageableDefault(size = 10) Pageable pageable,
                                                            @SortDefault.SortDefaults({
                                                                    @SortDefault(sort = "name", direction = Sort.Direction.ASC)}) Sort sort) {
        try {
            log.info("User {} is requesting all the categories.", user.getUsername());
            Specification<Category> spec = null;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                spec = (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + searchQuery.toLowerCase() + "%");
            }
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            Page<Category> categories = cRepository.findAll(spec, pageable);
            if (categories.getTotalElements() < 1) {
                log.info("User {} requested all the categories. NO DATA FOUND", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_CATEGORY_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            List<CategoryDTO> categoryDTOS = new ArrayList<>();
            for (Category category : categories.getContent()) {
                CategoryDTO categoryDTO = CategoryDTO.convert(category);
                categoryDTOS.add(createHATEOAS(categoryDTO));
            }
            PagedModel.PageMetadata pmd = new PagedModel.PageMetadata(categories.getSize(), categories.getNumber(), categories.getTotalElements());
            PagedModel<CategoryDTO> categoryDTOPage = PagedModel.of(categoryDTOS, pmd);
            categoryDTOPage.add(linkTo(CategoryController.class).withRel("categories"));
            return new ResponseEntity<>(categoryDTOPage, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested all the categories. UNEXPECTED ERROR!", user.getUsername());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to find the category that matches the id provided by the user.
     * Firstly, database query through the category repository.
     * Secondly, verification of the returned Optional object.
     *      --> If is not present: returns an HttpStatus.BAD_REQUEST to the user.
     * Thirdly, Category object conversion into CategoryDTO and adding HATEOAS links.
     * Finally, return the CategoryDTO object with HttpStatus.OK.
     *
     * @param id Corresponds to the id of the category that the user wants to access.
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing a CategoryDTO objects or a Error Message.
     *      --> HttpStatus.OK if the category exists. (CategoryDTO)
     *      --> HttpStatus.BAD_REQUEST if no category corresponds to the id. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_VENDOR', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<?> getCategoryndById(@PathVariable(value = "id") Long id,
                                                             @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting the category with id: '{}'", user.getUsername(), id);
            Optional<Category> categoryOptional = cRepository.findById(id);
            if (!categoryOptional.isPresent()) {
                log.info("User {} requested the category with id: '{}'. NO DATA FOUND.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_CATEGORY_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            CategoryDTO categoryDTO = CategoryDTO.convert(categoryOptional.get());
            createHATEOAS(categoryDTO);
            log.info("User {} requested the category with the id: '{}'. RETURNING DATA.", user.getUsername(), id);
            return new ResponseEntity<>(categoryDTO, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested the category with the id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to create and save a new category.
     * Firstly, validation of the CategoryCuDTO object provided by the user, using the validateCategory function.
     *      --> If the object is invalid, we return an HttpStatus that corresponds to the reason of the invalidation.
     * Secondly, create and set up Category object. Saving the created Category in the database.
     * Thirdly, convert the returned Category as a CategoryDTO. Adding HATEOAS links.
     * Finally, returning the CategoryDTO to the user with an HttpStatus.CREATED.
     *
     * @param categoryCuDTO Corresponds to the category that the user wants to create and save.
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing a CategoryDTO objects or a Error Message.
     *      --> HttpStatus.CREATED if the category has been created. (CategoryDTO)
     *      --> HttpStatus.XX if a criteria has not been validated. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<?> createCategory(@RequestBody CategoryCuDTO categoryCuDTO,
                                                          @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to create and save a new category with the name: '{}'.",
                    user.getUsername(), categoryCuDTO.getName());
            Pair<HttpStatus, String> validation = validateCategory(categoryCuDTO);
            if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
                log.info("User {} requested to create and save a new category with the name: '{}'. {}",
                        user.getUsername(), categoryCuDTO.getName(), validation.getSecond());
                ErrorResponse bm = new ErrorResponse(validation.getFirst().getReasonPhrase(), validation.getSecond());
                return new ResponseEntity<>(bm, validation.getFirst());
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
        } catch (Exception e) {
            log.info("User {} requested to create and save a new category. UNEXPECTED ERROR!", user.getUsername());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to update an existing category by its id.
     * Firstly, database query through the category repository to find the category.
     * Secondly, verification of the returned Optional object.
     *      --> If is not present: returns an HttpStatus.BAD_REQUEST to the user.
     * Thirdly, updating the category description and saving the updated category.
     * Fourthly, convert the Category object into a CategoryDTO, and adding the HATEOAS LINKS.
     * Finally, return the CategoryDTO with an HttpStatus.OK.
     *
     * @param id Corresponds to the category's id in the database that we want to update.
     * @param categoryCuDTO Corresponds to the user's new data.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing a CategoryDTO objects or an Error Message.
     *      --> HttpStatus.OK if the category has been updated. (CategoryDTO)
     *      --> HttpStatus.BAD_REQUEST if no category corresponds to the given id. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<?> updateCategory(@PathVariable(value = "id") Long id,
                                                          @RequestBody CategoryCuDTO categoryCuDTO,
                                                          @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to update the category with the id: '{}'.", user.getUsername(), id);
            Optional<Category> categoryOptional = cRepository.findById(id);
            if (!categoryOptional.isPresent()) {
                log.info("User {} requested to update the category with the id: '{}'. NO DATA FOUND", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_CATEGORY_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            Category category = categoryOptional.get();
            category.setDescription(categoryCuDTO.getDescription());
            log.debug("User {} requested to update the category with the id: '{}'. SAVING THE CATEGORY.", user.getUsername(), id);
            Category updatedCategory = cRepository.save(category);
            log.info("User {} requested to update the category with the id: '{}'. CATEGORY UPDATE", user.getUsername(), id);
            CategoryDTO categoryDTO = CategoryDTO.convert(updatedCategory);
            return new ResponseEntity<>(createHATEOAS(categoryDTO), HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to update the category with the id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_ADMIN
     * This function is used to delete a category by its id.
     * Firstly, check that a category corresponds to the given id.
     *      --> If not, return an HttpStatus.BAD_REQUEST.
     * Secondly, check that no product are related to the category.
     *      --> If yes, return an HttpStatus.CONFLICT.
     * Finally, deletion of the category using the category repository, and returning an HttpStatus.NO_CONTENT.
     *
     * @param id Corresponds to the category's id in the database that we want to delete.
     * @param user Corresponds to the authenticated user.
     * @return a ResponseEntity containing an Error Message.
     *      --> HttpStatus.OK if the category has been deleted.
     *      --> HttpStatus.BAD_REQUEST if no category corresponds to the given id.
     *      --> HttpStatus.CONFLICT if the category has at least one relationship.
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs.
     */
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<ErrorResponse> deleteCategory(@PathVariable(value = "id") Long id,
                                                                      @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to delete the category with id: '{}'", user.getUsername(), id);
            if (!cRepository.existsById(id)) {
                log.info("User {} requested to delete the category with id: '{}'. NO DATA FOUND.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_CATEGORY_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            if (pRepository.existsByCategoryId(id)) {
                log.info("User {} requested to delete the category with id : '{}'. PRODUCTS RELATED TO THIS CATEGORY.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.CONFLICT.getReasonPhrase(), "CATEGORY_HAS_RELATIONSHIPS");
                return new ResponseEntity<>(bm, HttpStatus.CONFLICT);
            }
            log.debug("User {} requested to delete the category with id: '{}'. DELETING DATA.", user.getUsername(), id);
            cRepository.deleteById(id);
            log.info("User {} requested to delete the category with id: '{}'. CATEGORY DELETED", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to delete the category with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}
