package com.example.demo.service;

import com.example.demo.dto.UserCreateRequestDTO;
import com.example.demo.dto.UserResponseDTO;
import com.example.demo.entity.AppUser;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    @Test
    void listsUsersWithoutExposingPasswordHashes() {
        UserRepository userRepository = mock(UserRepository.class);
        UserServiceImpl userService = new UserServiceImpl(
                userRepository, new BCryptPasswordEncoder());
        when(userRepository.findAll()).thenReturn(java.util.List.of(
                AppUser.builder().id(1L).username("admin")
                        .passwordHash("$2a$hash").role("ADMIN").build(),
                AppUser.builder().id(2L).username("seller")
                        .passwordHash("$2a$hash").role("USER").build()));

        var users = userService.findAll();

        assertEquals(2, users.size());
        assertEquals("admin", users.get(0).getUsername());
        assertEquals("ADMIN", users.get(0).getRole());
        assertFalse(users.get(0).toString().contains("passwordHash"));
        assertFalse(users.get(0).toString().contains("$2a$hash"));
    }

    @Test
    void createsUserWithHashedPasswordAndDoesNotReturnHash() {
        UserRepository userRepository = mock(UserRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        UserServiceImpl userService = new UserServiceImpl(userRepository, passwordEncoder);
        UserCreateRequestDTO request = new UserCreateRequestDTO();
        request.setUsername("new.user");
        request.setPassword("secure-pass");
        request.setRole("USER");
        when(userRepository.existsByUsername("new.user")).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            return AppUser.builder().id(5L).username(user.getUsername())
                    .passwordHash(user.getPasswordHash()).role(user.getRole()).build();
        });

        UserResponseDTO response = userService.create(request);

        assertEquals("new.user", response.getUsername());
        assertEquals("USER", response.getRole());
        assertFalse(response.toString().contains("secure-pass"));
        verify(userRepository).save(any(AppUser.class));
        org.mockito.ArgumentCaptor<AppUser> savedUser =
                org.mockito.ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(savedUser.capture());
        assertTrue(passwordEncoder.matches("secure-pass", savedUser.getValue().getPasswordHash()));
    }
}
