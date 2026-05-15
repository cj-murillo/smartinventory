package ec.org.cedia.smartinventory.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data           // ← getters, setters, equals, hashCode, toString
@Builder        // ← patrón builder
@NoArgsConstructor  // ← constructor vacío
@AllArgsConstructor // ← constructor completo
public class Product {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private Boolean active;
}
