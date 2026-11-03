package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.HoraLibre;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cita;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.ListaEspera;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MapperCita {

	CitaDTO aDTO(Cita oCita);

	ListaEsperaDTO aDTO(ListaEspera oListaEspera, List<HoraLibre> horasLibres);

}
