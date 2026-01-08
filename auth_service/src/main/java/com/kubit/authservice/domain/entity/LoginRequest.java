package com.kubit.authservice.domain.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "LoginRequest", description = "DTO para login. Email, password obligatorios. Se recomienda enviar deviceId persistente.")
public class LoginRequest {

    @Schema(description = "Email de login", example = "juan@example.com")
    @Email
    @NotBlank
    private String email;

    @Schema(description = "Contraseña", example = "MiPassw0rd!")
    @NotBlank
    private String password;

    @Schema(description = "Identificador persistente de dispositivo", example = "dcd74a9a-5d6f-4bd5-a90f-60e935e5d74a")
    private String deviceId;
    
    @Schema(description = "IP del usuario extraída por backend", example = "189.230.11.1")
    private String ipAddress;

}
