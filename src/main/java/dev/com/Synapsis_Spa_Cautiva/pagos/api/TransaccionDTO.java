package dev.com.Synapsis_Spa_Cautiva.pagos.api;

/** estado: el de TRANSACCION_PASARELA, como texto porque el enum del dominio es interno. */
public record TransaccionDTO(Long idTransaccion, String estado, String codigoOperacion, String enlacePago) {

	private static final String APROBADA = "APROBADA";

	public boolean aprobada() {
		return APROBADA.equals(estado);
	}

}
