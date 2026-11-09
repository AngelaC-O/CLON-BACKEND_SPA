package dev.com.Synapsis_Spa_Cautiva.inventario.repository;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.inventario.domain.ConsumoPendiente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioConsumoPendiente extends JpaRepository<ConsumoPendiente, Long> {

	/** Consumos PENDIENTES de la sede, del más antiguo al más reciente (CUS33, RF46, RN53). */
	@Query("""
			select c from ConsumoPendiente c
			where c.idSede = :idSede and c.estado = dev.com.Synapsis_Spa_Cautiva.inventario.domain.EstadoConsumoPendiente.PENDIENTE
			order by c.fechaRegistro
			""")
	List<ConsumoPendiente> findPendientes(@Param("idSede") Long idSede);

}
