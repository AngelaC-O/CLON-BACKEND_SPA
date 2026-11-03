package dev.com.Synapsis_Spa_Cautiva.ventas.service;

/** Puerto de APISPERU facturación (ADR-09). El adaptador real, AdaptadorApisPeruFacturacion, llega en B5. */
public interface FacturacionElectronicaPort {

	RespuestaCpe emitir(DocumentoElectronico oDocumentoElectronico);

}
