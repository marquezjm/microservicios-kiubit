package com.kubit.authservice.service;

import com.kubit.authservice.domain.entity.RegisterRequest;
import com.kubit.authservice.domain.entity.UserLoginResponse;
import com.kubit.authservice.domain.entity.AuthUser;
import com.kubit.authservice.domain.entity.RegisterRequest;
import com.kubit.authservice.domain.entity.LoginRequest;
import com.kubit.authservice.domain.entity.RefreshToken;

public interface AuthService {
    AuthUser register(RegisterRequest request); // Ajustado para retornar el usuario creado

    UserLoginResponse login(LoginRequest request); // Login y devolución de AuthUser o token

    UserLoginResponse refreshToken(String refreshToken);

    void logout(String refreshToken);

    void logoutAllForUser(Long userId);

    boolean validateJwt(String jwt);
}

