package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Atencion;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MapperAtencion {

	@Mapping(target = "consumosPendientes", ignore = true)
	AtencionDTO aDTO(Atencion oAtencion);

}
