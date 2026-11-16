package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import dev.com.Synapsis_Spa_Cautiva.caja.domain.Caja;
import dev.com.Synapsis_Spa_Cautiva.caja.domain.CajaArqueo;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MapperCaja {

	@Mapping(target = "nombreUsuarioApertura", ignore = true)
	@Mapping(target = "nombreUsuarioCierre", ignore = true)
	@Mapping(target = "nombreUsuarioRevisa", ignore = true)
	@Mapping(target = "nombreUsuarioReapertura", ignore = true)
	CajaDTO aDTO(Caja oCaja);

	ArqueoCalculadoDTO aDTO(CajaArqueo oCajaArqueo);

}
