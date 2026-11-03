package dev.com.Synapsis_Spa_Cautiva.catalogo.repository;

import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Paquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Promocion;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Servicio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.VarianteServicio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioServicio extends JpaRepository<Servicio, Long> {

	@Query("select s from Servicio s join s.variantes v where v.idVariante = :idVariante")
	Optional<Servicio> findByVariante(@Param("idVariante") Long idVariante);

	@Query("select v from VarianteServicio v where v.idVariante = :idVariante")
	Optional<VarianteServicio> findVariante(@Param("idVariante") Long idVariante);

	@Query("select p from Paquete p where p.idPaquete = :idPaquete")
	Optional<Paquete> findPaquete(@Param("idPaquete") Long idPaquete);

	@Query("select p from Promocion p where p.idPromocion = :idPromocion")
	Optional<Promocion> findPromocion(@Param("idPromocion") Long idPromocion);

}
