package dev.com.Synapsis_Spa_Cautiva.caja.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import dev.com.Synapsis_Spa_Cautiva.caja.domain.Caja;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioCaja extends JpaRepository<Caja, Long> {

	/** Bloquea la caja abierta de la sede: quien la usa no compite con su cierre (RN30). */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select c from Caja c
			where c.idSede = :idSede and c.estado = dev.com.Synapsis_Spa_Cautiva.caja.domain.EstadoCaja.ABIERTA
			""")
	Optional<Caja> findAbiertaBySede(@Param("idSede") Long idSede);

	default boolean existsAbiertaBySede(Long idSede) {
		return findAbiertaBySede(idSede).isPresent();
	}

	/** RF67: cierres de la sede con alguna diferencia en el arqueo que la Administradora aún no aprueba (CUS46). */
	@Query("""
			select distinct c from Caja c join c.arqueos a
			where c.idSede = :idSede and c.estado = dev.com.Synapsis_Spa_Cautiva.caja.domain.EstadoCaja.CERRADA
			and c.idUsuarioRevisa is null and a.diferencia <> 0
			order by c.fechaCierre
			""")
	List<Caja> findCerradasConDiferencia(@Param("idSede") Long idSede);

	/** RF66: la caja que se abrió ese día en la sede (la última, si hubo más de una), para reabrirla (CUS46). */
	default Optional<Caja> findDelDia(Long idSede, LocalDateTime fecha) {
		LocalDateTime desde = fecha.toLocalDate().atStartOfDay();
		return findAperturasEntre(idSede, desde, desde.plusDays(1)).stream().findFirst();
	}

	@Query("""
			select c from Caja c
			where c.idSede = :idSede and c.fechaApertura >= :desde and c.fechaApertura < :hasta
			order by c.fechaApertura desc
			""")
	List<Caja> findAperturasEntre(@Param("idSede") Long idSede, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

}
