package dev.com.Synapsis_Spa_Cautiva.ventas.service;

/** Puerto de APISPERU facturación (ADR-09). Si el proveedor no responde, devuelve PENDIENTE (RN46). */
public interface FacturacionElectronicaPort {

	RespuestaCpe emitir(DocumentoElectronico oDocumentoElectronico);

}
