package com.example.demo.service;

import com.example.demo.dto.SaleItemRequestDTO;
import com.example.demo.dto.SaleRequestDTO;
import com.example.demo.dto.SaleResponseDTO;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Product;
import com.example.demo.entity.Sale;
import com.example.demo.exception.InsufficientStockException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaleServiceImplTest {

    private ProductRepository productRepository;
    private SaleRepository saleRepository;
    private UserRepository userRepository;
    private SaleServiceImpl saleService;
    private Product keyboard;
    private Product mouse;
    private AppUser seller;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        saleRepository = mock(SaleRepository.class);
        userRepository = mock(UserRepository.class);
        saleService = new SaleServiceImpl(productRepository, saleRepository, userRepository);
        keyboard = Product.builder()
                .id(1L).nombre("Teclado")
                .precio(new BigDecimal("12.50")).stock(5).build();
        mouse = Product.builder()
                .id(2L).nombre("Mouse")
                .precio(new BigDecimal("7.25")).stock(3).build();
        seller = AppUser.builder().id(7L).username("user").role("USER").build();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(seller));
    }

    @Test
    void createsOneSaleWithMultipleItemsAndAttributesSeller() {
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(keyboard));
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(mouse));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale sale = invocation.getArgument(0);
            return Sale.builder().id(10L).createdAt(sale.getCreatedAt())
                    .user(sale.getUser()).items(sale.getItems()).build();
        });

        SaleResponseDTO response = saleService.createSale(new SaleRequestDTO(List.of(
                new SaleItemRequestDTO(1L, 2),
                new SaleItemRequestDTO(2L, 1)
        )), "user");

        assertEquals(10L, response.getSaleId());
        assertEquals(7L, response.getUserId());
        assertEquals("user", response.getUsername());
        assertEquals(new BigDecimal("32.25"), response.getTotal());
        assertEquals(2, response.getItems().size());
        assertEquals(3, response.getItems().get(0).getRemainingStock());
        assertEquals(2, response.getItems().get(1).getRemainingStock());
        assertEquals(3, keyboard.getStock());
        assertEquals(2, mouse.getStock());
        verify(productRepository).save(keyboard);
        verify(productRepository).save(mouse);
        verify(saleRepository).save(any(Sale.class));
    }

    @Test
    void rejectsWholeSaleIfAnyItemExceedsStock() {
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(keyboard));
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(mouse));

        assertThrows(InsufficientStockException.class, () -> saleService.createSale(
                new SaleRequestDTO(List.of(
                        new SaleItemRequestDTO(1L, 2),
                        new SaleItemRequestDTO(2L, 4)
                )), "user"));

        assertEquals(5, keyboard.getStock());
        assertEquals(3, mouse.getStock());
        verify(productRepository, never()).save(any(Product.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void rejectsUnknownProductWithoutPersistingSale() {
        when(productRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> saleService.createSale(
                new SaleRequestDTO(99L, 1), "user"));

        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void preservesLegacySingleProductRequestShape() {
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(keyboard));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale sale = invocation.getArgument(0);
            return Sale.builder().id(11L).createdAt(sale.getCreatedAt())
                    .user(sale.getUser()).items(sale.getItems()).build();
        });

        SaleResponseDTO response = saleService.createSale(new SaleRequestDTO(1L, 2), "user");

        assertEquals(1, response.getItems().size());
        assertEquals(new BigDecimal("25.00"), response.getTotal());
    }
}
