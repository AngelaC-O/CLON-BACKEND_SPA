package dev.com.Synapsis_Spa_Cautiva.seguridad.repository;

import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Parametro;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioParametro extends JpaRepository<Parametro, Long> {

	Optional<Parametro> findByCodigo(String codigo);

}
