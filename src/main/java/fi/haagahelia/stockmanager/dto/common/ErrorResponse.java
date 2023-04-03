package fi.haagahelia.stockmanager.dto.common;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fi.haagahelia.stockmanager.serialization.LocalDateTimeDeserializer;
import fi.haagahelia.stockmanager.serialization.LocalDateTimeSerializer;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;


@Data
public class ErrorResponse {
    @NonNull
    private String status;
    @NonNull
    private String message;

    // Source: https://stackoverflow.com/questions/27952472/serialize-deserialize-java-8-java-time-with-jackson-json-mapper
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime timestamp = LocalDateTime.now();
}
