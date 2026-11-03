package dev.com.Synapsis_Spa_Cautiva.ventas.repository;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Comprobante;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RepositorioComprobante extends JpaRepository<Comprobante, Long> {

	/** Solo boletas y facturas: las notas de crédito no son de esta iteración. */
	@Query(nativeQuery = true, value = """
			SELECT id_documento AS "idDocumento", serie AS "serie", numero AS "numero", fecha_limite AS "fechaLimite"
			FROM V_CPE_PENDIENTES
			WHERE estado_sunat = 'PENDIENTE' AND documento = 'COMPROBANTE'
			ORDER BY fecha_limite, id_documento
			""")
	List<FilaCpePendiente> findPendientesDeEnvio();

}
