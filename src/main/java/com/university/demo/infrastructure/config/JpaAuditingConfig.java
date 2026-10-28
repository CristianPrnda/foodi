package com.university.demo.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Activa createdAt / updatedAt automáticos (ver domain.Auditable). */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
