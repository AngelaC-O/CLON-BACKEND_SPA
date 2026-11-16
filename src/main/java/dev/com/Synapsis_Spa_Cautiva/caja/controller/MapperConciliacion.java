package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import dev.com.Synapsis_Spa_Cautiva.caja.domain.Conciliacion;
import dev.com.Synapsis_Spa_Cautiva.caja.domain.ConciliacionItem;
import dev.com.Synapsis_Spa_Cautiva.caja.domain.EstadoConciliacionItem;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** [It. 2 · CUS47] */
@Mapper(componentModel = "spring")
public interface MapperConciliacion {

	@Mapping(target = "sinJustificar", expression = "java(contarSinJustificar(oConciliacion))")
	ConciliacionDTO aDTO(Conciliacion oConciliacion);

	ConciliacionDTO.ItemConciliacion aDTO(ConciliacionItem oItem);

	default Long contarSinJustificar(Conciliacion oConciliacion) {
		return oConciliacion.getItems().stream().filter(oItem -> oItem.getEstado() == EstadoConciliacionItem.SIN_JUSTIFICAR).count();
	}

}
