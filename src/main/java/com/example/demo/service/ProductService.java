package com.example.demo.service;

import com.example.demo.dto.ProductDTO;

import java.util.List;

public interface ProductService {

    List<ProductDTO> findAll();

    ProductDTO findById(Long id);

    ProductDTO create(ProductDTO dto);

    ProductDTO update(Long id, ProductDTO dto);

    void delete(Long id);
}
