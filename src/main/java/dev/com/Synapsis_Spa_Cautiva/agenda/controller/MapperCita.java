package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.HoraLibre;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cita;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.ListaEspera;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MapperCita {

	@Mapping(target = "idEspera", ignore = true)
	@Mapping(target = "nombreCliente", ignore = true)
	@Mapping(target = "nombreEspecialista", ignore = true)
	@Mapping(target = "nombreServicio", ignore = true)
	@Mapping(target = "nombreRecurso", ignore = true)
	CitaDTO aDTO(Cita oCita);

	/** [It. 2] CUS05: la cita que salió de una espera (RF09). */
	@Mapping(target = "idEspera", source = "idEspera")
	@Mapping(target = "nombreCliente", ignore = true)
	@Mapping(target = "nombreEspecialista", ignore = true)
	@Mapping(target = "nombreServicio", ignore = true)
	@Mapping(target = "nombreRecurso", ignore = true)
	CitaDTO aDTO(Cita oCita, Long idEspera);

	@Mapping(target = "nombreCliente", ignore = true)
	@Mapping(target = "nombreServicio", ignore = true)
	ListaEsperaDTO aDTO(ListaEspera oListaEspera, List<HoraLibre> horasLibres);

	/** [It. 2] CUS06: la espera con el nombre de la clienta y del servicio. */
	@Mapping(target = "horasLibres", ignore = true)
	@Mapping(target = "nombreCliente", source = "nombreCliente")
	@Mapping(target = "nombreServicio", source = "nombreServicio")
	ListaEsperaDTO aDTO(ListaEspera oListaEspera, String nombreCliente, String nombreServicio);

}
