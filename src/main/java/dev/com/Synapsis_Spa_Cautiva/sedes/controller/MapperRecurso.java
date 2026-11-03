package dev.com.Synapsis_Spa_Cautiva.sedes.controller;

import dev.com.Synapsis_Spa_Cautiva.sedes.domain.Recurso;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MapperRecurso {

	RecursoDTO aDTO(Recurso oRecurso);

}
