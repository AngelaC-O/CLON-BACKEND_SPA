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

	/** CUS61: todos los recursos de la sede, en cualquier estado. */
	@Query("select r from Recurso r where r.idSede = :idSede order by r.tipo, r.nombre")
	List<Recurso> findBySede(@Param("idSede") Long idSede);

	@Query("select s from Sede s where s.idSede = :idSede")
	Optional<Sede> findSede(@Param("idSede") Long idSede);

}
