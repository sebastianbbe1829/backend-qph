package com.example.demo.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserResponseDTO {
    Long id;
    String username;
    String role;
}
