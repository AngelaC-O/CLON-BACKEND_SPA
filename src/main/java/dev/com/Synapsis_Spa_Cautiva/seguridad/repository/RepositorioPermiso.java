package dev.com.Synapsis_Spa_Cautiva.seguridad.repository;

import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Permiso;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioPermiso extends JpaRepository<Permiso, Long> {

	Optional<Permiso> findByModuloAndAccion(String modulo, String accion);

	default Permiso findOrCreate(String modulo, String accion) {
		return findByModuloAndAccion(modulo, accion).orElseGet(() -> save(new Permiso(modulo, accion)));
	}

}
