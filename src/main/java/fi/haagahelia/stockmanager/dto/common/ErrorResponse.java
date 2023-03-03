package fi.haagahelia.stockmanager.dto.common;

import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;

@Data
public class ErrorResponse {
    @NonNull
    private String status;
    @NonNull
    private String message;
    private LocalDateTime timestamp = LocalDateTime.now();
}
