package com.example.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanProducts() {
        productRepository.deleteAll();
    }

    @Test
    void productCanBeCreatedReadUpdatedAndDeleted() throws Exception {
        String createdResponse = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Teclado",
                                  "precio": 49.99,
                                  "stock": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Teclado"))
                .andExpect(jsonPath("$.precio").value(49.99))
                .andExpect(jsonPath("$.stock").value(10))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long productId = objectMapper.readTree(createdResponse).get("id").asLong();

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Teclado"));

        mockMvc.perform(put("/api/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Teclado mecánico",
                                  "precio": 89.90,
                                  "stock": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Teclado mecánico"))
                .andExpect(jsonPath("$.precio").value(89.90))
                .andExpect(jsonPath("$.stock").value(5));

        mockMvc.perform(delete("/api/products/{id}", productId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRejectsInvalidProductData() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": " ",
                                  "precio": 0,
                                  "stock": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"))
                .andExpect(jsonPath("$.details.nombre").exists())
                .andExpect(jsonPath("$.details.precio").exists())
                .andExpect(jsonPath("$.details.stock").exists());
    }

    @Test
    void productListSupportsServerSidePaginationSortingAndSearch() throws Exception {
        productRepository.saveAll(java.util.List.of(
                Product.builder().nombre("Teclado").precio(new java.math.BigDecimal("10.00")).stock(5).build(),
                Product.builder().nombre("Mouse").precio(new java.math.BigDecimal("20.00")).stock(8).build(),
                Product.builder().nombre("Monitor").precio(new java.math.BigDecimal("30.00")).stock(2).build()));

        mockMvc.perform(get("/api/products")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "nombre")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].nombre").value("Monitor"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(true));

        mockMvc.perform(get("/api/products").param("search", "tecla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].nombre").value("Teclado"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void productListRejectsInvalidPaginationAndSortFields() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "101"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/products").param("sort", "password"))
                .andExpect(status().isBadRequest());
    }
}
