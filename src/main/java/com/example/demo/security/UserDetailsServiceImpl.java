package com.example.demo.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    // Password: "admin123" encriptado con BCrypt
	private static final Map<String, String> USERS = Map.of(
	        "admin", "$2a$10$eMSPwJfxqXRuTXFmPRKy2.CvXAMpOQ1q3Wgvc4cOkli9RuOBK5Cge"
	);

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String encodedPassword = USERS.get(username);
        if (encodedPassword == null) {
            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
        }
        return User.builder()
                .username(username)
                .password(encodedPassword)
                .roles("ADMIN")
                .build();
    }
}
