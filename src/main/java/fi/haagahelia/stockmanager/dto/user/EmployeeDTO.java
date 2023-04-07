package fi.haagahelia.stockmanager.dto.user;

import fi.haagahelia.stockmanager.model.user.Employee;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Builder
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeDTO extends RepresentationModel<EmployeeDTO> {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Long id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    // private String password;
    private Boolean isActive;
    private Boolean isBlocked;

    /* ------------------------------------------------- CONVERTORS ------------------------------------------------- */

    public static EmployeeDTO convert(Employee employee) {
        return builder()
                .id(employee.getId())
                .email(employee.getEmail())
                .username(employee.getUsername())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                // .password(employee.getPassword())
                .isActive(employee.getActive())
                .isBlocked(employee.getBlocked())
                .build();
    }
}
