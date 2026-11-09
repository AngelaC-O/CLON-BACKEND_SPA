package dev.com.Synapsis_Spa_Cautiva.caja.repository;

import dev.com.Synapsis_Spa_Cautiva.caja.domain.Conciliacion;

import org.springframework.data.jpa.repository.JpaRepository;

/** CUS47: la conciliación del mes con sus ítems (composición); una por mes (UQ_CONCILIACION_ANIO_MES, 409). */
public interface RepositorioConciliacion extends JpaRepository<Conciliacion, Long> {

}
