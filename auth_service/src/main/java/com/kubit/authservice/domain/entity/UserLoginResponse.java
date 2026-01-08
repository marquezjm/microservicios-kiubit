package com.kubit.authservice.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "UserLoginResponse", description = "Respuesta estándar para login y refresh; devuelve usuario visible y tokens JWT.")
public class UserLoginResponse {
    @Schema(description = "Datos del usuario autenticado (sin hash de contraseña)")
    private AuthUser user;

    @Schema(description = "Nuevo JWT access token", example = "eyJhbGciOiJIUzI1NiIsIn...")
    private String accessToken;

    @Schema(description = "Nuevo refresh token persistente", example = "cc0aa3d0-9492-4...d32e")
    private String refreshToken;
}
