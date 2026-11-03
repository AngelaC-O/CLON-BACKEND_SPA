package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Atencion;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MapperAtencion {

	AtencionDTO aDTO(Atencion oAtencion);

}
