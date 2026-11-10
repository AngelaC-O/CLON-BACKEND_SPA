package dev.com.Synapsis_Spa_Cautiva.caja.repository;

import dev.com.Synapsis_Spa_Cautiva.caja.domain.Conciliacion;

import org.springframework.data.jpa.repository.JpaRepository;

/** CUS47: la conciliación del mes con sus ítems (composición); una por mes (UQ_CONCILIACION_ANIO_MES, 409). */
public interface RepositorioConciliacion extends JpaRepository<Conciliacion, Long> {

	/** [It. 2] CUS47: una conciliación por mes; la segunda es 409 antes de leer el mes. No está en el diagrama (DF-126). */
	boolean existsByAnioAndMes(Integer anio, Integer mes);

}
