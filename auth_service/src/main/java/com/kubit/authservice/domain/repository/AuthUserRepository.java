package com.kubit.authservice.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kubit.authservice.domain.entity.AuthUser;

import java.util.Optional;


@Service
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
    
    Optional<AuthUser> findByEmail(String email);

    boolean existsByEmail(String email);
}