package dev.com.Synapsis_Spa_Cautiva.inventario.repository;

/** Proyección de V_STOCK_ALERTA: productos activos de la sede en su stock mínimo o por debajo (RF47, RN16). */
public interface FilaStockAlerta {

	Long getIdSede();

	Long getIdProducto();

	String getCodigo();

	String getProducto();

	String getMarca();

	String getTipoUso();

	Integer getStockActual();

	Integer getStockMinimo();

	String getUbicacion();

}
