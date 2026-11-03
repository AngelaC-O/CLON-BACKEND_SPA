package dev.com.Synapsis_Spa_Cautiva.pagos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.pagos.domain.MedioPago;
import dev.com.Synapsis_Spa_Cautiva.pagos.domain.TransaccionPasarela;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioTransaccionPasarela extends JpaRepository<TransaccionPasarela, Long> {

	Optional<TransaccionPasarela> findByCodigoOperacion(String codigoOperacion);

	@Query("select m from MedioPago m where m.idMedioPago = :idMedioPago")
	Optional<MedioPago> findMedioPago(@Param("idMedioPago") Long idMedioPago);

	/** Por fecha_respuesta, cuando la pasarela confirmó el cobro (RF64). */
	@Query("""
			select t.idMedioPago as idMedioPago, sum(t.monto) as total from TransaccionPasarela t
			where t.idSede = :idSede
			and t.estado = dev.com.Synapsis_Spa_Cautiva.pagos.domain.EstadoTransaccion.APROBADA
			and t.tipoOperacion = dev.com.Synapsis_Spa_Cautiva.pagos.domain.TipoOperacion.COBRO
			and t.fechaRespuesta >= :desde and t.fechaRespuesta < :hasta
			group by t.idMedioPago
			""")
	List<FilaAprobadoDelDia> sumarAprobadosDelDia(@Param("idSede") Long idSede, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

}
