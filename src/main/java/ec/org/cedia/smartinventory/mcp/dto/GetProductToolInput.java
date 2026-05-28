package ec.org.cedia.smartinventory.mcp.dto;

import jakarta.validation.constraints.NotNull;

public record GetProductToolInput(
        @NotNull(message = "El id es obligatorio") Long id) {
}
