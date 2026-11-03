package dev.com.Synapsis_Spa_Cautiva.sedes.repository;

import java.util.List;
import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.sedes.domain.EstadoRecurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.domain.Recurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.domain.Sede;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioRecurso extends JpaRepository<Recurso, Long> {

	List<Recurso> findByIdSedeAndEstado(Long idSede, EstadoRecurso estado);

	@Query("select s from Sede s where s.idSede = :idSede")
	Optional<Sede> findSede(@Param("idSede") Long idSede);

}
