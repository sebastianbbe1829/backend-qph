package com.example.demo.service;

import com.example.demo.dto.SaleRequestDTO;
import com.example.demo.dto.SaleResponseDTO;
import com.example.demo.entity.Product;
import com.example.demo.entity.Sale;
import com.example.demo.exception.InsufficientStockException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;

    @Override
    @Transactional
    public SaleResponseDTO createSale(SaleRequestDTO request) {
        Product product = productRepository.findByIdForUpdate(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado con id: " + request.getProductId()));

        if (product.getStock() < request.getQuantity()) {
            throw new InsufficientStockException(
                    "Stock insuficiente para el producto con id: " + product.getId());
        }

        int remainingStock = product.getStock() - request.getQuantity();
        product.setStock(remainingStock);
        productRepository.save(product);

        BigDecimal unitPrice = product.getPrecio();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));
        LocalDateTime createdAt = LocalDateTime.now();
        Sale sale = saleRepository.save(Sale.builder()
                .product(product)
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .total(total)
                .createdAt(createdAt)
                .build());

        return SaleResponseDTO.builder()
                .saleId(sale.getId())
                .productId(product.getId())
                .productName(product.getNombre())
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .total(total)
                .remainingStock(remainingStock)
                .createdAt(createdAt)
                .build();
    }
}
