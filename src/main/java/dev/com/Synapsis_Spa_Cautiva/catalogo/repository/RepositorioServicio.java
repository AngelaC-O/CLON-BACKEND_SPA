package dev.com.Synapsis_Spa_Cautiva.catalogo.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.EstadoServicio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Paquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Promocion;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Servicio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.VarianteServicio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioServicio extends JpaRepository<Servicio, Long> {

	/** Catálogo con sus variantes y precios (CUS58); cada filtro es opcional. Lo usan CUS05, CUS06, CUS13 y CUS21. */
	@Query("""
			select s from Servicio s
			where (:idArea is null or s.idArea = :idArea) and (:estado is null or s.estado = :estado)
			order by s.nombre
			""")
	List<Servicio> findByAreaAndEstado(@Param("idArea") Long idArea, @Param("estado") EstadoServicio estado);

	@Query("select s from Servicio s join s.variantes v where v.idVariante = :idVariante")
	Optional<Servicio> findByVariante(@Param("idVariante") Long idVariante);

	/** [It. 2.1 · nombres] Los servicios de varias variantes, en una consulta (CatalogoApi.obtenerVariantes). */
	@Query("select distinct s from Servicio s join s.variantes v where v.idVariante in :idsVariante")
	List<Servicio> findByVariantes(@Param("idsVariante") Collection<Long> idsVariante);

	@Query("select v from VarianteServicio v where v.idVariante = :idVariante")
	Optional<VarianteServicio> findVariante(@Param("idVariante") Long idVariante);

	@Query("select p from Paquete p where p.idPaquete = :idPaquete")
	Optional<Paquete> findPaquete(@Param("idPaquete") Long idPaquete);

	@Query("select p from Promocion p where p.idPromocion = :idPromocion")
	Optional<Promocion> findPromocion(@Param("idPromocion") Long idPromocion);

}
