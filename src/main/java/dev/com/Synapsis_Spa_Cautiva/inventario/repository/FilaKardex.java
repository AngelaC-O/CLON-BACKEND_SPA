package dev.com.Synapsis_Spa_Cautiva.inventario.repository;

import java.time.LocalDateTime;

/** Proyección de V_KARDEX (CUS35, RF50): cada movimiento con su saldo, su usuario y su origen (RN18). */
public interface FilaKardex {

	Long getIdMovimiento();

	Long getIdSede();

	Long getIdProducto();

	String getCodigo();

	String getProducto();

	LocalDateTime getFechaHora();

	String getTipo();

	String getMotivo();

	Integer getCantidadConSigno();

	Integer getSaldo();

	String getUsuario();

	Long getIdDetalleVenta();

	Long getIdDetalleCompra();

	Long getIdAtencion();

}
