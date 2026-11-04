package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleResponseDTO {

    private Long saleId;
    private Long userId;
    private String username;
    private List<SaleItemResponseDTO> items;
    private BigDecimal total;
    private LocalDateTime createdAt;
}
