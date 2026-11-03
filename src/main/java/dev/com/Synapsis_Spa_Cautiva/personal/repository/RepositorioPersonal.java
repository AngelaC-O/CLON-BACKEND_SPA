package dev.com.Synapsis_Spa_Cautiva.personal.repository;

import java.util.Collection;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.personal.domain.CargoPersonal;
import dev.com.Synapsis_Spa_Cautiva.personal.domain.EstadoPersonal;
import dev.com.Synapsis_Spa_Cautiva.personal.domain.Personal;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioPersonal extends JpaRepository<Personal, Long> {

	List<Personal> findByIdSedeAndEstadoAndCargoIn(Long idSede, EstadoPersonal estado, Collection<CargoPersonal> cargos);

}
