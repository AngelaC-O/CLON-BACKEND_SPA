package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSunat;

import org.springframework.stereotype.Component;

/** Siempre responde PENDIENTE: la venta se cierra igual y el comprobante espera el reenvío (RN46). */
@Component
class FacturacionElectronicaFalsa implements FacturacionElectronicaPort {

	@Override
	public RespuestaCpe emitir(DocumentoElectronico oDocumentoElectronico) {
		return new RespuestaCpe(EstadoSunat.PENDIENTE, null, "Respuesta del adaptador falso", null, null, null);
	}

}
