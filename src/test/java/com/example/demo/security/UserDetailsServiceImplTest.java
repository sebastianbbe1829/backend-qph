package com.example.demo.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDetailsServiceImplTest {

    private final UserDetailsServiceImpl userDetailsService = new UserDetailsServiceImpl();

    @Test
    void userAccountHasOnlyUserRoleAndUsesBcryptPassword() {
        UserDetails user = userDetailsService.loadUserByUsername("user");

        assertEquals("user", user.getUsername());
        assertEquals(1, user.getAuthorities().size());
        assertTrue(user.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
        assertTrue(new BCryptPasswordEncoder().matches("user123", user.getPassword()));
    }

    @Test
    void unknownUserIsRejected() {
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown"));
    }
}
