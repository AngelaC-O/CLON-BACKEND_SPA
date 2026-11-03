package dev.com.Synapsis_Spa_Cautiva.caja.repository;

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

}
