package dev.com.Synapsis_Spa_Cautiva.catalogo.controller;

import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Area;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Servicio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.VarianteServicio;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MapperCatalogo {

	@Mapping(target = "nombreArea", source = "nombre")
	AreaDTO aDTO(Area oArea);

	ServicioDTO aDTO(Servicio oServicio);

	@Mapping(target = "nombreVariante", source = "nombre")
	VarianteDTO aDTO(VarianteServicio oVariante);

}
