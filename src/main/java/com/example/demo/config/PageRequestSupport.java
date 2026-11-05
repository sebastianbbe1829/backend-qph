package com.example.demo.config;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

public final class PageRequestSupport {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageRequestSupport() {
    }

    public static Pageable create(
            int page,
            int size,
            String sort,
            String direction,
            Set<String> allowedSortFields) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La página debe ser no negativa y el tamaño debe estar entre 1 y " + MAX_SIZE);
        }
        if (!allowedSortFields.contains(sort)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campo de ordenamiento no válido");
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dirección de ordenamiento no válida");
        }

        return PageRequest.of(page, size, Sort.by(sortDirection, sort));
    }
}
