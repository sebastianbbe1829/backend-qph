package com.example.demo.service;

import com.example.demo.dto.SaleItemRequestDTO;
import com.example.demo.dto.SaleRequestDTO;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Product;
import com.example.demo.exception.InsufficientStockException;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sale_concurrency_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS qph_prueba"
})
class SaleConcurrencyIntegrationTest {

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
        saleRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(AppUser.builder()
                .username("seller")
                .passwordHash("$2a$10$test")
                .role("USER")
                .build());
        productId = productRepository.save(Product.builder()
                .nombre("Producto de prueba")
                .precio(new BigDecimal("10.00"))
                .stock(1)
                .build())
                .getId();
    }

    @Test
    void concurrentSalesCannotOversellTheLastUnit() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> firstSale = executor.submit(() -> attemptSale(ready, start));
            Future<Boolean> secondSale = executor.submit(() -> attemptSale(ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<Boolean> results = List.of(
                    firstSale.get(10, TimeUnit.SECONDS),
                    secondSale.get(10, TimeUnit.SECONDS));

            assertEquals(1, results.stream().filter(result -> result).count());
            assertEquals(0, productRepository.findById(productId).orElseThrow().getStock());
            assertEquals(1, saleRepository.count());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private boolean attemptSale(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            saleService.createSale(new SaleRequestDTO(List.of(
                    new SaleItemRequestDTO(productId, 1))), "seller");
            return true;
        } catch (InsufficientStockException ex) {
            return false;
        }
    }
}
