package com.example.demo.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Map<String, Account> USERS = Map.of(
            "admin", new Account("$2a$10$eMSPwJfxqXRuTXFmPRKy2.CvXAMpOQ1q3Wgvc4cOkli9RuOBK5Cge", "ADMIN"),
            "user", new Account("$2a$10$0RJsNuZN7OOjcFfG/MGNIef7iRamr5/1SJBAQbhwLzJ27MsrGlfmW", "USER")
    );

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = USERS.get(username);
        if (account == null) {
            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
        }
        return User.withUsername(username)
                .password(account.encodedPassword())
                .roles(account.role())
                .build();
    }

    private record Account(String encodedPassword, String role) {}
}
