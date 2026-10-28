package com.university.demo.presentation.controller;

import com.university.demo.application.dto.DashboardDto;
import com.university.demo.application.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardDto indicadores() {
        return dashboardService.mostrarIndicadores();
    }
}
