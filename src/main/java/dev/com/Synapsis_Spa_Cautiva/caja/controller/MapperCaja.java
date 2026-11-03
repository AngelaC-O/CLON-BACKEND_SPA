package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import dev.com.Synapsis_Spa_Cautiva.caja.domain.Caja;
import dev.com.Synapsis_Spa_Cautiva.caja.domain.CajaArqueo;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MapperCaja {

	CajaDTO aDTO(Caja oCaja);

	ArqueoCalculadoDTO aDTO(CajaArqueo oCajaArqueo);

}
