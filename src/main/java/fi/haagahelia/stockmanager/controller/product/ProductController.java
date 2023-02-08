package fi.haagahelia.stockmanager.controller.product;


import fi.haagahelia.stockmanager.controller.product.brand.BrandController;
import fi.haagahelia.stockmanager.controller.product.category.CategoryController;
import fi.haagahelia.stockmanager.controller.supplier.SupplierController;
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
     * @return the constructed object.
     */
    private Product createProductObj(ProductCuDTO productCuDTO, Brand brand, Category category,
                                     Supplier supplier, boolean isUpdate) {
        Product product = new Product();
        if (isUpdate) product.setId(productCuDTO.getId());
        product.setName(productCuDTO.getName());
        product.setDescription(productCuDTO.getDescription());
        product.setPurchasePrice(productCuDTO.getPurchasePrice());
        product.setSalePrice(productCuDTO.getSalePrice());
        product.setStock(productCuDTO.getStock());
        product.setMinStock(productCuDTO.getMinStock());
        product.setBatchSize(productCuDTO.getBatchSize());
        product.setBrand(brand);
        product.setCategory(category);
        product.setSupplier(supplier);
        return product;
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
    private Pair<HttpStatus, String> validateProduct(ProductCuDTO productCuDTO) {
        if (productCuDTO.getName() == null || productCuDTO.getName().equals("")) {
            return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID NAME");
        }
        if (productCuDTO.getDescription() == null || productCuDTO.getDescription().equals("")) {
            return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID DESCRIPTION");
        }
        if (productCuDTO.getPurchasePrice() < 0) {
            return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID PURCHASE PRICE");
        }
        if (productCuDTO.getSalePrice() < 0) {
            return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID SALE PRICE");
        }
        if (productCuDTO.getStock() < 0) {
            return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID STOCK");
        }
        if (productCuDTO.getMinStock() < 0) {
            return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID MINIMUM STOCK");
        }
        if (productCuDTO.getBatchSize() < 0) {
            return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID BATCH SIZE");
        }
        if (pRepository.existsByNameAndSupplierId(productCuDTO.getName(), productCuDTO.getSupplierId())) {
            return Pair.of(HttpStatus.CONFLICT, "ALREADY EXISTS");
        }
        if (productCuDTO.getBrandId() == null || !bRepository.existsById(productCuDTO.getBrandId())) {
            return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID BRAND ID");
        }
        if (productCuDTO.getCategoryId() == null || !cRepository.existsById(productCuDTO.getCategoryId())) {
            return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID CATEGORY ID");
        }
        if (productCuDTO.getSupplierId() == null || !sRepository.existsById(productCuDTO.getSupplierId())) {
            return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID SUPPLIER ID");
        }
        return Pair.of(HttpStatus.ACCEPTED, "");
    }


    /* ------------------------------------------------- API METHODS ------------------------------------------------ */

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get all the products that are saved in the database.
     * Firstly, we will SELECT all the products that are in the database, using the product repository.
     * Secondly, we check that the list returned by the previous step as at least one product.
     *      If not, the list is empty, so, we return an empty ResponseEntity with an HttpStatus.NO_CONTENT.
     * Thirdly, we transform all the products as a ProductDTO object. We also add the HATEOAS links.
     * Finally, we return the data in a ResponseEntity with the HttpStatus.OK.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<List<ProductSimpleDTO>> getAllProducts(@AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting all the products.", user.getUsername());
        List<Product> products = pRepository.findAll();
        if (products.size() < 1) {
            log.info("User {} requested all the products. NO DATA FOUND.", user.getUsername());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<ProductSimpleDTO> productsDTO = new ArrayList<>();
        for (Product product : products) {
            ProductSimpleDTO productSimpleDTO = ProductSimpleDTO.convert(product);
            createHATEOAS(productSimpleDTO);
            productsDTO.add(productSimpleDTO);
        }
        log.info("User {} requested all the products. RETURNING DATA.", user.getUsername());
        return new ResponseEntity<>(productsDTO, HttpStatus.OK);
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get the product that corresponds to a given id.
     * Firstly, we will SELECT the data in the database.
     * Secondly, we verify that data is not empty. If data is empty, we return a HttpStatus.NO_CONTENT.
     * Thirdly, we transform the product as a ProductSimpleDTO object, and we add the HATEOAS links to the object.
     * Finally, if everything went OK, we return the data to the user with a HttpStatus.OK.
     * @param id Correspond to the id of the product searched by the user.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if it exists.
     */
    @GetMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<ProductSimpleDTO> getProductById(@PathVariable(value = "id") Long id,
                                                                         @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting the product with id: {}.", user.getUsername(), id);
        Optional<Product> productOptional = pRepository.findById(id);
        if (productOptional.isEmpty()) {
            log.info("User {} requested the product with id: {}. NO DATA FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        ProductSimpleDTO productSimpleDTO = ProductSimpleDTO.convert(productOptional.get());
        createHATEOAS(productSimpleDTO);
        log.info("User {} requested the product with id: {}. RETURNING DATA.", user.getUsername(), id);
        return new ResponseEntity<>(productSimpleDTO, HttpStatus.OK);
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get the detailed product that correspond to a given id.
     * Firstly, we will SELECT the data in the database.
     * Secondly, we verify that data is not empty. If data is empty, we return a HttpStatus.NO_CONTENT.
     * Thirdly we transform the product as a ProductSimpleDTO object, and we add the HATEOAS links to the object.
     * Finally, if everything went OK, we return the data to the user with a HttpStatus.OK.
     * @param id Correspond to the id of the product searched by the user.
     * @param user Corresponds to the user that is authenticated.
     * @return Return a ResponseEntity with the corresponding HttpStatus and the ProductDTO.
     */
    @GetMapping(value = "/{id}/details", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<ProductCompleteDTO> getProdDetailsById(@PathVariable(value = "id") Long id,
                                                                               @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting the detailed product with id: {}.", user.getUsername(), id);
        Optional<Product> productOptional = pRepository.findById(id);
        if (productOptional.isEmpty()) {
            log.info("User {} requested the detailed product with id: {}. NO DATA FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        ProductCompleteDTO productCompleteDTO = ProductCompleteDTO.convert(productOptional.get());
        createHATEOAS(productCompleteDTO);
        log.info("User {} requested the detailed product with id: {}. RETURNING DATA.", user.getUsername(), id);
        return new ResponseEntity<>(productCompleteDTO, HttpStatus.OK);
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
     * @param user Corresponds to the user that is authenticated.
     * @return ResponseEntity with data and the HttpStatus.Ok.
     */
    @GetMapping(value = "/low", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<List<ProductCompleteDTO>> getLowStockProd(@AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting all the products that have a low stock.", user.getUsername());

        List<Product> lowStockProducts = pRepository.findByStockIsLessThanMinStock();

        if (lowStockProducts.size() == 0) {
            log.info("User {} requested all the products that have a low stock. NO DATA FOUND", user.getUsername());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        List<ProductCompleteDTO> productCompleteDTOS = new ArrayList<>();
        for (Product product : lowStockProducts) {
            ProductCompleteDTO productCompleteDTO = ProductCompleteDTO.convert(product);
            createHATEOAS(productCompleteDTO);
            productCompleteDTOS.add(productCompleteDTO);
        }
        log.info("User {} requested all the products that have a low stock. RETURNING DATA.", user.getUsername());
        return new ResponseEntity<>(productCompleteDTOS, HttpStatus.OK);
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
     * @param productCuDTO Correspond to the data that are given by the user.
     * @param user Corresponds to the user that is authenticated.
     * @return Is a ResponseEntity object with the HttpStatus code and data.
     */
    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<ProductCompleteDTO> createProduct(@RequestBody ProductCuDTO productCuDTO,
                                                                          @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to create a new product with name: {}",
                user.getUsername(), productCuDTO.getName());
        Pair<HttpStatus, String> productValidation = validateProduct(productCuDTO);
        if (!productValidation.getFirst().equals(HttpStatus.ACCEPTED)) {
            log.info("User {} requested to create the product with name: {}. {}",
                    user.getUsername(), productCuDTO.getName(), productValidation.getSecond());
            return new ResponseEntity<>(productValidation.getFirst());
        }
        Brand brandOptional = bRepository.findById(productCuDTO.getBrandId()).get();
        Category categoryOptional = cRepository.findById(productCuDTO.getCategoryId()).get();
        Supplier supplierOptional = sRepository.findById(productCuDTO.getSupplierId()).get();

        Product product = createProductObj(productCuDTO, brandOptional, categoryOptional, supplierOptional, false);

        log.warn("User {} requested to create the product with name: {}. SAVING DATA",
                user.getUsername(), product.getName());
        Product savedProduct = pRepository.save(product);
        ProductCompleteDTO productCompleteDTO = ProductCompleteDTO.convert(savedProduct);
        createHATEOAS(productCompleteDTO);
        log.info("User {} requested to create the product with name: {}. RETURNING DATA",
                user.getUsername(), savedProduct.getName());
        return new ResponseEntity<>(productCompleteDTO, HttpStatus.CREATED);
    }

    /**
     * AVAILABLE FOR: ROLE_MANAGER | ROLE_ADMIN
     * This function is used to update a product.
     * Firstly, we check that the id given in the url path and in the ProductCuDto are the same.
     * We also check that the product already exists with the given id.
     * Secondly, we can use the function validateProduct to get sure that the product is correct.
     * Thirdly, if everything is OK, we can go to select all the related object in the database.
     * Fourthly, we can create the updated product with the createProductObj function, and we can save the object.
     * Finally, we can return the saved object to the user with an HttpStatus.ACCEPTED.
     * @param id The id that is given in the url.
     * @param productCuDTO Corresponds to the product data, that the user wants to update.
     * @param user Corresponds to the user that is authenticated.
     * @return Is a ResponseEntity that contains data and the corresponding HttpStatus code.
     */
    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public @ResponseBody ResponseEntity<ProductCompleteDTO> updateProductById(@PathVariable(value = "id") Long id,
                                                                              @RequestBody ProductCuDTO productCuDTO,
                                                                              @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to update the product with id: {}.", user.getUsername(), id);
        if (!pRepository.existsById(id)) {
            log.info("User {} requested to update the product with id: {}. NO DATA FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Pair<HttpStatus, String> productValidation = validateProduct(productCuDTO);
        if (!productValidation.getFirst().equals(HttpStatus.ACCEPTED)) {
            log.info("User {} requested to update the product with name: {}. {}",
                    user.getUsername(), productCuDTO.getName(), productValidation.getSecond());
            return new ResponseEntity<>(productValidation.getFirst());
        }
        Brand brandOptional = bRepository.findById(productCuDTO.getBrandId()).get();
        Category categoryOptional = cRepository.findById(productCuDTO.getCategoryId()).get();
        Supplier supplierOptional = sRepository.findById(productCuDTO.getSupplierId()).get();

        Product product = createProductObj(productCuDTO, brandOptional, categoryOptional, supplierOptional, true);
        log.debug("User {} requested to update the product with id: {}. SAVING DATA",
                user.getUsername(), product.getId());
        Product savedProduct = pRepository.save(product);
        ProductCompleteDTO productCompleteDTO = ProductCompleteDTO.convert(savedProduct);
        createHATEOAS(productCompleteDTO);
        log.info("User {} requested to update the product with id: {}. RETURNING DATA",
                user.getUsername(), savedProduct.getId());
        return new ResponseEntity<>(productCompleteDTO, HttpStatus.ACCEPTED);
    }

    /**
     * AVAILABLE FOR: ROLE_ADMIN
     * This function is used to delete a product if it is no more linked to an order line.
     * Firstly, we check that a product exists with the id given by the user.
     *      If the product does not exist, we return an HttpStatus.NO_CONTENT to the use.
     * Secondly, we check that the product is no more linked to any order line.
     *      If the product still has some relations, we return an HttpStatus.NOT_ACCEPTABLE to the user.
     * Finally, we can delete the product in the database, and return to the user an HttpStatus.ACCEPTED.
     * @param id Corresponds to the product's id that the user wants to delete.
     * @param user Corresponds to the user that is authenticated.
     * @return Is a ResponseEntity that contains the corresponding HttpStatus code.
     */
    @DeleteMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<ProductCompleteDTO> deleteProdByID(@PathVariable(value = "id") Long id,
                                                                           @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to delete the product with id: {}.", user.getUsername(), id);
        if (!pRepository.existsById(id)) {
            log.info("User {} requested to delete the product with id: {}. NO PRODUCT FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        if (solRepository.existsByProductId(id) || colRepository.existsByProductId(id)) {
            log.info("User {} requested to delete the product with id: {}. PRODUCT HAS RELATIONS.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        log.debug("User {} requested to delete the product with id: {}. DELETING PRODUCT.", user.getUsername(), id);
        pRepository.deleteById(id);
        log.info("USer {} requested to delete the product with id: {}. PRODUCT DELETED.", user.getUsername(), id);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
