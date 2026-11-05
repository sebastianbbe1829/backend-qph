package com.example.demo.service;

import com.example.demo.dto.UserCreateRequestDTO;
import com.example.demo.dto.UserResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    Page<UserResponseDTO> findAll(Pageable pageable, String search);

    UserResponseDTO create(UserCreateRequestDTO request);
}
