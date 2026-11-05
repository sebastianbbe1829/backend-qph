package com.example.demo.repository;

import com.example.demo.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Page<AppUser> findByUsernameContainingIgnoreCaseOrRoleContainingIgnoreCase(
            String username, String role, Pageable pageable);

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
