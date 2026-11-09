package dev.com.Synapsis_Spa_Cautiva.sedes.controller;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.sedes.api.DatosRecurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.domain.Recurso;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MapperRecurso {

	RecursoDTO aDTO(Recurso oRecurso);

	/** [It. 2] tipo y estado vuelven a sus enums por nombre. */
	RecursoDTO aDTO(DatosRecurso oDatosRecurso);

	List<RecursoDTO> aDTOs(List<DatosRecurso> recursos);

}
