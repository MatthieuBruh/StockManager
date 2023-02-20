package fi.haagahelia.stockmanager.controller.customer;

import fi.haagahelia.stockmanager.controller.common.GeolocationController;
import fi.haagahelia.stockmanager.dto.customer.CustomerCuDTO;
import fi.haagahelia.stockmanager.dto.customer.CustomerDTO;
import fi.haagahelia.stockmanager.model.common.Geolocation;
import fi.haagahelia.stockmanager.model.customer.Customer;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.common.GeolocationRepository;
import fi.haagahelia.stockmanager.repository.customer.CustomerRepository;
import fi.haagahelia.stockmanager.repository.customer.order.CustomerOrderRepository;
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
@RequestMapping("/api/customers")
public class CustomerController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final CustomerRepository cRepository;
    private final GeolocationRepository gRepository;
    private final CustomerOrderRepository oRepository;

    @Autowired
    public CustomerController(CustomerRepository cRepository, GeolocationRepository gRepository,
                              CustomerOrderRepository oRepository) {
        this.cRepository = cRepository;
        this.gRepository = gRepository;
        this.oRepository = oRepository;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    /**
     * This method is used to create and add the HATEOAS links to a DTO models.
     * @param customerDTO The dto model to which we will add the HATEOAS links.
     * @return The dto model with the HATEOAS links.
     */
    private CustomerDTO createHATEOAS(CustomerDTO customerDTO) {
        Link selfLink = linkTo(CustomerController.class).slash(String.valueOf(customerDTO.getId())).withSelfRel();
        customerDTO.add(selfLink);
        Link collectionLink = linkTo(CustomerController.class).withRel("customers");
        customerDTO.add(collectionLink);
        // Geolocation related field
        if (customerDTO.getGeolocationDTO() != null) {
            Long geolocationId = customerDTO.getGeolocationDTO().getId();
            Link geolocation = linkTo(GeolocationController.class).slash(geolocationId).withRel("geolocation");
            customerDTO.add(geolocation);

        }
        return customerDTO;
    }

    /**
     * This function is used to validate a customer (before create or update)
     * We check the following constraints:
     *      - First name is not null, and not empty
     *      - Last name is not null, and not empty
     *      - Geolocation exists if it is not null
     *      - Email is not null, not empty, and does not already exist.
     *          - If update we verify that a customer has this email address.
     *          - If create we verify that no customers have this email address.
     * @param customerCuDTO Corresponds to the customer that we have to validate.
     * @param isForUpdate In the case of an update we have this variable should be true
     * @return A Pair object that contains an HttpStatus and the decision reason.
     */
    private Pair<HttpStatus, String> validateCustomer(CustomerCuDTO customerCuDTO, boolean isForUpdate) {
        if (customerCuDTO.getFirstName() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "FIRST NAME IS NULL.");
        if (customerCuDTO.getFirstName().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "FIRST NAME IS EMPTY.");
        if (customerCuDTO.getLastName() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "LAST NAME IS NULL.");
        if (customerCuDTO.getLastName().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "LAST NAME IS EMPTY.");
        if (customerCuDTO.getEmail() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "EMAIL IS NULL.");
        if (customerCuDTO.getEmail().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "EMAIL IS EMPTY");
        if (customerCuDTO.getGeolocationId() != null) {
            if (!gRepository.existsById(customerCuDTO.getGeolocationId())) {
                return Pair.of(HttpStatus.NOT_FOUND, "NO GEOLOCATION FOUND");
            }
        }
        if (isForUpdate) {
            if (!cRepository.existsByEmail(customerCuDTO.getEmail()))
                return Pair.of(HttpStatus.NOT_FOUND, "CUSTOMER NOT FOUND.");
        } else {
            if (cRepository.existsByEmail(customerCuDTO.getEmail()))
                return Pair.of(HttpStatus.CONFLICT, "CUSTOMER ALREADY EXISTS.");
        }
        return Pair.of(HttpStatus.ACCEPTED, "");
    }

    /**
     * This function is used to add a geolocation to a customer, if needed.
     * We start by checking that the id correspond to a valid geolocation.
     * If correspond to a valid geolocation we add the geolocation to the supplier object.
     * Else, we do nothing.
     * @param customer the customer that we need to add the geolocation
     * @param geoId the geolocation id
     */
    private void setGeolocationById(Customer customer, Long geoId, String username) {
        Optional<Geolocation> geolocationOptional = gRepository.findById(geoId);
        if (geolocationOptional.isPresent()) {
            log.debug("User {} requested to create a new customer with the name: {}. Adding the geolocation with id: {}.",
                    username, customer.getFirstName() + customer.getLastName(), geoId);
            Geolocation geolocation = geolocationOptional.get();
            customer.setGeolocation(geolocation);
        } else {
            customer.setGeolocation(null);
        }
    }

    /* ------------------------------------------------- API METHODS ------------------------------------------------ */

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get all the customers that are in the database.
     * Firstly, we will find all the customers that are in the database by using the customer repository.
     * Secondly, we check that the list is not empty.
     *      If the list is empty, we return an empty ResponseEntity with an HttpStatus.NO_CONTENT.
     * Thirdly, we transform all the customers as a CustomerDTO objects. We also add the HATEOAS links.
     * Finally, we return the data in a ResponseEntity with the HttpStatus.OK.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<List<CustomerDTO>> getAllCustomers(@AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting all the customers from the database.", user.getUsername());
        List<Customer> customers = cRepository.findAll();
        if (customers.size() < 1) {
            log.info("User {} requested all the customers from the database. NO DATA FOUND", user.getUsername());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<CustomerDTO> customerDTOS = new ArrayList<>();
        for (Customer customer : customers) {
            CustomerDTO customerDTO = CustomerDTO.convert(customer);
            customerDTOS.add(createHATEOAS(customerDTO));
        }
        log.info("User {} requested all the customers from the database. RETURNING DATA.", user.getUsername());
        return new ResponseEntity<>(customerDTOS, HttpStatus.OK);
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to get a customer by its email.
     * Firstly, we find the customer by using the customer repository.
     * Secondly, we check that the Optional returned by the customer repository is empty.
     *      If it is empty, we return an HttpStatus.NO_CONTENT to the user.
     * Thirdly, we can convert the Customer to a CustomerDTO. We also add the HATEOAS links.
     * Finally, we return the data to the user with an HttpStatus.OK.
     * @param email Corresponds to the email address of the searched customer.
     * @param user Corresponds to the authenticated user.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @GetMapping(value = "/{email}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<CustomerDTO> getCustomerById(@PathVariable(value = "email") String email,
                                                                     @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting the customer with email: {}.", user.getUsername(), email);
        Optional<Customer> customerOptional = cRepository.findByEmail(email);
        if (!customerOptional.isPresent()) {
            log.info("User {} requested the customer with email: {}. NO DATA FOUND.", user.getUsername(), email);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        CustomerDTO customerDTO = CustomerDTO.convert(customerOptional.get());
        createHATEOAS(customerDTO);
        log.info("User {} requested the customer with email: {}. RETURNING DATA.", user.getUsername(), email);
        return new ResponseEntity<>(customerDTO, HttpStatus.OK);
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to create, and save a new customer.
     * Firstly, we validate the customer data by using the validateCustomer function.
     *      If the data are not validated, we return an HttpStatus with the corresponding code to the user.
     * Secondly, we create a new customer, and we set all his attributes.
     * Thirdly, we save the data in the database using the customer repository.
     * Fourthly, we convert the customer as a CustomerDTO, and we add the HATEOAS links.
     * Finally, we return the data to the user with an HttpStatus.CREATED.
     * @param customerCuDTO Corresponds to the new customer information that the user wants to save.
     * @param user Corresponds to the authenticated user.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<CustomerDTO> createCustomer(@RequestBody CustomerCuDTO customerCuDTO,
                                                                    @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to create and save a new customer with email: '{}'", user.getUsername(),
                customerCuDTO.getEmail());
        Pair<HttpStatus, String> validation = validateCustomer(customerCuDTO, false);
        if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
            log.info("User {} requested to create and save a new customer with email: '{}'. {}", user.getUsername(),
                    customerCuDTO.getEmail(), validation.getSecond());
            return new ResponseEntity<>(validation.getFirst());
        }
        Customer customer = new Customer();
        customer.setFirstName(customerCuDTO.getFirstName());
        customer.setLastName(customerCuDTO.getLastName());
        customer.setEmail(customerCuDTO.getEmail());
        setGeolocationById(customer, customerCuDTO.getGeolocationId(), user.getUsername());

        log.debug("User {} requested to create and save a new customer with email: '{}'. SAVING CUSTOMER.",
                user.getUsername(), customer.getEmail());
        Customer savedCustomer = cRepository.save(customer);
        CustomerDTO customerDTO = CustomerDTO.convert(savedCustomer);
        createHATEOAS(customerDTO);
        log.info("User {} requested to create and save a new customer with email: '{}'. RETURNING DATA.",
                user.getUsername(), savedCustomer.getEmail());
        return new ResponseEntity<>(customerDTO, HttpStatus.CREATED);
    }

    /**
     * AVAILABLE FOR: ROLE_VENDOR | ROLE_MANAGER | ROLE_ADMIN
     * This function is used to update a customer by its email.
     * Firstly, we check that customer already exists with the email given by the user.
     * Secondly, we validate all the attributes of the customerCuDTO by using the validateCustomer function.
     * Thirdly, we modify the data of the customer.
     * Fourthly, we save the modification in the database.
     * Finally, we convert the customer as a CustomerDTO, we add the HATEOAS links, and we return it to the user.
     * @param email Corresponds to the email address of the customer that the user wants to update.
     * @param customerCuDTO Corresponds to the data of the customer that the user wants to update.
     * @param user Corresponds to the authenticated user.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @PutMapping(value = "/{email}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public @ResponseBody ResponseEntity<CustomerDTO> updateCustomerById(@PathVariable(value = "email") String email,
                                                                        @RequestBody CustomerCuDTO customerCuDTO,
                                                                        @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to update the customer with email: '{}'.", user.getUsername(), email);
        Optional<Customer> customerOptional = cRepository.findByEmail(email);
        if (!customerOptional.isPresent()) {
            log.info("User {} requested to update the customer with email: '{}'. NO CUSTOMER FOUND.",
                    user.getUsername(), email);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Pair<HttpStatus, String> validation = validateCustomer(customerCuDTO, true);
        if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
            log.info("User {} requested to update the customer with email: '{}'. {}.", user.getUsername(), email,
                    validation.getSecond());
            return new ResponseEntity<>(validation.getFirst());
        }
        Customer customer = customerOptional.get();
        customer.setFirstName(customerCuDTO.getFirstName());
        customer.setLastName(customerCuDTO.getLastName());
        setGeolocationById(customer, customerCuDTO.getGeolocationId(), user.getUsername());
        log.debug("User {} requested to update the customer with email: '{}'. UPDATING CUSTOMER.", user.getUsername(), email);
        Customer savedCustomer = cRepository.save(customer);
        CustomerDTO customerDTO = CustomerDTO.convert(savedCustomer);
        createHATEOAS(customerDTO);
        log.info("User {} requested to update the customer with email: '{}'. CUSTOMER UPDATED.", user.getUsername(), email);
        return new ResponseEntity<>(customerDTO, HttpStatus.ACCEPTED);
    }

    /**
     * AVAILABLE FOR: ROLE_ADMIN
     * This function is used to delete a customer by its email.
     * Firstly, we find the customer in the database by its email using the customer repository.
     * Secondly, we verify that the Optional returned by the previous step contains a customer.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Thirdly, we delete the relation between the customer and all the related orders.
     * Finally, we can delete the customer from the database.
     * @param email Correspond to the email of the customer to be deleted.
     * @return Return an empty ResponseEntity with the corresponding HttpStatus code.
     */
    @DeleteMapping(value = "/{email}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<CustomerDTO> deleteCustomerById(@PathVariable(value = "email") String email,
                                                                        @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to delete the customer with email: '{}'", user.getUsername(), email);
        Optional<Customer> customer = cRepository.findByEmail(email);
        if (!customer.isPresent()) {
            log.info("User {} requested to delete the customer with email: '{}'. NO DATA FOUND",
                    user.getUsername(), email);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Customer toDelete = customer.get();
        log.debug("User {} requested to delete the customer with email: '{}'. REMOVING RELATED ORDER.",
                user.getUsername(), email);
        oRepository.removeRelatedCustomer(toDelete.getId());
        log.debug("User {} requested to delete the customer with email: '{}'. DELETING DATA.",
                user.getUsername(), email);
        cRepository.deleteById(toDelete.getId());
        log.info("User {} requested to delete the customer with email: '{}'. CUSTOMER DELETED.",
                user.getUsername(), email);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
