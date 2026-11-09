package dev.com.Synapsis_Spa_Cautiva.catalogo.repository;

import java.time.LocalDate;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Promocion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioPromocion extends JpaRepository<Promocion, Long> {

	/** Promociones ACTIVAS cuya vigencia incluye la fecha (CUS60; las aplican CUS18 y CUS21). */
	@Query("""
			select p from Promocion p
			where p.estado = dev.com.Synapsis_Spa_Cautiva.catalogo.domain.EstadoPromocion.ACTIVA
			and p.fechaInicio <= :fecha and p.fechaFin >= :fecha
			order by p.fechaFin, p.nombre
			""")
	List<Promocion> findVigentes(@Param("fecha") LocalDate fecha);

}
