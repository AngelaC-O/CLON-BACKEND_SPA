package dev.com.Synapsis_Spa_Cautiva.agenda.service;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.PaqueteClienteApi;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.ClientePaquete;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioClientePaquete;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ServicioClientePaquete implements PaqueteClienteApi {

	private final RepositorioClientePaquete repositorioClientePaquete;

	@Override
	@Transactional
	public void registrarPaquete(Long idCliente, Long idPaquete, Long idDetalle) {
		repositorioClientePaquete.save(new ClientePaquete(idCliente, idPaquete, idDetalle));
	}

}
