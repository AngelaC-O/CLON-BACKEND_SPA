package dev.com.Synapsis_Spa_Cautiva.catalogo.repository;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Paquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.TipoPaquete;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioPaquete extends JpaRepository<Paquete, Long> {

	/** Paquetes o membresías del catálogo (CUS59); sin tipo, todos. Lo usan CUS18 y CUS21. */
	@Query("select p from Paquete p where (:tipo is null or p.tipo = :tipo) order by p.nombre")
	List<Paquete> findByTipo(@Param("tipo") TipoPaquete tipo);

}
