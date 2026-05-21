package ec.org.cedia.smartinventory.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;          // ← NUEVO
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity                                // ← NUEVO
@Table(name = "products")             // ← NUEVO
public class Product {

    @Id                                // ← NUEVO
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ← NUEVO
    private Long id;

    private String name;
    
    // @Transient
    private String description;

    private Double price;
    @JsonIgnore
    private Integer stock;
    private Boolean active;
    @Column(name = "is_available")  // ← NUEVO
    private String isAvailable;
}