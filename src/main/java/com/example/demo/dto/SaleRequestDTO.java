package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequestDTO {

    @Valid
    @NotEmpty(message = "La venta debe incluir al menos un producto")
    @Size(max = 100, message = "Una venta no puede contener más de 100 productos")
    private List<SaleItemRequestDTO> items;

}
