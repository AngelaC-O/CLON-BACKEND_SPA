package dev.com.Synapsis_Spa_Cautiva.comun.service;

import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
class ConfiguracionTransacciones {

	@Bean
	PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory, UsuarioActual usuarioActual) {
		return new TransaccionConSesionOracle(entityManagerFactory, usuarioActual);
	}

}
