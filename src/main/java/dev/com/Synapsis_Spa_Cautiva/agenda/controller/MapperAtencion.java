package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Atencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.RecomendacionProducto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MapperAtencion {

	@Mapping(target = "consumosPendientes", ignore = true)
	AtencionDTO aDTO(Atencion oAtencion);

	/** [It. 2] CUS12 y la ficha de CUS03. */
	RecomendacionDTO aDTO(RecomendacionProducto oRecomendacion);

}
