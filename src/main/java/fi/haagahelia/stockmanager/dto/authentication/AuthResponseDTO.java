package fi.haagahelia.stockmanager.dto.authentication;


import lombok.*;

@Data
public class AuthResponseDTO {
    @NonNull
    private String token;
    private String tokenType = "Bearer ";
}
