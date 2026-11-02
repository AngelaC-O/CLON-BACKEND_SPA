package dev.com.Synapsis_Spa_Cautiva.ventas.domain;

/** Copia fija de los datos del comprador en el comprobante (RN08). */
public record DocumentoComprador(Long idTipoDocComprador, String nroDocComprador, String nombreComprador) {

	boolean estaCompleto() {
		return idTipoDocComprador != null && nroDocComprador != null && nombreComprador != null;
	}

}
