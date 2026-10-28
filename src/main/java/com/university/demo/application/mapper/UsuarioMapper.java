package com.university.demo.application.mapper;

import com.university.demo.application.dto.UsuarioCreateRequest;
import com.university.demo.application.dto.UsuarioDto;
import com.university.demo.domain.Rol;
import com.university.demo.domain.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioDto toDto(Usuario usuario) {
        return new UsuarioDto(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol().getNombre(),
                usuario.isEstado());
    }

    /** La contraseña llega ya cifrada; el mapper no cifra nada. */
    public Usuario toEntity(UsuarioCreateRequest request, Rol rol, String contrasenaCifrada) {
        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setCorreo(request.correo());
        usuario.setContrasena(contrasenaCifrada);
        usuario.setRol(rol);
        return usuario;
    }
}
