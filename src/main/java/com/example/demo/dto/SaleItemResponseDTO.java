package com.example.demo.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class SaleItemResponseDTO {
    Long productId;
    String productName;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal total;
    Integer remainingStock;
}
