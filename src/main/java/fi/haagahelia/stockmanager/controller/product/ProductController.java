package fi.haagahelia.stockmanager.controller.product;


import fi.haagahelia.stockmanager.controller.product.brand.BrandController;
import fi.haagahelia.stockmanager.controller.product.category.CategoryController;
import fi.haagahelia.stockmanager.controller.supplier.SupplierController;
import fi.haagahelia.stockmanager.dto.common.ErrorResponse;
import fi.haagahelia.stockmanager.dto.product.ProductCompleteDTO;
import fi.haagahelia.stockmanager.dto.product.ProductCuDTO;
import fi.haagahelia.stockmanager.dto.product.ProductSimpleDTO;
import fi.haagahelia.stockmanager.dto.product.category.ProductDTO;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.product.brand.Brand;
import fi.haagahelia.stockmanager.model.product.category.Category;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.customer.order.CustomerOrderLineRepository;
import fi.haagahelia.stockmanager.repository.product.BrandRepository;
import fi.haagahelia.stockmanager.repository.product.CategoryRepository;
import fi.haagahelia.stockmanager.repository.product.ProductRepository;
import fi.haagahelia.stockmanager.repository.supplier.SupplierRepository;
import fi.haagahelia.stockmanager.repository.supplier.order.SupplierOrderLineRepository;
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
@RequestMapping("/api/products")
public class ProductController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final ProductRepository pRepository;
    private final BrandRepository bRepository;
    private final CategoryRepository cRepository;
    private final SupplierRepository sRepository;
    private final SupplierOrderLineRepository solRepository;
    private final CustomerOrderLineRepository colRepository;

    @Autowired
    public ProductController(ProductRepository pRepository, BrandRepository bRepository, CategoryRepository cRepository,
                             SupplierRepository sRepository, SupplierOrderLineRepository solRepository,
                             CustomerOrderLineRepository colRepository) {
        this.pRepository = pRepository;
        this.bRepository = bRepository;
        this.cRepository = cRepository;
        this.sRepository = sRepository;
        this.solRepository = solRepository;
        this.colRepository = colRepository;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    /**
     * This method is used to create and add the HATEOAS links to a DTO models.
     * @param productDTO The dto model to which we will add the HATEOAS links.
     */
    private void createHATEOAS(ProductDTO productDTO) {
        Link selfLink = linkTo(ProductController.class).slash(String.valueOf(productDTO.getId())).withSelfRel();
        productDTO.add(selfLink);
        Link collectionLink = linkTo(ProductController.class).slash("").withRel("products");
        productDTO.add(collectionLink);

        // Related object link
        if (productDTO.getBrand() != null) {
            Link brandLink = linkTo(BrandController.class)
                    .slash(String.valueOf(productDTO.getBrand().getId())).withRel("brand");
            productDTO.add(brandLink);
        }

        if (productDTO.getCategory() != null) {
            Link categoryLink = linkTo(CategoryController.class)
                    .slash(String.valueOf(productDTO.getCategory().getId())).withRel("category");
            productDTO.add(categoryLink);
        }
        // Supplier link
        if (productDTO.getClass() == ProductCompleteDTO.class) {
            ProductCompleteDTO prodCompDTO = (ProductCompleteDTO) productDTO;
            Link supplierLink = linkTo(SupplierController.class)
                    .slash(String.valueOf(prodCompDTO.getSupplier().getId())).withRel("supplier");
            productDTO.add(supplierLink);
        }
    }

    /**
     * This function is used to create or convert a ProductCuDTO to a product.
     * @param productCuDTO the ProductCuDTO that contains the data
     * @param brand the brand of the product.
     * @param category the category of the product.
     * @param supplier the supplier of the product.
     * @param isUpdate If it is an update, we set the id of the product object.
     */
    private void createProductObj(ProductCuDTO productCuDTO, Product product, Brand brand, Category category,
                                     Supplier supplier, boolean isUpdate) {
        if (isUpdate) product.setId(productCuDTO.getId());
        product.setName(productCuDTO.getName());
        product.setDescription(productCuDTO.getDescription());
        product.setPurchasePrice(productCuDTO.getPurchasePrice());
        product.setSalePrice(productCuDTO.getSalePrice());
        if (!isUpdate) product.setStock(productCuDTO.getStock());
        product.setMinStock(productCuDTO.getMinStock());
        product.setBatchSize(productCuDTO.getBatchSize());
        product.setBrand(brand);
        product.setCategory(category);
        product.setSupplier(supplier);
    }

    /**
     * This function is used to ensure that a ProductCuDTO (given by the user) is valid.
     * It works on several verifications on different fields: name, description, prices, stock, minimumStock, batchSize
     * and the related fields (brand, category, supplier).
     * Depending of the verification, the function will return a Pair, that is composed of a HttpStatus and a string
     * that briefly describe why the validation is refused.
     * If the product is validated, the function return an HttpStatus.Ok with an empty string.
     * @param productCuDTO Correspond to the Product given by the user.
     * @return The HttpStatusCode and the brief description as a string.
     */
    private Pair<HttpStatus, String> validateProduct(ProductCuDTO productCuDTO, boolean isUpdate) {
        if (productCuDTO.getName() == null || productCuDTO.getName().equals("")) {
            return Pair.of(HttpStatus.BAD_REQUEST, "PRODUCT_INVALID_NAME");
        }
        if (productCuDTO.getDescription() == null || productCuDTO.getDescription().equals("")) {
            return Pair.of(HttpStatus.BAD_REQUEST, "PRODUCT_INVALID_DESCRIPTION");
        }
        if (productCuDTO.getPurchasePrice() < 0) {
            return Pair.of(HttpStatus.BAD_REQUEST, "PRODUCT_INVALID_PURCHASE_PRICE");
        }
        if (productCuDTO.getSalePrice() < 0) {
            return Pair.of(HttpStatus.BAD_REQUEST, "PRODUCT_INVALID_SALE_PRICE");
        }
        if (productCuDTO.getStock() < 0) {
            return Pair.of(HttpStatus.BAD_REQUEST, "PRODUCT_INVALID_STOCK");
        }
        if (productCuDTO.getMinStock() < 0) {
            return Pair.of(HttpStatus.BAD_REQUEST, "PRODUCT_INVALID_MINIMUM_STOCK");
        }
        if (productCuDTO.getBatchSize() < 0) {
            return Pair.of(HttpStatus.BAD_REQUEST, "PRODUCT_INVALID_BATCH_SIZE");
        }
        if (productCuDTO.getBrandId() == null || !bRepository.existsById(productCuDTO.getBrandId())) {
            return Pair.of(HttpStatus.NOT_FOUND, "PRODUCT_INVALID_BRAND_ID");
        }
        if (productCuDTO.getCategoryId() == null || !cRepository.existsById(productCuDTO.getCategoryId())) {
            return Pair.of(HttpStatus.NOT_FOUND, "PRODUCT_INVALID_CATEGORY_ID");
        }
        if (productCuDTO.getSupplierId() == null || !sRepository.existsById(productCuDTO.getSupplierId())) {
            return Pair.of(HttpStatus.NOT_FOUND, "PRODUCT_INVALID_SUPPLIER_ID");
        }
        if (!isUpdate) {
            if (pRepository.existsByNameAndSupplierId(productCuDTO.getName(), productCuDTO.getSupplierId())) {
                return Pair.of(HttpStatus.CONFLICT, "PRODUCT_ALREADY_EXISTS");
            }
        }
        return Pair.of(HttpStatus.ACCEPTED, "");
    }


    /* ------------------------------------------------- API METHODS ------------------------------------------------ */

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get all the products.
     * Firstly, database query to find all products through the product repository.
     * Secondly, check that the returned page is not empty
     *      If page empty, return an HttpStatus.NO_CONTENT.
     * Thirdly, convert each product to as a ProductDTO object. Also adding the HATEOAS links.
     * Finally, return the PageModel of ProductDTO object with HttpStatus.OK.
     *
     * @param user authenticated Employee object
     * @param searchQuery the search query, which can be null or an empty string
     * @param pageable pagination information (page number, size, and sorting)
     * @param sort sorting information for the query
     * @return a ResponseEntity containing a page model of ProductDTO objects or a Error Message.
     *      --> HttpStatus.OK if at least one product has been found. (Page of ProductDTO)
     *      --> HttpStatus.NO_CONTENT if no product exists. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_VENDOR', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<?> getProduct(@AuthenticationPrincipal Employee user,
                                                      @RequestParam(required = false) String searchQuery,
                                                      @PageableDefault(size = 10) Pageable pageable,
                                                      @SortDefault.SortDefaults({
                                                              @SortDefault(sort = "name", direction = Sort.Direction.ASC)}) Sort sort) {
        try {
            log.info("User {} is requesting all the products.", user.getUsername());
            Specification<Product> spec = null;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                spec = (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + searchQuery.toLowerCase() + "%");
            }
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            Page<Product> products = pRepository.findAll(spec, pageable);
            if (products.getTotalElements() < 1) {
                log.info("User {} requested all the products. NO DATA FOUND.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_PRODUCT_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            List<ProductSimpleDTO> productsDTO = new ArrayList<>();
            for (Product product : products) {
                ProductSimpleDTO productSimpleDTO = ProductSimpleDTO.convert(product);
                createHATEOAS(productSimpleDTO);
                productsDTO.add(productSimpleDTO);
            }
            PagedModel.PageMetadata pmd = new PagedModel.PageMetadata(products.getSize(), products.getNumber(), products.getTotalElements());
            PagedModel<ProductSimpleDTO> productDTOPage = PagedModel.of(productsDTO, pmd);
            productDTOPage.add(linkTo(ProductController.class).withRel("products"));
            log.info("User {} requested all the products. RETURNING DATA.", user.getUsername());
            return new ResponseEntity<>(productDTOPage, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested all the products. UNEXPECTED ERROR.", user.getUsername());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get the product that corresponds to a given id.
     * Firstly, database query to find the corresponding product using the product repository.
     * Secondly, verification of the returned Optional object.
     *      --> If is not present: returns an HttpStatus.BAD_REQUEST to the user.
     * Thirdly, Product object conversion into ProductSimpleDTO and adding HATEOAS links.
     * Finally, return the ProductSimpleDTO object with HttpStatus.OK.
     *
     * @param id Correspond to the id of the product searched by the user.
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing a ProductSimpleDTO objects or a Error Message.
     *      --> HttpStatus.OK if the product exists. (ProductSimpleDTO)
     *      --> HttpStatus.BAD_REQUEST if no product corresponds to the id. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_VENDOR', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<?> getProduct(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting the product with id: '{}'.", user.getUsername(), id);
            Optional<Product> productOptional = pRepository.findById(id);
            if (productOptional.isEmpty()) {
                log.info("User {} requested the product with id: '{}'. NO DATA FOUND.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_PRODUCT_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            ProductSimpleDTO productSimpleDTO = ProductSimpleDTO.convert(productOptional.get());
            createHATEOAS(productSimpleDTO);
            log.info("User {} requested the product with id: '{}'. RETURNING DATA.", user.getUsername(), id);
            return new ResponseEntity<>(productSimpleDTO, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested the product with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get the details of a product that corresponds to a given id.
     * Firstly, database query to find the corresponding product using the product repository.
     * Secondly, verification of the returned Optional object.
     *      --> If is not present: returns an HttpStatus.BAD_REQUEST to the user.
     * Thirdly, Product object conversion into ProductCompleteDTO and adding HATEOAS links.
     * Finally, return the ProductCompleteDTO object with HttpStatus.OK.
     *
     * @param id Correspond to the id of the product searched by the user.
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing a ProductCompleteDTO objects or a Error Message.
     *      --> HttpStatus.OK if the product exists. (ProductCompleteDTO)
     *      --> HttpStatus.BAD_REQUEST if no product corresponds to the id. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/{id}/details", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<?> getProdDetail(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting the detailed product with id: '{}'.", user.getUsername(), id);
            Optional<Product> productOptional = pRepository.findById(id);
            if (productOptional.isEmpty()) {
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_PRODUCT_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            ProductCompleteDTO productCompleteDTO = ProductCompleteDTO.convert(productOptional.get());
            createHATEOAS(productCompleteDTO);
            log.info("User {} requested the detailed product with id: '{}'. RETURNING DATA.", user.getUsername(), id);
            return new ResponseEntity<>(productCompleteDTO, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested the detailed product with id: '{}' UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to obtain all the products that have a low stock.
     * A product has a low stock if it's stock <= minimalStock.
     * Firstly, we get all the data that correspond to our criteria.
     * Secondly, we check that the list is not empty.
     *      If the list is empty (no product), we return an HttpStatus.NO_CONTENT.
     * Thirdly (if we have some products), we transform each product as a ProductDTO and we create HATEOAS link.
     * Finally, we return all the data to the user with an HttpStatus.Ok.
     *
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing a page model of ProductSimpleDTO objects or a Error Message.
     *      --> HttpStatus.OK if at least one product has been found. (Page of ProductSimpleDTO)
     *      --> HttpStatus.NO_CONTENT if no product exists. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/low", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<?> getLowStockProd(@AuthenticationPrincipal Employee user,
                                                           @RequestParam(required = false) String searchQuery,
                                                           @PageableDefault(size = 10) Pageable pageable,
                                                           @SortDefault.SortDefaults({
                                                                   @SortDefault(sort = "stock", direction = Sort.Direction.ASC)}) Sort sort) {
        try {
            log.info("User {} is requesting all the products that have a low stock.", user.getUsername());
            Specification<Product> spec = null;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                spec = (root, query, cb) -> cb.like(cb.lower(root.get("stock")), "%" + searchQuery.toLowerCase() + "%");
            }
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            Page<Product> lowStock = pRepository.findByStockIsLessThanMinStock(spec, pageable);
            if (lowStock.getTotalElements() < 1) {
                log.info("User {} requested all the products that have a low stock. NO DATA FOUND", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_LOW_PRODUCT_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            List<ProductCompleteDTO> productCompleteDTOS = new ArrayList<>();
            for (Product product : lowStock) {
                ProductCompleteDTO productCompleteDTO = ProductCompleteDTO.convert(product);
                createHATEOAS(productCompleteDTO);
                productCompleteDTOS.add(productCompleteDTO);
            }
            PagedModel.PageMetadata pmd = new PagedModel.PageMetadata(lowStock.getSize(), lowStock.getNumber(), lowStock.getTotalElements());
            PagedModel<ProductCompleteDTO> productDTOPage = PagedModel.of(productCompleteDTOS, pmd);
            productDTOPage.add(linkTo(ProductController.class).slash("low").withSelfRel());
            log.info("User {} requested all the products that have a low stock. RETURNING DATA.", user.getUsername());
            return new ResponseEntity<>(productDTOPage, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested all the products that have a low stock. UNEXPECTED ERROR!", user.getUsername());
            System.out.println(e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to create a new product.
     * Firstly, we verify that the data entered by the user are correct, by using the validateProduct function.
     *      If the validation return a different code that HttpStatus.Ok, we return a ResponseEntity to the user with
     *      the code given by validateProduct.
     * Secondly, when all data are correct, we can search the related objects in the database.
     * Thirdly, we can create the product object that will be persisted.
     * To create the object we use the createProductObj function.
     * Fourthly, we can persist the new product in the database.
     * Finally, we can create a ProductCompleteDTO of the persisted object and return data to the user.
     *
     * @param productCuDTO Correspond to the data that are given by the user.
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing a ProductCompleteDTO objects or a Error Message.
     *      --> HttpStatus.CREATED if the product has been created. (ProductCompleteDTO)
     *      --> HttpStatus.XX if a criteria has not been validated. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<?> createProduct(@RequestBody ProductCuDTO productCuDTO, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to create a new product with name: '{}'.", user.getUsername(), productCuDTO.getName());
            Pair<HttpStatus, String> validation = validateProduct(productCuDTO, false);
            if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
                log.info("User {} requested to create the product with name: '{}'. {}",
                        user.getUsername(), productCuDTO.getName(), validation.getSecond());
                ErrorResponse bm = new ErrorResponse(validation.getFirst().getReasonPhrase(), validation.getSecond());
                return new ResponseEntity<>(bm, validation.getFirst());
            }
            Brand brandOptional = bRepository.findById(productCuDTO.getBrandId()).get();
            Category categoryOptional = cRepository.findById(productCuDTO.getCategoryId()).get();
            Supplier supplierOptional = sRepository.findById(productCuDTO.getSupplierId()).get();
            Product product = new Product();
            createProductObj(productCuDTO, product, brandOptional, categoryOptional, supplierOptional, false);
            log.warn("User {} requested to create the product with name: '{}'. SAVING DATA", user.getUsername(), product.getName());
            Product savedProduct = pRepository.save(product);
            ProductCompleteDTO productCompleteDTO = ProductCompleteDTO.convert(savedProduct);
            createHATEOAS(productCompleteDTO);
            log.info("User {} requested to create the product with name: '{}'. RETURNING DATA", user.getUsername(), savedProduct.getName());
            return new ResponseEntity<>(productCompleteDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            log.info("User {} requested to create the product with name: '{}'. UNEXPECTED ERROR!", user.getUsername(), productCuDTO.getName());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to update an existing product.
     * Firstly, we check that the id given in the url path and in the ProductCuDto are the same.
     * We also check that the product already exists with the given id.
     * Secondly, we can use the function validateProduct to get sure that the product is correct.
     * Thirdly, if everything is OK, we can go to select all the related object in the database.
     * Fourthly, we can create the updated product with the createProductObj function, and we can save the object.
     * Finally, we can return the saved object to the user with an HttpStatus.OK.
     *
     * @param id The id that is given in the url.
     * @param productCuDTO Corresponds to the product data, that the user wants to update.
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing a ProductCompleteDTO objects or a Error Message.
     *      --> HttpStatus.OK if the product has been updated. (ProductCompleteDTO)
     *      --> HttpStatus.BAD_REQUEST if no product corresponds to the given id. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<?> updateProduct(@PathVariable(value = "id") Long id,
                                                         @RequestBody ProductCuDTO productCuDTO,
                                                         @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to update the product with id: '{}'.", user.getUsername(), id);
            Optional<Product> productOptional = pRepository.findById(id);
            if (productOptional.isEmpty()) {
                log.info("User {} requested to update the product with id: '{}'. NO DATA FOUND.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_PRODUCT_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            Pair<HttpStatus, String> validation = validateProduct(productCuDTO, true);
            if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
                log.info("User {} requested to update the product with name: '{}'. {}", user.getUsername(), productCuDTO.getName(), validation.getSecond());
                ErrorResponse bm = new ErrorResponse(validation.getFirst().getReasonPhrase(), validation.getSecond());
                return new ResponseEntity<>(bm, validation.getFirst());
            }
            Brand brandOptional = bRepository.findById(productCuDTO.getBrandId()).get();
            Category categoryOptional = cRepository.findById(productCuDTO.getCategoryId()).get();
            Supplier supplierOptional = sRepository.findById(productCuDTO.getSupplierId()).get();
            Product product = productOptional.get();
            createProductObj(productCuDTO, product, brandOptional, categoryOptional, supplierOptional, true);
            log.debug("User {} requested to update the product with id: '{}'. SAVING DATA", user.getUsername(), product.getId());
            Product savedProduct = pRepository.save(product);
            ProductCompleteDTO productCompleteDTO = ProductCompleteDTO.convert(savedProduct);
            createHATEOAS(productCompleteDTO);
            log.info("User {} requested to update the product with id: '{}'. RETURNING DATA", user.getUsername(), savedProduct.getId());
            return new ResponseEntity<>(productCompleteDTO, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to update the product with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), productCuDTO.getName());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AVAILABLE FOR: ROLE_ADMIN
     * This function is used to delete a product if it is no more linked to an order line.
     * Firstly, we check that a product exists with the id given by the user.
     *      If the product does not exist, we return an HttpStatus.NO_CONTENT to the use.
     * Secondly, we check that the product is no more linked to any order line.
     *      If the product still has some relations, we return an HttpStatus.NOT_ACCEPTABLE to the user.
     * Finally, we can delete the product in the database, and return to the user an HttpStatus.OK.
     *
     * @param id Corresponds to the product's id that the user wants to delete.
     * @param user Corresponds to the user that is authenticated.
     * @return a ResponseEntity containing an Error Message.
     *      --> HttpStatus.OK if the product has been deleted.
     *      --> HttpStatus.BAD_REQUEST if no product corresponds to the given id.
     *      --> HttpStatus.CONFLICT if the product has at least one relationship.
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs.
     */
    @DeleteMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<?> deleteProduct(@PathVariable(value = "id") Long id, @AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to delete the product with id: '{}'.", user.getUsername(), id);
            if (!pRepository.existsById(id)) {
                log.info("User {} requested to delete the product with id: '{}'. NO PRODUCT FOUND.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_PRODUCT_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            if (solRepository.existsByProductId(id) || colRepository.existsByProductId(id)) {
                log.info("User {} requested to delete the product with id: '{}'. PRODUCT HAS RELATIONS.", user.getUsername(), id);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "PRODUCT_HAS_RELATIONSHIPS");
                return new ResponseEntity<>(bm, HttpStatus.CONFLICT);
            }
            log.debug("User {} requested to delete the product with id: '{}'. DELETING PRODUCT.", user.getUsername(), id);
            pRepository.deleteById(id);
            log.info("User {} requested to delete the product with id: '{}'. PRODUCT DELETED.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to delete the product with id: '{}'. UNEXPECTED ERROR!", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
