package com.roles.usermanagement.repository;

import com.roles.usermanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para operaciones CRUD y consultas sobre la entidad User.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por su dirección de correo electrónico.
     *
     * @param email Email del usuario.
     * @return Optional con el usuario si existe.
     */
    Optional<User> findByEmail(String email);

    /**
     * Comprueba si ya existe un usuario registrado con el correo dado.
     *
     * @param email Email a validar.
     * @return true si el email ya se encuentra registrado.
     */
    boolean existsByEmail(String email);
}
