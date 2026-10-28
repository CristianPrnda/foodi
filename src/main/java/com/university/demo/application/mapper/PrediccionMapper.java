package com.university.demo.application.mapper;

import com.university.demo.application.dto.PrediccionDto;
import com.university.demo.domain.PrediccionIA;
import org.springframework.stereotype.Component;

@Component
public class PrediccionMapper {

    public PrediccionDto toDto(PrediccionIA p) {
        return new PrediccionDto(p.getFecha(), p.getVentasPredichas(), p.getNivelConfianza(),
                p.getHoraPico(), p.getRecomendacion());
    }
}
