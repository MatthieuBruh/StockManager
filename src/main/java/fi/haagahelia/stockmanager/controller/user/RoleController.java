package fi.haagahelia.stockmanager.controller.user;


import fi.haagahelia.stockmanager.dto.user.RoleCuDTO;
import fi.haagahelia.stockmanager.dto.user.RoleDTO;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
import fi.haagahelia.stockmanager.repository.user.RoleRepository;
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
@RequestMapping("/api/roles")
public class RoleController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final RoleRepository rRepository;

    @Autowired
    public RoleController(RoleRepository rRepository) {
        this.rRepository = rRepository;
    }

    /* ---------------------------------------------------- TOOLS --------------------------------------------------- */

    /**
     * This method is used to create and add the HATEOAS links to a DTO models.
     * @param roleDTO The dto model to which we will add the HATEOAS links.
     * @return The dto model with the HATEOAS links.
     */
    private RoleDTO createHATEOAS(RoleDTO roleDTO) {
        Link selfLink = linkTo(RoleController.class).slash(String.valueOf(roleDTO.getId())).withSelfRel();
        roleDTO.add(selfLink);
        Link collectionLink = linkTo(methodOn(RoleController.class)).slash("").withRel("roles");
        roleDTO.add(collectionLink);
        return roleDTO;
    }

    /**
     * This function is used to validate the role fields before the creation.
     * We check the following constraints:
     *      - Name is not null and not empty
     *      - Name is not already used.
     * @param roleCuDTO Corresponds to the data of the role to validate.
     * @return A Pair object that contains an HttpStatus and the decision reason.
     */
    private Pair<HttpStatus, String> validateRole(RoleCuDTO roleCuDTO) {
        if (roleCuDTO.getName() == null) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "NAME IS NULL.");
        if (roleCuDTO.getName().length() < 1) return Pair.of(HttpStatus.UNPROCESSABLE_ENTITY, "NAME IS EMPTY.");
        if (rRepository.existsByName(roleCuDTO.getName())) return Pair.of(HttpStatus.CONFLICT, "ALREADY EXISTS");
        return Pair.of(HttpStatus.ACCEPTED, "");
    }

    /* ------------------------------------------------- API METHODS ------------------------------------------------ */

    /**
     * Available for : ROLE_ADMIN
     * This function is used to get all the roles that are saved in the database.
     * Firstly, we will SELECT all the roles that are in the database.
     * Secondly, we check that the list is not empty.
     * If the list is empty, we return an empty ResponseEntity with an HttpStatus.NO_CONTENT.
     * If the list contains at least one object, we can do the third step.
     * Thirdly, we transform all the roles as a RoleDTO objects. We also add the HATEOAS links.
     * Finally, we return the data in a ResponseEntity with the HttpStatus.OK.
     * @param user Corresponds to the user that is authenticated.
     * @return ResponseEntity that contains data and the corresponding HttpStatus.
     */
    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<List<RoleDTO>> getAllRoles(@AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting all the roles from the database.", user.getUsername());
        List<Role> roles = rRepository.findAll();
        if (roles.size() < 1) {
            log.info("User {} requested all roles. NO DATA FOUND.", user.getUsername());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        List<RoleDTO> roleDTOS = new ArrayList<>();
        for (Role role : roles) {
            RoleDTO roleDTO = RoleDTO.convert(role);
            roleDTOS.add(createHATEOAS(roleDTO));
        }
        log.info("User {} requested all roles. RETURNING DATA.", user.getUsername());
        return new ResponseEntity<>(roleDTOS, HttpStatus.OK);
    }

    /**
     * Available for : ROLE_ADMIN
     * This function is used to get the role that correspond to a given id.
     * Firstly, we will SELECT the data in the database.
     * Secondly, we verify that data is not empty. If data is empty, we return a HttpStatus.NO_CONTENT.
     * Thirdly we transform the role as a RoleDTO object, and we add the HATEOAS links to the object.
     * Finally, if everything went OK, we return the data to the user with a HttpStatus.OK.
     * @param id Correspond to the id of the role searched by the user.
     * @param user Corresponds to the user that is authenticated.
     * @return ResponseEntity with the data and the corresponding HttpStatus.
     */
    @GetMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<RoleDTO> getRoleById(@PathVariable(value = "id") Long id,
                                                             @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to get the role with id: {}.", user.getUsername(), id);
        Optional<Role> roleOptional = rRepository.findById(id);
        if (roleOptional.isEmpty()) {
            log.info("User {} requested the role with id: {}. NO DATA FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        RoleDTO roleDTO = RoleDTO.convert(roleOptional.get());
        createHATEOAS(roleDTO);
        log.info("User {} requested the role with id: {}. RETURNING DATA.", user.getUsername(), id);
        return new ResponseEntity<>(roleDTO, HttpStatus.OK);
    }

    /**
     * Available for : ROLE_ADMIN
     * This function is used to create and save a new role.
     * Firstly, we get the new role in the RequestBody.
     * Secondly, we check that the role's information are correct by using the validateRole function.
     *      If not, we return an HttpStatus with the corresponding code to the user.
     * Thirdly, we can create the role object and set all the values.
     * Fourthly, we can save the data in the database.
     * Finally, we can create a RoleDTO object, add the HATEOAS links,
     * and return it to the user with an HttpStatus.CREATED
     * @param roleCuDTO The role that the user want to save in the database.
     * @param user Corresponds to the user that is authenticated.
     * @return ResponseEntity with the data and the corresponding HttpStatus.
     */
    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<RoleDTO> createRole(@RequestBody RoleCuDTO roleCuDTO,
                                                            @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to create and save a new role with name: {}",
                user.getUsername(), roleCuDTO.getName());
        Pair<HttpStatus, String> validation = validateRole(roleCuDTO);
        if (!validation.getFirst().equals(HttpStatus.ACCEPTED)) {
            log.info("User {} requested to create and save a new role with name: {}. {}",
                    user.getUsername(), roleCuDTO.getName(), validation.getSecond());
            return new ResponseEntity<>(validation.getFirst());
        }
        Role role = new Role();
        role.setName(roleCuDTO.getName());
        role.setDescription(roleCuDTO.getDescription());
        log.warn("User {} requested to create and save a new role with name: {}. SAVING ROLE.",
                user.getUsername(), role.getName());
        Role savedRole = rRepository.save(role);
        RoleDTO roleDTO = RoleDTO.convert(savedRole);
        createHATEOAS(roleDTO);
        log.info("User {} requested to create and save a new role with name: {}. RETURNING DATA.",
                user.getUsername(), savedRole.getName());
        return new ResponseEntity<>(roleDTO, HttpStatus.CREATED);
    }

    /**
     * Available for : ROLE_ADMIN
     * This function is used to update an existing role by his id.
     * Firstly, we check that a role is already existing by selecting data in database.
     *      If it is not the case, we return a HttpStatus.NO_CONTENT.
     * Thirdly, we modify the role object that we found in the database, and we save the changes.
     * Finally, when everything is correct, we return the updated RoleDTO to the user.
     * @param id The id that correspond to the role to update.
     * @param roleCuDTO The new version of the role
     * @return ResponseEntity with the updated data and the corresponding HttpStatus.
     */
    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public @ResponseBody ResponseEntity<RoleDTO> updateRole(@PathVariable(value = "id") Long id,
                                                            @RequestBody RoleCuDTO roleCuDTO,
                                                            @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to update the role with id: {}.", user.getUsername(), id);
        Optional<Role> roleOptional = rRepository.findById(id);
        if (roleOptional.isEmpty()) {
            log.info("User {} requested to update the role with id: {}. NO DATA FOUND", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Role role = roleOptional.get();
        role.setDescription(roleCuDTO.getDescription());
        log.warn("User {} requested to update the role with id: {}. UPDATING DATA.", user.getUsername(), id);
        Role updatedRole = rRepository.save(role);
        RoleDTO roleDTO = RoleDTO.convert(updatedRole);
        createHATEOAS(roleDTO);
        log.info("User {} requested to update the role with id: {}. RETURNING DATA.", user.getUsername(), id);
        return new ResponseEntity<>(roleDTO, HttpStatus.ACCEPTED);
    }

    /**
     * Available for : ROLE_ADMIN
     * This function is used to delete a role by is ID.
     * Firstly, we will check that a role exists with the corresponding id.
     * If any role exists, we return a HttpStatus.NO_CONTENT to the user.
     * If the role exists, we delete it from the database. We finally return a HttpStatus.OK to the user.
     * @param id Correspond to the id of the role to be deleted.
     * @return Return an empty ResponseEntity with the corresponding HttpStatus code.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping(value = "/{id}", produces = "application/json")
    public @ResponseBody ResponseEntity<RoleDTO> deleteRole(@PathVariable(value = "id") Long id,
                                                            @AuthenticationPrincipal Employee user) {
        log.info("User {} is requesting to delete the role with id: {}.", user.getUsername(), id);
        if (!rRepository.existsById(id)) {
            log.info("User {} requested to delete the role with id: {}. NO DATA FOUND.", user.getUsername(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        log.debug("User {} requested to delete the role with id: {}. DELETING DATA.", user.getUsername(), id);
        rRepository.deleteById(id);
        log.info("User {} requested to delete the role with id: {}. ROLE DELETED.", user.getUsername(), id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
