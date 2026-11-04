package com.example.demo.service;

import com.example.demo.dto.SaleRequestDTO;
import com.example.demo.dto.SaleResponseDTO;

public interface SaleService {

    SaleResponseDTO createSale(SaleRequestDTO request);
}
