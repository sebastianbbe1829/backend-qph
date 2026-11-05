package com.example.demo.service;

import com.example.demo.dto.UserCreateRequestDTO;
import com.example.demo.dto.UserResponseDTO;
import com.example.demo.entity.AppUser;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserResponseDTO create(UserCreateRequestDTO request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Ese nombre de usuario ya existe");
        }

        AppUser user = userRepository.save(AppUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build());

        return toResponse(user);
    }

    private UserResponseDTO toResponse(AppUser user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }
}
