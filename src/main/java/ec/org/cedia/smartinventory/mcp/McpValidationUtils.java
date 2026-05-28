package ec.org.cedia.smartinventory.mcp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class McpValidationUtils {

    private final Validator validator;

    public <T> void validate(T target) {
        Set<ConstraintViolation<T>> violations = validator.validate(target);
        if (violations.isEmpty()) {
            return;
        }

        Map<String, String> fields = new LinkedHashMap<>();
        for (ConstraintViolation<T> violation : violations) {
            fields.put(violation.getPropertyPath().toString(), violation.getMessage());
        }

        throw new McpProtocolException(
                McpErrorCode.INVALID_PARAMS,
                "Parámetros inválidos",
                Map.of("fields", fields));
    }
}
