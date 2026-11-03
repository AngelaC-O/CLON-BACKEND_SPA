package dev.com.Synapsis_Spa_Cautiva.catalogo.repository;

import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Servicio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioServicio extends JpaRepository<Servicio, Long> {

	@Query("select s from Servicio s join s.variantes v where v.idVariante = :idVariante")
	Optional<Servicio> findByVariante(@Param("idVariante") Long idVariante);

}
