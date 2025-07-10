package store.piku.back.global.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

@Data
@AllArgsConstructor
public class ValidationErrorResponse {
    private final int status;
    private final Map<String, String> errors;
}
