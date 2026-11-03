package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSunat;

/** xml y cdr son el contenido; ServicioFacturacionElectronica los guarda en el almacén. Pueden venir vacíos si quedó PENDIENTE. */
public record RespuestaCpe(EstadoSunat estadoSunat, String codigoRespuesta, String descripcionRespuesta, String hashCpe, String xml, String cdr) {
}
