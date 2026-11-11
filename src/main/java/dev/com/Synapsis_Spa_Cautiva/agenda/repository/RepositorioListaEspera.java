package dev.com.Synapsis_Spa_Cautiva.agenda.repository;

import java.time.LocalDate;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.ListaEspera;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioListaEspera extends JpaRepository<ListaEspera, Long> {

	/** Esperas PENDIENTES del día de la sede, en el orden en que se anotaron (CUS06, RF09). */
	@Query("""
			select e from ListaEspera e
			where e.idSede = :idSede and e.fechaDeseada = :fecha
			and e.estado = dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoListaEspera.PENDIENTE
			order by e.fechaRegistro
			""")
	List<ListaEspera> findPendientes(@Param("idSede") Long idSede, @Param("fecha") LocalDate fecha);

}
