package com.example.demo.service;

import com.example.demo.dto.SaleRequestDTO;
import com.example.demo.dto.SaleResponseDTO;
import com.example.demo.entity.Product;
import com.example.demo.entity.Sale;
import com.example.demo.exception.InsufficientStockException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaleServiceImplTest {

    private ProductRepository productRepository;
    private SaleRepository saleRepository;
    private SaleServiceImpl saleService;
    private Product product;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        saleRepository = mock(SaleRepository.class);
        saleService = new SaleServiceImpl(productRepository, saleRepository);
        product = Product.builder()
                .id(1L)
                .nombre("Teclado")
                .precio(new BigDecimal("12.50"))
                .stock(5)
                .build();
    }

    @Test
    void createSaleDecreasesStockAndReturnsTotal() {
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(saleRepository.save(org.mockito.ArgumentMatchers.any(Sale.class)))
                .thenAnswer(invocation -> {
                    Sale sale = invocation.getArgument(0);
                    return Sale.builder()
                            .id(10L)
                            .product(sale.getProduct())
                            .quantity(sale.getQuantity())
                            .unitPrice(sale.getUnitPrice())
                            .total(sale.getTotal())
                            .createdAt(sale.getCreatedAt())
                            .build();
                });

        SaleResponseDTO response = saleService.createSale(new SaleRequestDTO(1L, 2));

        assertEquals(3, response.getRemainingStock());
        assertEquals(new BigDecimal("25.00"), response.getTotal());
        assertEquals(10L, response.getSaleId());
        assertEquals(3, product.getStock());
        verify(productRepository).save(product);
        verify(saleRepository).save(org.mockito.ArgumentMatchers.any(Sale.class));
    }

    @Test
    void createSaleRejectsQuantityAboveAvailableStock() {
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class,
                () -> saleService.createSale(new SaleRequestDTO(1L, 6)));

        assertEquals(5, product.getStock());
        verify(productRepository, never()).save(product);
        verify(saleRepository, never()).save(org.mockito.ArgumentMatchers.any(Sale.class));
    }

    @Test
    void createSaleRejectsUnknownProduct() {
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> saleService.createSale(new SaleRequestDTO(1L, 1)));

        verify(productRepository, never()).save(product);
        verify(saleRepository, never()).save(org.mockito.ArgumentMatchers.any(Sale.class));
    }
}
