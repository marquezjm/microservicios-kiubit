package com.kubit.authservice.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Schema(name = "RegisterRequest", description = "DTO para registro de usuario. Campos obligatorios: name, email, password. Nombre debe ser único para usuario.")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {
    @Schema(description = "Nombre real del usuario", example = "Juan Pérez")
    @NotBlank
    private String name;

    @Schema(description = "Edad del usuario", example = "22")
    @Min(1)
    private Integer age;

    @Schema(description = "Email único del usuario", example="juan@example.com")
    @Email
    @NotBlank
    private String email;

    @Schema(description = "Contraseña en texto plano", example = "MiPassw0rd!")
    @NotBlank
    private String password;
}
