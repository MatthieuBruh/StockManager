package fi.haagahelia.stockmanager.controller.user;


import fi.haagahelia.stockmanager.dto.user.EmployeeCuDTO;
import fi.haagahelia.stockmanager.dto.user.EmployeeDTO;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
import fi.haagahelia.stockmanager.repository.user.EmployeeRepository;
import fi.haagahelia.stockmanager.repository.user.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final EmployeeRepository eRepository;
    private final RoleRepository rRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public EmployeeController(EmployeeRepository eRepository, RoleRepository rRepository, PasswordEncoder passwordEncoder) {
        this.eRepository = eRepository;
        this.rRepository = rRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */


    /**
     * This method is used to create and add the HATEOAS links to a DTO models.
     * @param employeeDTO The dto model to which we will add the HATEOAS links.
     * @return The dto model with the HATEOAS links.
     */
    private EmployeeDTO createHATEOAS(EmployeeDTO employeeDTO) {
        Link selfLink = linkTo(EmployeeController.class).slash(String.valueOf(employeeDTO.getId())).withSelfRel();
        employeeDTO.add(selfLink);
        Link collectionLink = linkTo(EmployeeController.class).slash("").withRel("employees");
        // Link collectionLink = linkTo(methodOn(EmployeeController.class)).slash("/").withRel("employees");
        employeeDTO.add(collectionLink);
        return employeeDTO;
    }

    /**
     * This function is used to validate an employeeCuDTO (before creation or modification)
     * We check the following constraints:
     *      - Email is not null and not empty
     *      - Username is not null and not empty
     *      - First name and last name are not null and not empty
     *      - If creation: check that username and email is not already used, and password is not null.
     *      - If modification: check that username and email exist.
     * @param employeeCuDTO Corresponds to the employee's information to validate
     * @param isForUpdate Determine if it is for a modification or not
     * @return A Pair object that contains an HttpStatus and the decision reason.
     */
    private Pair<HttpStatus, String> validateEmployee(EmployeeCuDTO employeeCuDTO, boolean isForUpdate) {
        if (employeeCuDTO.getEmail() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "EMAIL IS NULL");
        if (employeeCuDTO.getEmail().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "EMAIL IS EMPTY");
        if (employeeCuDTO.getUserName() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "USERNAME IS NULL");
        if (employeeCuDTO.getUserName().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "USERNAME IS NULL");
        if (employeeCuDTO.getFirstName() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "FIRST NAME IS NULL");
        if (employeeCuDTO.getFirstName().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "FIRST NAME IS EMPTY");
        if (employeeCuDTO.getLastName() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "LAST NAME IS NULL");
        if (employeeCuDTO.getLastName().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "LAST NAME IS EMPTY");
        if (isForUpdate) {
            if (eRepository.existsByUsername(employeeCuDTO.getUserName())) {
                return Pair.of(HttpStatus.NOT_FOUND, "USERNAME NOT FOUND");
            }
            if (eRepository.existsByEmail(employeeCuDTO.getEmail())) {
                return Pair.of(HttpStatus.NOT_FOUND, "EMAIL NOT FOUND");
            }
        } else {
            if (employeeCuDTO.getPassword() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "PASSWORD IS NULL");
            if (eRepository.existsByUsername(employeeCuDTO.getUserName())) {
                return Pair.of(HttpStatus.CONFLICT, "USERNAME ALREADY EXIST");
            }
            if (eRepository.existsByEmail(employeeCuDTO.getEmail())) {
                return Pair.of(HttpStatus.NOT_FOUND, "EMAIL ALREADY EXIST");
            }
        }
        return Pair.of(HttpStatus.ACCEPTED, "");
    }

    /* ------------------------------------------------- API METHODS ------------------------------------------------ */

    /**
     * Available for: ROLE_ADMIN
     * This function is used to get all the employees from the database.
     * Firstly, we find all the employees by using the employee repository.
     * Secondly, we check that the list returned by the previous step contains at least one employee.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Thirdly, we convert each employee as an EmployeeDTO and we add the HATEOAS links.
     * Finally, we return the list to the user with an HttpStatus.OK.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<List<EmployeeDTO>> getAllEmployees(@AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to get all employees.", user.getUsername());
        List<Employee> employees = eRepository.findAll();
        if (employees.size() < 1) {
            log.info("User {} requested to get all employees. NO DATA FOUND.", user.getUsername());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<EmployeeDTO> employeeDTOS = new ArrayList<>();
        for (Employee emp : employees) {
            EmployeeDTO employeeDTO = EmployeeDTO.convert(emp);
            employeeDTOS.add(createHATEOAS(employeeDTO));
        }
        log.info("User {} requested all the employees. RETURNING DATA.", user.getUsername());
        return new ResponseEntity<>(employeeDTOS, HttpStatus.OK);
    }

    /**
     * Available for: ROLE_ADMIN
     * This function is used to get an employee by his id.
     * Firstly, we find the employee in the database, using the employee repository.
     * Secondly, we check that the optional received in parameter step is not empty.
     *      If it is empty, we return an HttpStatus.NO_CONTENT to the user.
     * Thirdly, we can convert the employee as EmployeeDTO, and we can add him the HATEOAS links.
     * Finally, we can return the data to the user with an HttpStatus.OK.
     * @param id Corresponds to the employee's id searched by the user.
     * @param user Corresponds to the user that is authenticated.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if they exist.
     */
    @GetMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable(value = "id") Long id,
                                                                     @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to get the employee with id: {}.", user.getUsername(), id);
        Optional<Employee> employeeOptional = eRepository.findById(id);
        if (employeeOptional.isEmpty()) {
            log.info("User {} requested to get the employee with id: {}. NO DATA FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        EmployeeDTO employeeDTO = EmployeeDTO.convert(employeeOptional.get());
        createHATEOAS(employeeDTO);
        log.info("User {} requested to get the employee with id: {}. RETURNING DATA.",
                user.getUsername(), employeeDTO.getId());
        return new ResponseEntity<>(employeeDTO ,HttpStatus.OK);
    }

    /**
     * Available for: ROLE_ADMIN
     * This function is used to create and save a new employee.
     * Firstly, we validate the fields of the employeeCuDTO, using the validateEmployee function.
     *      If the information are not valid, we return an HttpStatus with the correspond code to the user.
     * Secondly, we create an employee object, and we set all the attributes.
     * Thirdly, we can save the employee in the database.
     * Finally, we can convert the employee as an EmployeeDTO, we can add links,
     * and we can return data to the user with an HttpStatus.CREATED:
     * @param user Corresponds to the authenticated user
     * @param employeeCuDTO Corresponds to the employee information that the user wants to create.
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if it exists.
     */
    @PostMapping(produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<EmployeeDTO> createEmployee(@AuthenticationPrincipal Employee user,
                                                                    @RequestBody EmployeeCuDTO employeeCuDTO) {
        log.info("User {} is requesting to create a new employee with email: '{}'.",
                user.getUsername(), employeeCuDTO.getEmail());
        Pair<HttpStatus, String> validation = validateEmployee(employeeCuDTO, false);
        if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
            log.info("User {} requested to create a new employee with email: '{}'. {}",
                    user.getUsername(), employeeCuDTO.getEmail(), validation.getSecond());
            return new ResponseEntity<>(validation.getFirst());
        }
        Employee employee = new Employee();
        employee.setEmail(employeeCuDTO.getEmail());
        employee.setUsername(employeeCuDTO.getUserName());
        employee.setFirstName(employeeCuDTO.getFirstName());
        employee.setLastName(employeeCuDTO.getLastName());
        employee.setPassword(passwordEncoder.encode(employeeCuDTO.getPassword()));
        employee.setActive(false);
        employee.setBlocked(true);
        Optional<Role> roleOptional = rRepository.findByName("ROLE_VENDOR");
        if (roleOptional.isEmpty()) {
            log.info("User {} requested to create a new employee with email: '{}'. DEFAULT ROLE NOT FOUND.",
                    user.getUsername(), employee.getEmail());
        }
        employee.addRole(roleOptional.get());
        log.warn("User {} requested to create a new employee with email: {}. SAVING EMPLOYEE.",
                user.getUsername(), employee.getEmail());
        Employee savedEmployee = eRepository.save(employee);
        EmployeeDTO employeeDTO = EmployeeDTO.convert(savedEmployee);
        createHATEOAS(employeeDTO);
        log.info("user {} requested to create a new employee with email: {}. EMPLOYEE CREATED AND SAVED.",
                user.getUsername(), savedEmployee.getEmail());
        return new ResponseEntity<>(employeeDTO, HttpStatus.CREATED);
    }

    /**
     * Available for: ROLE_ADMIN
     * This function is used to update an employee by his username.
     * Firstly, we find the employee in the database using the employee repository.
     * Secondly, we check that the optional returned by the previous step is empty
     *      If it is empty, we return an HttpStatus.NO_CONTENT to the user.
     * Thirdly, we validate the new employee's information that the user gives.
     * Fourthly, we can update the fields and save the modification in the database.
     * Finally, we can convert the employee as an EmployeeDTO,
     * add HATEOAS links and return an HttStatus.ACCEPTED to the user.
     * @param id Corresponds to the username of the employee the user wants to update.
     * @param employeeCuDTO Corresponds to the new employee's information.
     * @param user Corresponds to the authenticated user
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if it exists.
     */
    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<EmployeeDTO> updateEmployee(@PathVariable(value = "id") Long id,
                                                      @RequestBody EmployeeCuDTO employeeCuDTO,
                                                      @AuthenticationPrincipal Employee user) {
        log.info("User {} requested to update the employee with id: {}", "AUTH IS NOT SET", id);
        Optional<Employee> employeeOptional = eRepository.findById(id);
        if (employeeOptional.isEmpty()) {
            log.info("User {} requested to update the employee with id: {}. NO DATA FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Pair<HttpStatus, String> validation = validateEmployee(employeeCuDTO, true);
        if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
            log.info("User {} requested to update the employee with id: {}. {}",
                    user.getUsername(), id, validation.getSecond());
            return new ResponseEntity<>(validation.getFirst());
        }
        Employee employee = employeeOptional.get();
        if (!employeeCuDTO.getEmail().equals(employee.getEmail())
                || !employeeCuDTO.getUserName().equals(employee.getUsername())) {
            log.info("User {} requested to update the employee with id: {}. USERNAME OR EMAIL IS NOT CORRECT.",
                    user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }
        employee.setFirstName(employeeCuDTO.getFirstName());
        employee.setLastName(employeeCuDTO.getLastName());
        log.warn("User {} requested to update the employee with id: {}. UPDATING EMPLOYEE.", user.getUsername(), id);
        Employee savedEmployee = eRepository.save(employee);
        EmployeeDTO employeeDTO = EmployeeDTO.convert(savedEmployee);
        createHATEOAS(employeeDTO);
        log.info("User {} requested to update the employee with id: {}. EMPLOYEE UPDATED.", user.getUsername(), id);
        return new ResponseEntity<>(employeeDTO, HttpStatus.ACCEPTED);
    }

    /**
     * Available for: ROLE_ADMIN
     * This function is used to re-activate an employee account by the employee id.
     * Firstly, we check that the employee account exists by using the employee repository.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Secondly, we change setActive to true and we setBlocked to false, and we save the modification.
     * Finally, we convert the employee as an EmployeeDTO and we return it to the user with an HttpStatus.ACCPETED.
     * @param id Corresponds to the id to the user to activate.
     * @param user Corresponds to the authenticated user
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if it exists.
     */
    @PutMapping(value = "/{id}/activate", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<EmployeeDTO> activateEmployee(@PathVariable(value = "id") Long id,
                                                        @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to activate the employee with id: {}.", user.getUsername(), id);
        Optional<Employee> employeeOptional = eRepository.findById(id);
        if (employeeOptional.isEmpty()) {
            log.info("User {} requested to activate the employee with id: {}. NO DATA FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Employee employee = employeeOptional.get();
        employee.setActive(true);
        employee.setBlocked(false);
        log.warn("User {} requested to activate the employee with id: {}. ACTIVATING EMPLOYEE.", user.getUsername(), id);
        Employee savedEmployee = eRepository.save(employee);
        EmployeeDTO employeeDTO = EmployeeDTO.convert(savedEmployee);
        createHATEOAS(employeeDTO);
        log.info("User {} requested to activate the employee with id: {}. EMPLOYEE UPDATED.", user.getUsername(), id);
        return new ResponseEntity<>(employeeDTO, HttpStatus.ACCEPTED);
    }

    /**
     * Available for: ROLE_ADMIN
     * This function is used to "delete" an employee by his id.
     * Firstly, we check that an employee exists with the given id.
     *      If not, we return an HttpStatus.NO_CONTENT to the user.
     * Secondly, we set the employee as blocked and activate false. Then, we can save the data.
     * Finally, we return to the user an HttpStatus.ACCEPTED.
     * @param id Corresponds to the id to the user to "delete".
     * @param user Corresponds to the authenticated user
     * @return Corresponds to a ResponseEntity that contains the HttpStatus and data if it exists.
     */
    @DeleteMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<EmployeeDTO> deleteEmployeeById(@PathVariable(value = "id") Long id,
                                                                        @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to delete the employee with id: {}.", user.getUsername(), id);
        if (!eRepository.existsById(id)) {
            log.info("User {} requested to delete the employee with id: {}. NO DATA FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        log.debug("User {} requested to delete the employee with id: {}. BLOCKING EMPLOYEE", user.getUsername(), id);
        eRepository.blockEmployeeById(id);
        log.info("User {} requested to delete the employee with id: {}. EMPLOYEE BLOCKED.", user.getUsername(), id);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
