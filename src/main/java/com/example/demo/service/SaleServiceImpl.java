package com.example.demo.service;

import com.example.demo.dto.SaleRequestDTO;
import com.example.demo.dto.SaleItemRequestDTO;
import com.example.demo.dto.SaleItemResponseDTO;
import com.example.demo.dto.SaleResponseDTO;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Product;
import com.example.demo.entity.Sale;
import com.example.demo.entity.SaleItem;
import com.example.demo.exception.InsufficientStockException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SaleResponseDTO createSale(SaleRequestDTO request, String username) {
        List<SaleItemRequestDTO> requestedItems = resolveItems(request);
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
        Map<Long, SaleItemRequestDTO> itemsByProductId = new TreeMap<>();
        for (SaleItemRequestDTO item : requestedItems) {
            if (item.getProductId() == null || item.getProductId() <= 0
                    || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Cada producto debe tener un id y una cantidad positiva");
            }
            if (itemsByProductId.putIfAbsent(item.getProductId(), item) != null) {
                throw new IllegalArgumentException("No repitas productos en una misma venta");
            }
        }

        Map<Long, Product> lockedProducts = new TreeMap<>();
        for (Long productId : itemsByProductId.keySet()) {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado con id: " + productId));
            int quantity = itemsByProductId.get(productId).getQuantity();
            if (product.getStock() < quantity) {
                throw new InsufficientStockException(
                        "Stock insuficiente para el producto con id: " + productId);
            }
            lockedProducts.put(productId, product);
        }

        BigDecimal saleTotal = BigDecimal.ZERO;
        LocalDateTime createdAt = LocalDateTime.now();
        Sale sale = Sale.builder().createdAt(createdAt).user(user).items(new ArrayList<>()).build();
        List<SaleItemResponseDTO> responseItems = new ArrayList<>();
        for (Map.Entry<Long, SaleItemRequestDTO> entry : itemsByProductId.entrySet()) {
            Product product = lockedProducts.get(entry.getKey());
            int quantity = entry.getValue().getQuantity();
            BigDecimal unitPrice = product.getPrecio();
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            int remainingStock = product.getStock() - quantity;

            product.setStock(remainingStock);
            productRepository.save(product);
            sale.getItems().add(SaleItem.builder()
                    .sale(sale)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .total(itemTotal)
                    .build());
            responseItems.add(SaleItemResponseDTO.builder()
                    .productId(product.getId())
                    .productName(product.getNombre())
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .total(itemTotal)
                    .remainingStock(remainingStock)
                    .build());
            saleTotal = saleTotal.add(itemTotal);
        }

        Sale savedSale = saleRepository.save(sale);
        return SaleResponseDTO.builder()
                .saleId(savedSale.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .items(responseItems)
                .total(saleTotal)
                .createdAt(createdAt)
                .build();
    }

    private List<SaleItemRequestDTO> resolveItems(SaleRequestDTO request) {
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            return request.getItems();
        }
        if (request.getProductId() != null || request.getQuantity() != null) {
            return List.of(new SaleItemRequestDTO(request.getProductId(), request.getQuantity()));
        }
        throw new IllegalArgumentException("La venta debe incluir al menos un producto");
    }
}
