package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequestDTO {

    private Long productId;

    private Integer quantity;

    @Valid
    @Size(max = 100, message = "Una venta no puede contener más de 100 productos")
    private List<SaleItemRequestDTO> items;

    public SaleRequestDTO(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public SaleRequestDTO(List<SaleItemRequestDTO> items) {
        this.items = items;
    }
}
