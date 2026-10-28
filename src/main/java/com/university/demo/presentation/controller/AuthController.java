package com.university.demo.presentation.controller;

import com.university.demo.application.dto.LoginRequest;
import com.university.demo.application.dto.UsuarioDto;
import com.university.demo.application.service.AutenticacionService;
import com.university.demo.application.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String SESION_USUARIO = "usuarioId";
    public static final String SESION_ROL = "rol";

    private final AutenticacionService autenticacionService;
    private final UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<UsuarioDto> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        return autenticacionService.autenticar(request.correo(), request.contrasena())
                .map(usuario -> {
                    session.setAttribute(SESION_USUARIO, usuario.id());
                    session.setAttribute(SESION_ROL, usuario.rol());
                    return ResponseEntity.ok(usuario);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    /** verificarSesion(): 200 con el usuario si hay sesión, 401 si no. */
    @GetMapping("/me")
    public ResponseEntity<UsuarioDto> me(HttpSession session) {
        Object id = session.getAttribute(SESION_USUARIO);
        if (!(id instanceof UUID usuarioId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return usuarioService.buscarPorId(usuarioId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
