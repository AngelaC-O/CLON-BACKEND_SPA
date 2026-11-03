package dev.com.Synapsis_Spa_Cautiva.personal.service;

import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.personal.api.PersonalApi;
import dev.com.Synapsis_Spa_Cautiva.personal.domain.Personal;
import dev.com.Synapsis_Spa_Cautiva.personal.repository.RepositorioPersonal;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ServicioPersonal implements PersonalApi {

	private final RepositorioPersonal repositorioPersonal;

	@Override
	@Transactional(readOnly = true)
	public Optional<Long> obtenerIdSede(Long idPersonal) {
		return repositorioPersonal.findById(idPersonal).map(Personal::getIdSede);
	}

}
