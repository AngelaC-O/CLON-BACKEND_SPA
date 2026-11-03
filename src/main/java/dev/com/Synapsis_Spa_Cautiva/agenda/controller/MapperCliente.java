package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cliente;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaDTO;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MapperCliente {

	ClienteDTO aDTO(Cliente oCliente, PersonaDTO oPersonaDTO);

}
