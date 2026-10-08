package com.roles.usermanagement.dto;

import com.roles.usermanagement.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para exponer los datos públicos del usuario sin la contraseña.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private Long id;
    private String name;
    private String email;
    private Role role;
}
