package campusdesk.controller;

import campusdesk.dto.LoginRequestDTO;
import campusdesk.dto.LoginResponseDTO;
import campusdesk.dto.UserRegistrationDTO;
import campusdesk.dto.UserResponseDTO;
import campusdesk.entity.User;
import campusdesk.security.JwtTokenProvider;
import campusdesk.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST que expone los endpoints públicos de registro e inicio de sesión.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Endpoint público para registrar usuarios (/auth/register).
     *
     * @param registrationDTO Datos requeridos para la creación de cuenta.
     * @return 201 Created con la información del usuario creado.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        UserResponseDTO response = userService.registerUser(registrationDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint público para iniciar sesión (/auth/login).
     * Valida credenciales, genera el JWT con el rol y retorna la respuesta en formato JSON.
     *
     * @param loginDTO Credenciales enviadas (email y password).
     * @return 200 OK con el token y el rol, o 401 Unauthorized si las credenciales son incorrectas.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginDTO) {
        User user = userService.findByEmail(loginDTO.getEmail())
                .orElse(null);

        // Validación de existencia y coincidencia de contraseña cifrada con BCrypt
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid credentials");
        }

        // Generar token JWT incorporando el subject y el claim del rol
        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());

        return ResponseEntity.ok(LoginResponseDTO.builder()
                .token(token)
                .type("Bearer")
                .role(user.getRole().name())
                .build());
    }
}
