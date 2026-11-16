package dev.com.Synapsis_Spa_Cautiva.inventario.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.inventario.domain.Producto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioProducto extends JpaRepository<Producto, Long> {

	/**
	 * Productos e insumos con su inventario (CUS32, RF44); cada filtro es opcional. Con sede, solo los que tienen
	 * inventario en ella. Lo usan CUS33 a CUS35 y el registro de insumos de CUS57.
	 */
	@Query("""
			select distinct p from Producto p left join p.inventarios i
			where (:nombre is null or upper(p.nombre) like upper(concat('%', :nombre, '%')))
			and (:idCategoria is null or p.idCategoria = :idCategoria)
			and (:idSede is null or i.id.idSede = :idSede)
			order by p.nombre
			""")
	List<Producto> findByFiltros(@Param("nombre") String nombre, @Param("idCategoria") Long idCategoria, @Param("idSede") Long idSede);

	/** Kardex del producto en la sede entre dos fechas, incluidas (CUS35, RF50). Lee V_KARDEX. */
	default List<FilaKardex> findMovimientos(Long idProducto, Long idSede, LocalDate fechaInicio, LocalDate fechaFin) {
		return findMovimientosEntre(idProducto, idSede, fechaInicio.atStartOfDay(), fechaFin.plusDays(1).atStartOfDay());
	}

	@Query(nativeQuery = true, value = """
			SELECT id_movimiento AS "idMovimiento", id_sede AS "idSede", id_producto AS "idProducto", codigo AS "codigo",
				producto AS "producto", fecha_hora AS "fechaHora", tipo AS "tipo", motivo AS "motivo",
				cantidad_con_signo AS "cantidadConSigno", saldo AS "saldo", usuario AS "usuario",
				id_detalle_venta AS "idDetalleVenta", id_detalle_compra AS "idDetalleCompra", id_atencion AS "idAtencion",
				comprobante AS "comprobante", documento_compra AS "documentoCompra"
			FROM V_KARDEX
			WHERE id_producto = :idProducto AND id_sede = :idSede AND fecha_hora >= :desde AND fecha_hora < :hasta
			ORDER BY fecha_hora, id_movimiento
			""")
	List<FilaKardex> findMovimientosEntre(@Param("idProducto") Long idProducto, @Param("idSede") Long idSede,
			@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

	/** Productos de la sede en su stock mínimo, para el panel de avisos de inventario (CUS50, RF47). Lee V_STOCK_ALERTA. */
	@Query(nativeQuery = true, value = """
			SELECT id_sede AS "idSede", id_producto AS "idProducto", codigo AS "codigo", producto AS "producto", marca AS "marca",
				tipo_uso AS "tipoUso", stock_actual AS "stockActual", stock_minimo AS "stockMinimo", ubicacion AS "ubicacion"
			FROM V_STOCK_ALERTA
			WHERE id_sede = :idSede
			ORDER BY producto
			""")
	List<FilaStockAlerta> findEnStockMinimo(@Param("idSede") Long idSede);

}
