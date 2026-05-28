package ec.org.cedia.smartinventory.mcp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ListProductsToolInput(
        @Min(value = 0, message = "La página no puede ser negativa") Integer page,
        @Min(value = 1, message = "El tamaño debe ser mayor a 0")
        @Max(value = 100, message = "El tamaño no puede ser mayor a 100") Integer size) {
}
