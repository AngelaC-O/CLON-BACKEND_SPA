package dev.com.Synapsis_Spa_Cautiva.seguridad.repository;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Persona;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioPersona extends JpaRepository<Persona, Long> {

	@Query("""
			select p from Persona p
			where (p.idTipoDocumento = :idTipoDocumento and p.numeroDocumento = :numeroDocumento)
			or p.telefono = :telefono
			""")
	List<Persona> findByDocumentoOTelefono(@Param("idTipoDocumento") Long idTipoDocumento,
			@Param("numeroDocumento") String numeroDocumento, @Param("telefono") String telefono);

	/** Lectura de PersonaApi.buscarPersonas (CUS01). */
	@Query("""
			select p from Persona p
			where (:nombres is null
				or upper(concat(p.nombres, ' ', coalesce(p.apellidos, ''))) like upper(concat('%', :nombres, '%')))
			and (:telefono is null or p.telefono = :telefono)
			""")
	List<Persona> findByNombresOTelefono(@Param("nombres") String nombres, @Param("telefono") String telefono);

}
