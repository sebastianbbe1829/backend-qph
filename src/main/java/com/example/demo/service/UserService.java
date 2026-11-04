package com.example.demo.service;

import com.example.demo.dto.UserCreateRequestDTO;
import com.example.demo.dto.UserResponseDTO;

public interface UserService {

    UserResponseDTO create(UserCreateRequestDTO request);
}
