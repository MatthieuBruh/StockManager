package fi.haagahelia.stockmanager.dto.common;

import lombok.Data;
import lombok.NonNull;

@Data
public class BodyMessage {
    @NonNull
    private String errorCode;
    @NonNull
    private String message;
}
