package dev.com.Synapsis_Spa_Cautiva.ventas.repository;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.SerieComprobante;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioSerieComprobante extends JpaRepository<SerieComprobante, Long> {

	/** Series de la sede, activas e inactivas (CUS63, RF107). */
	@Query("select s from SerieComprobante s where s.idSede = :idSede order by s.tipo, s.serie")
	List<SerieComprobante> findBySede(@Param("idSede") Long idSede);

}
