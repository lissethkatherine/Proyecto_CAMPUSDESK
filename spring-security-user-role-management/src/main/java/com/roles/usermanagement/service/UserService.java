package com.roles.usermanagement.service;

import com.roles.usermanagement.dto.UserRegistrationDTO;
import com.roles.usermanagement.dto.UserResponseDTO;
import com.roles.usermanagement.entity.Role;
import com.roles.usermanagement.entity.User;
import com.roles.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Servicio de negocio para la gestión de usuarios.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registra un nuevo usuario en el sistema.
     * Encripta la contraseña con BCrypt y asigna estrictamente el rol USER.
     *
     * @param registrationDTO DTO con la información de registro.
     * @return DTO con la información del usuario creado.
     */
    @Transactional
    public UserResponseDTO registrarUsuario(UserRegistrationDTO registrationDTO) {
        // 1. Validar unicidad del email
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("El correo electrónico ya se encuentra registrado.");
        }

        // 2. Encriptar contraseña usando BCrypt
        String passwordEncriptada = passwordEncoder.encode(registrationDTO.getPassword());

        // 3. Crear entidad forzando el rol USER por defecto
        User nuevoUsuario = User.builder()
                .name(registrationDTO.getName())
                .email(registrationDTO.getEmail())
                .password(passwordEncriptada)
                .role(Role.USER) // Asignación estricta del rol USER por defecto
                .build();

        // 4. Persistir en PostgreSQL
        User usuarioGuardado = userRepository.save(nuevoUsuario);

        // 5. Mapear y retornar DTO de respuesta
        return UserResponseDTO.builder()
                .id(usuarioGuardado.getId())
                .name(usuarioGuardado.getName())
                .email(usuarioGuardado.getEmail())
                .role(usuarioGuardado.getRole())
                .build();
    }

    /**
     * Busca un usuario en la base de datos por su email.
     *
     * @param email Correo del usuario.
     * @return Optional con el usuario si existe.
     */
    @Transactional(readOnly = true)
    public Optional<User> buscarPorEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
