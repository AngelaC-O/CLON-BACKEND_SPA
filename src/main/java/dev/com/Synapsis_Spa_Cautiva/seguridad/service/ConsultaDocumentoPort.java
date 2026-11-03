package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

/** Puerto de la consulta DNI/RUC (ADR-09). */
public interface ConsultaDocumentoPort {

	DatosPersona consultar(String codigoSunat, String numeroDocumento);

}
