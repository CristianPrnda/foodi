package com.university.demo.application.service;

import com.university.demo.application.dto.UsuarioCreateRequest;
import com.university.demo.application.dto.UsuarioDto;
import com.university.demo.application.mapper.UsuarioMapper;
import com.university.demo.domain.Rol;
import com.university.demo.domain.Usuario;
import com.university.demo.infrastructure.repository.RolRepository;
import com.university.demo.infrastructure.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        return usuarioRepository.findAll().stream().map(usuarioMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<UsuarioDto> buscarPorId(UUID id) {
        return usuarioRepository.findById(id).map(usuarioMapper::toDto);
    }

    @Transactional
    public UsuarioDto crear(UsuarioCreateRequest request) {
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo");
        }
        Rol rol = rolRepository.findByNombre(request.rol().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("El rol no existe: " + request.rol()));
        Usuario usuario = usuarioMapper.toEntity(request, rol, passwordEncoder.encode(request.contrasena()));
        return usuarioMapper.toDto(usuarioRepository.save(usuario));
    }
}
