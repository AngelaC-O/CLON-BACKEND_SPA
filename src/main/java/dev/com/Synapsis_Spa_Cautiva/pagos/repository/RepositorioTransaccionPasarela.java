package dev.com.Synapsis_Spa_Cautiva.pagos.repository;

import java.math.BigDecimal;
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

	/** [It. 2 · CUS24, RN44] Lo ya devuelto (o en curso) de un cobro, como lo cuenta TRG_TRANS_PAS_REGLAS (-20077). */
	@Query("""
			select coalesce(sum(t.monto), 0) from TransaccionPasarela t
			where t.idTransaccionOrigen = :idTransaccionOrigen
			and t.tipoOperacion = dev.com.Synapsis_Spa_Cautiva.pagos.domain.TipoOperacion.DEVOLUCION
			and t.estado in (dev.com.Synapsis_Spa_Cautiva.pagos.domain.EstadoTransaccion.PENDIENTE,
				dev.com.Synapsis_Spa_Cautiva.pagos.domain.EstadoTransaccion.APROBADA)
			""")
	BigDecimal sumarDevuelto(@Param("idTransaccionOrigen") Long idTransaccionOrigen);

	/** Resumen del mes de la pasarela para conciliar (CUS47, RF105, RN31). Lee V_PASARELA_LIQUIDACION. */
	@Query(nativeQuery = true, value = """
			SELECT id_sede AS "idSede", medio_pago AS "medioPago", cobros AS "cobros", devoluciones AS "devoluciones",
				comisiones AS "comisiones", neto_esperado AS "netoEsperado", neto_abonado AS "netoAbonado",
				cobros_sin_abono AS "cobrosSinAbono", sin_comision_registrada AS "sinComisionRegistrada"
			FROM V_PASARELA_LIQUIDACION
			WHERE anio = :anio AND mes = :mes
			ORDER BY id_sede, medio_pago
			""")
	List<FilaLiquidacionMes> findLiquidacionDelMes(@Param("anio") Integer anio, @Param("mes") Integer mes);

}
