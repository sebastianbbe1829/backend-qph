package com.example.demo.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Map<String, UserDetails> USERS = Map.of(
            "admin", User.withUsername("admin")
                    .password("$2a$10$eMSPwJfxqXRuTXFmPRKy2.CvXAMpOQ1q3Wgvc4cOkli9RuOBK5Cge")
                    .roles("ADMIN")
                    .build(),
            "user", User.withUsername("user")
                    .password("$2a$10$0RJsNuZN7OOjcFfG/MGNIef7iRamr5/1SJBAQbhwLzJ27MsrGlfmW")
                    .roles("USER")
                    .build()
    );

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = USERS.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
        }
        return user;
    }
}
