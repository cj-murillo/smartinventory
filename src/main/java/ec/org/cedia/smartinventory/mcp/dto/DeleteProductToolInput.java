package ec.org.cedia.smartinventory.mcp.dto;

import jakarta.validation.constraints.NotNull;

public record DeleteProductToolInput(
        @NotNull(message = "El id es obligatorio") Long id) {
}
