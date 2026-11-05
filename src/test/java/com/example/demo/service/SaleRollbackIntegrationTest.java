package com.example.demo.service;

import com.example.demo.dto.SaleItemRequestDTO;
import com.example.demo.dto.SaleRequestDTO;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Product;
import com.example.demo.entity.Sale;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sale_rollback_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS qph_prueba"
})
@Import(SaleRollbackIntegrationTest.RepositoryFailureConfiguration.class)
class SaleRollbackIntegrationTest {

    @Autowired
    private SaleService saleService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private UserRepository userRepository;

    private Long productId;

    @BeforeEach
    void setUp() {
        reset(saleRepository);
        userRepository.deleteAll();
        productRepository.deleteAll();

        userRepository.save(AppUser.builder()
                .username("seller")
                .passwordHash("$2a$10$test")
                .role("USER")
                .build());
        productId = productRepository.save(Product.builder()
                .nombre("Producto de prueba")
                .precio(new BigDecimal("10.00"))
                .stock(3)
                .build())
                .getId();
        when(saleRepository.save(any(Sale.class)))
                .thenThrow(new IllegalStateException("Fallo de persistencia simulado"));
    }

    @Test
    void rollsBackStockChangesWhenSalePersistenceFails() {
        assertThrows(InvalidDataAccessApiUsageException.class, () -> saleService.createSale(
                new SaleRequestDTO(List.of(new SaleItemRequestDTO(productId, 2))), "seller"));

        assertEquals(3, productRepository.findById(productId).orElseThrow().getStock());
        verify(saleRepository).save(any(Sale.class));
    }

    @TestConfiguration
    static class RepositoryFailureConfiguration {

        @Bean
        @Primary
        SaleRepository failingSaleRepository() {
            return mock(SaleRepository.class);
        }
    }
}
