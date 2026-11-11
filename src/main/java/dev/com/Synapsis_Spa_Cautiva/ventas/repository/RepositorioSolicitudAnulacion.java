package dev.com.Synapsis_Spa_Cautiva.ventas.repository;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSolicitudAnulacion;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.SolicitudAnulacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioSolicitudAnulacion extends JpaRepository<SolicitudAnulacion, Long> {

	/** Bandeja de la Administradora (CUS23, RF33): las solicitudes PENDIENTES de las ventas de la sede, la más antigua primero. */
	@Query("""
			select s from SolicitudAnulacion s, Venta v
			where v.idVenta = s.idVenta and v.idSede = :idSede
			and s.estado = dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSolicitudAnulacion.PENDIENTE
			order by s.fechaSolicitud
			""")
	List<SolicitudAnulacion> findPendientes(@Param("idSede") Long idSede);

	/** [It. 2] CUS23 con el filtro ?estado: APROBADAS o RECHAZADAS de la sede, la resuelta más reciente primero. */
	@Query("""
			select s from SolicitudAnulacion s, Venta v
			where v.idVenta = s.idVenta and v.idSede = :idSede and s.estado = :estado
			order by s.fechaResolucion desc, s.fechaSolicitud desc
			""")
	List<SolicitudAnulacion> findPorEstado(@Param("idSede") Long idSede, @Param("estado") EstadoSolicitudAnulacion estado);

}
