package com.example.demo.config;

import com.example.demo.entity.AppUser;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapTest {

    @Test
    void createsHashedAdminOnlyWhenUserTableIsEmpty() {
        UserRepository userRepository = mock(UserRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AdminBootstrap bootstrap = new AdminBootstrap(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(bootstrap, "username", "initial.admin");
        ReflectionTestUtils.setField(bootstrap, "password", "secure-password");
        when(userRepository.count()).thenReturn(0L);

        bootstrap.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<AppUser> savedUser = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(savedUser.capture());
        assertTrue(passwordEncoder.matches("secure-password", savedUser.getValue().getPasswordHash()));
        assertEquals("ADMIN", savedUser.getValue().getRole());
    }

    @Test
    void refusesToRunWhenUsersAlreadyExist() {
        UserRepository userRepository = mock(UserRepository.class);
        AdminBootstrap bootstrap = new AdminBootstrap(userRepository, new BCryptPasswordEncoder());
        ReflectionTestUtils.setField(bootstrap, "username", "initial.admin");
        ReflectionTestUtils.setField(bootstrap, "password", "secure-password");
        when(userRepository.count()).thenReturn(1L);

        assertThrows(IllegalStateException.class,
                () -> bootstrap.run(new DefaultApplicationArguments(new String[0])));
        verify(userRepository, never()).save(any(AppUser.class));
    }
}
