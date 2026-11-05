package com.example.demo.service;

import com.example.demo.dto.UserCreateRequestDTO;
import com.example.demo.dto.UserResponseDTO;

import java.util.List;

public interface UserService {

    List<UserResponseDTO> findAll();

    UserResponseDTO create(UserCreateRequestDTO request);
}
