package fi.haagahelia.stockmanager.dto.user;


import fi.haagahelia.stockmanager.model.user.Role;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class RoleDTO extends RepresentationModel<RoleDTO> {

    /* --------------------------------------------------- FIELDS --------------------------------------------------- */

    private Long id;
    private String name;
    private String description;

    /* ------------------------------------------------- CONVERTORS ------------------------------------------------- */

    public static RoleDTO convert(Role role) {
        return builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .build();
    }
}
