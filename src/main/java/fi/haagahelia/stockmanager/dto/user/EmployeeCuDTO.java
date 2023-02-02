package fi.haagahelia.stockmanager.dto.user;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EmployeeCuDTO {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private String email;
    private String userName;
    private String firstName;
    private String lastName;
    private String password;
}
