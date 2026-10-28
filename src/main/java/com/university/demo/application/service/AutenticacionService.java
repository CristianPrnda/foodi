package com.university.demo.application.service;

import com.university.demo.application.dto.UsuarioDto;
import com.university.demo.application.mapper.UsuarioMapper;
import com.university.demo.domain.Usuario;
import com.university.demo.infrastructure.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Clase "Autenticacion" del diagrama. Valida credenciales contra la BD (BCrypt).
 * La sesión (iniciar / cerrar / verificar) la maneja AuthController con HttpSession.
 */
@Service
@RequiredArgsConstructor
public class AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    /** Devuelve el usuario si el correo existe, está activo y la contraseña coincide. */
    @Transactional(readOnly = true)
    public Optional<UsuarioDto> autenticar(String correo, String contrasena) {
        return usuarioRepository.findByCorreo(correo)
                .filter(Usuario::isEstado)
                .filter(u -> passwordEncoder.matches(contrasena, u.getContrasena()))
                .map(usuarioMapper::toDto);
    }
}
