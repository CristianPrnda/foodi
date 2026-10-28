package com.university.demo.infrastructure.config;

import com.university.demo.presentation.controller.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Protege /api/** sin necesidad de Spring Security completo:
 *  - sin sesión iniciada → 401
 *  - /api/usuarios solo para el rol ADMINISTRADOR → 403
 * El login y logout quedan abiertos (ver WebConfig).
 */
public class SesionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(AuthController.SESION_USUARIO) == null) {
            return rechazar(response, HttpServletResponse.SC_UNAUTHORIZED, "Debes iniciar sesión");
        }
        if (request.getRequestURI().startsWith("/api/usuarios")
                && !"ADMINISTRADOR".equals(session.getAttribute(AuthController.SESION_ROL))) {
            return rechazar(response, HttpServletResponse.SC_FORBIDDEN, "Solo el administrador puede gestionar usuarios");
        }
        return true;
    }

    private boolean rechazar(HttpServletResponse response, int status, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + mensaje + "\"}");
        return false;
    }
}
