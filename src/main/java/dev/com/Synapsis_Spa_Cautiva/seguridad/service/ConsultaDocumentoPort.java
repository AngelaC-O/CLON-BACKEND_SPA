package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

/** Puerto de la consulta DNI/RUC (ADR-09). El adaptador real, AdaptadorApisPeruConsulta, llega en B5. */
public interface ConsultaDocumentoPort {

	DatosPersona consultar(String codigoSunat, String numeroDocumento);

}
