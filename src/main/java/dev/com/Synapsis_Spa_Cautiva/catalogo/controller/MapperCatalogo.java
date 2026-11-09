package dev.com.Synapsis_Spa_Cautiva.catalogo.controller;

import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Area;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Paquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.PaqueteServicio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Promocion;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.PromocionServicio;
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

	/** [It. 2] CUS59. */
	@Mapping(target = "idsServicio", source = "servicios")
	PaqueteDTO aDTO(Paquete oPaquete);

	/** [It. 2] CUS60. */
	@Mapping(target = "idsServicio", source = "servicios")
	PromocionDTO aDTO(Promocion oPromocion);

	default Long idServicioDePaquete(PaqueteServicio oPaqueteServicio) {
		return oPaqueteServicio.getIdServicio();
	}

	default Long idServicioDePromocion(PromocionServicio oPromocionServicio) {
		return oPromocionServicio.getIdServicio();
	}

}
