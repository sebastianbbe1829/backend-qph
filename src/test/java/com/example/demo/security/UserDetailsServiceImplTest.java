package com.example.demo.security;

import com.example.demo.entity.AppUser;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {

    private UserRepository userRepository;
    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDetailsService = new UserDetailsServiceImpl(userRepository);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(AppUser.builder()
                .id(1L).username("user").passwordHash(encoder.encode("user123"))
                .role("USER").build()));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(AppUser.builder()
                .id(2L).username("admin").passwordHash(encoder.encode("admin123"))
                .role("ADMIN").build()));
    }

    @Test
    void databaseAccountHasEncodedPasswordAndExpectedRole() {
        UserDetails user = userDetailsService.loadUserByUsername("user");

        assertAccountRole(user, "user", "ROLE_USER");
        assertTrue(new BCryptPasswordEncoder().matches("user123", user.getPassword()));
    }

    @Test
    void adminAccountHasAdminRole() {
        UserDetails admin = userDetailsService.loadUserByUsername("admin");

        assertAccountRole(admin, "admin", "ROLE_ADMIN");
        assertTrue(new BCryptPasswordEncoder().matches("admin123", admin.getPassword()));
    }

    @Test
    void unknownUserIsRejected() {
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown"));
    }

    @Test
    void accountsCanAuthenticateMoreThanOnceWithoutLosingTheirRoles() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(new BCryptPasswordEncoder());
        AuthenticationManager authenticationManager = new ProviderManager(List.of(provider));

        for (String[] credentials : List.of(
                new String[] {"user", "user123", "ROLE_USER"},
                new String[] {"admin", "admin123", "ROLE_ADMIN"})) {
            for (int attempt = 0; attempt < 2; attempt++) {
                UserDetails authenticated = (UserDetails) authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(
                                credentials[0], credentials[1]))
                        .getPrincipal();
                assertAccountRole(authenticated, credentials[0], credentials[2]);
            }
        }
    }

    private void assertAccountRole(UserDetails account, String username, String role) {
        assertEquals(username, account.getUsername());
        assertEquals(1, account.getAuthorities().size());
        assertTrue(account.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role)));
    }
}
