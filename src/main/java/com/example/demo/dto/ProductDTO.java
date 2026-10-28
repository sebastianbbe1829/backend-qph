package com.example.demo.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private Long id;

    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    @NotNull
    @Positive(message = "El precio debe ser positivo")
    private BigDecimal precio;

    @NotNull
    @Min(0)
    private Integer stock;
}
