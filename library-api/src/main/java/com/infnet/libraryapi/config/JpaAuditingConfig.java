package com.infnet.libraryapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Nao mova para a classe da aplicacao: la o {@code @EnableJpaAuditing} faria os
 * testes de fatia web ({@code @WebMvcTest}) tentarem inicializar o metamodelo
 * JPA, que nao existe naquele contexto.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
