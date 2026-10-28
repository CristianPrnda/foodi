package com.university.demo.application.dto;

import java.util.UUID;

/** Lo que se devuelve al front. Nunca incluye la contraseña. */
public record UsuarioDto(UUID id, String nombre, String correo, String rol, boolean estado) {
}
